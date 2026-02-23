import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService.ts";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import * as geodeticDensifyOperator from "@arcgis/core/geometry/operators/geodeticDensifyOperator.js";
import * as Terraformer from "@terraformer/arcgis";
import * as proximityOperator from "@arcgis/core/geometry/operators/proximityOperator.js";
import Point from "@arcgis/core/geometry/Point";
import SpatialReference from "@arcgis/core/geometry/SpatialReference";
import grpc from "@grpc/grpc-js";
import {
    LineWithNavigationTypeAndId,
    NavigationType,
    pointConnectsToLoxodromeLineFollowingSetBearing,
    SetBearing
} from "./point-connects-to-line-on-set-bearing";
import * as intersectionOperator from "@arcgis/core/geometry/operators/intersectionOperator.js";
import Multipoint from "@arcgis/core/geometry/Multipoint";
import {GeoJsonLineInput} from "../generated/arcgisjs/GeoJsonLineInput.js";
import {
    findLineConnectingToPoint,
    findParentLine,
    getNearestParentStartAndEndNodes,
    getStartAndEndNodes
} from "../utils/line-utils.js";

// 1 second in degrees (arc second) == 1° (degree) / 60'(minutes) / 60” (seconds)
const ONE_ARC_SECOND = 1 / 3600.0;

export const convertGeoJsonLineToEsriJsonLine: ArcGisServiceHandlers['convertGeoJsonLineToEsriJsonLine'] = async (call, callback) => {
    const {geoJsonString, wkid, isGeodesic, parentLines} = call.request;
    console.log(`Input srs: ${wkid} isGeodesic: ${isGeodesic} geojson: ${geoJsonString}`)

    let polyline: Polyline = Polyline.fromJSON(Terraformer.geojsonToArcGIS(JSON.parse(geoJsonString)));
    polyline.spatialReference = {wkid: wkid}

    if (isGeodesic) {
        // geodesic child lines will always have an associated parent line.
        if (parentLines.length > 0) {

            const {childStartPoint, childEndPoint} = getStartAndEndNodes(polyline)

            const parent = findParentLine(parentLines, childStartPoint, childEndPoint);

            if (parent === undefined) {
                const errorMessage = "Geodesic child line should have associated parent line but none were found";
                console.error(errorMessage)
                callback({
                    code: grpc.status.INVALID_ARGUMENT,
                    message: errorMessage
                }, null);
                return;
            } else {
                polyline = mergeParentDensePointsIntoChildLine(parent, childStartPoint, childEndPoint, polyline.spatialReference)
            }
        } else {
            // parentless geodesic lines need to be densified
            if (!geodeticDensifyOperator.isLoaded()) {
                await geodeticDensifyOperator.load();
            }

            polyline = geodeticDensifyOperator.execute(polyline, 50, {curveType: "geodesic", unit: "meters"}) as Polyline;
        }
    }

    const esriJsonString = JSON.stringify(polyline);
    console.log(`Output esrijson: ${esriJsonString}`)
    callback(null, {esriJsonString: esriJsonString});
}

export const batchConvertGeoJsonLinesToEsriJsonLines: ArcGisServiceHandlers['batchConvertGeoJsonLinesToEsriJsonLines'] = async (call, callback) => {
    const {linesWithType, wkid, parentLineJsons} = call.request;
    console.log(`Batch convert: ${linesWithType.length} lines, srs: ${wkid}`);

    const idToLineWithNavigationWrapper: Map<number, LineWithNavigationTypeAndId> = geoJsonLineInputToLinesWithNavigationTypeAndId(linesWithType, wkid);

    if (parentLineJsons.length === 0) {
        callback(null, {lines: await migrateParentPolygon(Array.from(idToLineWithNavigationWrapper.values()))});
        return;
    }

    for (const child of idToLineWithNavigationWrapper.values()) {
        const {line, navigationType, id} = child;
        // We want to process all the geodesic lines first and shift the start/end nodes of them and their connected lines.
        if (navigationType !== NavigationType.GEODESIC) {
            continue;
        }

        const {childStartPoint, childEndPoint} = getStartAndEndNodes(line);
        const parent = findParentLine(parentLineJsons, childStartPoint, childEndPoint);
        if (parent === undefined) {
            const errorMessage = "Geodesic child line should have associated parent line but none were found";
            console.error(errorMessage)
            callback({
                code: grpc.status.INVALID_ARGUMENT,
                message: errorMessage
            }, null);
            return;
        }

        const {nearestStartPoint, nearestEndPoint} = getNearestParentStartAndEndNodes(parent, childStartPoint, childEndPoint);
        const pointsNeedShifting = Math.abs(nearestStartPoint.distance) !==  0 || Math.abs(nearestEndPoint.distance) !== 0;
        if (!pointsNeedShifting) {
            console.log("Start/end nodes don't need shifting")
            child.line = mergeParentDensePointsIntoChildLine(parent, childStartPoint, childEndPoint, line.spatialReference);
            continue;
        }

        const newStartPoint = shiftNodeAndUpdateConnectedLine(
            childStartPoint, nearestStartPoint.coordinate, id, idToLineWithNavigationWrapper, parent, "start"
        );

        const newEndPoint = shiftNodeAndUpdateConnectedLine(
            childEndPoint, nearestEndPoint.coordinate, id, idToLineWithNavigationWrapper, parent, "end"
        );

        const newGeodesicLine = mergeParentDensePointsIntoChildLine(parent, newStartPoint, newEndPoint, line.spatialReference);
        child.line = newGeodesicLine;
        console.log(`new geodesic  ${newGeodesicLine.paths} points`);
    }

    // Fix direction of lines based on connection order
    fixDirectionOfAllLines(idToLineWithNavigationWrapper, linesWithType);

    const convertedLines: { esriJsonString: string; oracleLineSsid: number }[] = [];
    idToLineWithNavigationWrapper.forEach((value, key) => convertedLines.push({esriJsonString: JSON.stringify(value.line), oracleLineSsid: key}));
    callback(null, {lines: convertedLines});
}

function shiftNodeAndUpdateConnectedLine(
    childPoint: Point,
    nearestCoordinate: Point,
    childId: number,
    idToLineWrapper: Map<number, LineWithNavigationTypeAndId>,
    parentGeodesicLine: Polyline,
    nodeType: "start" | "end"
): Point {
    const linesWithNavigationTypeAndId = Array.from(idToLineWrapper.values());
    const lineOnBearing = pointConnectsToLoxodromeLineFollowingSetBearing(childPoint, linesWithNavigationTypeAndId);

    if (lineOnBearing !== undefined) {
        console.log(`Line connected to ${nodeType} node is on set bearing`);
        const newPoint = findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(childPoint, lineOnBearing.setBearing, parentGeodesicLine);

        if (newPoint === undefined) {
            throw new Error(`No intersection point for line ${lineOnBearing.id} on set bearing was found.`);
        }

        // Update the node on the connecting line and add it to the processed lines
        const index = getIndexOfPointOnLine(childPoint, lineOnBearing.line);
        lineOnBearing.line.setPoint(0, index, newPoint);
        idToLineWrapper.get(lineOnBearing.id).line = lineOnBearing.line;
        return newPoint;
    }

    console.log(`Line connected to ${nodeType} node is NOT on set bearing`);
    const lineConnectingToPoint = findLineConnectingToPoint(childPoint, childId, linesWithNavigationTypeAndId);

    if (lineConnectingToPoint === undefined) {
        throw new Error(`No line connecting to ${nodeType} node with id ${childId} was found.`);
    }

    const index = getIndexOfPointOnLine(childPoint, lineConnectingToPoint.line);
    lineConnectingToPoint.line.setPoint(0, index, nearestCoordinate);
    idToLineWrapper.get(lineConnectingToPoint.id).line = lineConnectingToPoint.line;
    return nearestCoordinate;
}

function geoJsonLineInputToLinesWithNavigationTypeAndId(input: GeoJsonLineInput[], wkid: number): Map<number, LineWithNavigationTypeAndId> {
    const idToLineWithNavigationWrapper: Map<number, LineWithNavigationTypeAndId> = new Map();
    input.forEach((lineObject) => {
        let line: Polyline = Polyline.fromJSON(Terraformer.geojsonToArcGIS(JSON.parse(lineObject.geoJsonString)));
        line.spatialReference = {wkid: wkid};

        idToLineWithNavigationWrapper.set(
            lineObject.oracleLineSsid,
            {
                line: line,
                navigationType: lineObject.isGeodesic ? NavigationType.GEODESIC : NavigationType.LOXODROME,
                id: lineObject.oracleLineSsid
            }
        )
    });
    return idToLineWithNavigationWrapper;
}

async function migrateParentPolygon(linesWithNavigationTypeAndId: LineWithNavigationTypeAndId[]) {
    console.log("Migrating parent polygon");

    const convertedLines: { esriJsonString: string; oracleLineSsid: number }[] = [];
    for (const lineObject of linesWithNavigationTypeAndId) {
        let {line, navigationType, id} = lineObject;

        if (navigationType === NavigationType.GEODESIC) {
            if (!geodeticDensifyOperator.isLoaded()) {
                await geodeticDensifyOperator.load();
            }
            line = geodeticDensifyOperator.execute(line, 50, {curveType: "geodesic", unit: "meters"}) as Polyline;
        }

        convertedLines.push({esriJsonString: JSON.stringify(line), oracleLineSsid: id});
    }
    return convertedLines;
}

function getIndexOfPointOnLine(point: Point, polyline: Polyline) {
    return proximityOperator.getNearestVertex(polyline, point).vertexIndex;
}

function findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(childPoint: Point, bearing: SetBearing, parent: Polyline): Point | undefined {
    let twoSecondLine: Polyline;
    switch (bearing) {
        case SetBearing.LATITUDE:
            twoSecondLine = pointToNorthSouthLine(childPoint.longitude, childPoint.latitude, parent.spatialReference);

            break;
        case SetBearing.LONGITUDE:
            twoSecondLine = pointToEastWestLine(childPoint.longitude, childPoint.latitude, parent.spatialReference);
            break;
    }

    // Using normal .execute doesn't work unless there is an overlap between the two lines to create a new polyline
    // if there is just an intersection the result should be Point/Multipoint which is a lower dimension so the .execute just
    // returns null.
    // see https://developers.arcgis.com/javascript/latest/api-reference/esri-geometry-geometryEngine.html#intersectLinesToPoints
    const intersectionResult = intersectionOperator.executeMany([parent], twoSecondLine);

    // return the first point of intersection
    return  (intersectionResult[0] as Multipoint).getPoint(0);
}

function fixDirectionOfAllLines(
    idToLineWithNavigationWrapper: Map<number, LineWithNavigationTypeAndId>,
    linesWithType: GeoJsonLineInput[]
) {
    const ringToConnectionOrderToId = new Map<number, Map<number, number>>();
    linesWithType.forEach((lineInput) => {
        const ringNumber = Number(lineInput.ringNumber);
        if (!ringToConnectionOrderToId.has(ringNumber)) {
            ringToConnectionOrderToId.set(ringNumber, new Map<number, number>());
        }
        ringToConnectionOrderToId.get(ringNumber).set(Number(lineInput.connectionOrder), lineInput.oracleLineSsid);
    });

    for (const [ringNumber, connectionOrderToId] of ringToConnectionOrderToId) {
        for (const currentConnectionOrder of Array.from(connectionOrderToId.keys()).sort((a, b) => a - b)) {
            let nextConnectionOrder = currentConnectionOrder + 1;
            let nextLineId = connectionOrderToId.get(currentConnectionOrder + 1);
            // If we're at the last line, connect back to the first line
            if (nextLineId === undefined) {
                nextConnectionOrder = Math.min(...connectionOrderToId.keys());
                nextLineId = connectionOrderToId.get(nextConnectionOrder);
            }

            if (nextLineId !== undefined && idToLineWithNavigationWrapper.has(nextLineId)) {
                const currentLineId = connectionOrderToId.get(currentConnectionOrder);
                const currentLineWithNavigationWrapper = idToLineWithNavigationWrapper.get(currentLineId);
                const currentStartPoint = currentLineWithNavigationWrapper.line.getPoint(0, 0);
                const currentEndPoint = currentLineWithNavigationWrapper.line.getPoint(0, currentLineWithNavigationWrapper.line.paths[0].length - 1);

                const nextLineWithNavigationWrapper = idToLineWithNavigationWrapper.get(nextLineId);
                const nextStartPoint = nextLineWithNavigationWrapper.line.getPoint(0, 0);
                const nextEndPoint = nextLineWithNavigationWrapper.line.getPoint(0, nextLineWithNavigationWrapper.line.paths[0].length - 1);

                console.log(`Ring ${ringNumber}: Checking line ${currentLineId} (order ${currentConnectionOrder}) -> line ${nextLineId} (order ${nextConnectionOrder})`);
                console.log(`Current start point: [${currentStartPoint.x}, ${currentStartPoint.y}]`);
                console.log(`Current end point: [${currentEndPoint.x}, ${currentEndPoint.y}]`);
                console.log(`Next start point: [${nextStartPoint.x}, ${nextStartPoint.y}]`);
                console.log(`Next end point: [${nextEndPoint.x}, ${nextEndPoint.y}]`);

                // Check if the end point of current line matches the start point of next line
                const pointsMatch = isMatching(currentEndPoint, nextStartPoint);

                console.log(`Points match: ${pointsMatch}`);

                // If they don't match, reverse the current line
                if (!pointsMatch) {
                    console.log(`Reversing line ${nextConnectionOrder}`);
                    const reversedPath = [...nextLineWithNavigationWrapper.line.paths[0]].reverse();
                    const reversedLine = new Polyline({
                        paths: [reversedPath],
                        spatialReference: nextLineWithNavigationWrapper.line.spatialReference
                    });

                    const newNextStartPoint = reversedLine.getPoint(0, 0);
                    if (!isMatching(currentEndPoint, newNextStartPoint)) {
                        console.error(`Lines do not match after reversing. Line ids, ${currentLineId}, ${nextLineId}`);
                    }
                    idToLineWithNavigationWrapper.get(nextLineId).line =  reversedLine;
                }
            }
        }
    }
}

function isMatching(point1: Point, point2: Point): boolean {
    console.log(`is matching: ${point1.x} === ${point2.x} && ${point1.y} === ${point2.y};`)
    return Math.abs(point1.x - point2.x) < 1e-10 && Math.abs(point1.y - point2.y) < 1e-10;
}


function mergeParentDensePointsIntoChildLine(
    parent: Polyline,
    childStartPoint: Point,
    childEndPoint: Point,
    srs: SpatialReference
): Polyline {
    const {nearestStartPoint, nearestEndPoint} = getNearestParentStartAndEndNodes(parent, childStartPoint, childEndPoint)

    if (nearestStartPoint.vertexIndex > nearestEndPoint.vertexIndex) {
        console.error(`Ring is in incorrect order, start point is after end point`);
    }

    const newPath = [[childStartPoint.x, childStartPoint.y]]

    // vertex index is the index of the coordinate before the nearest point.
    for (let i = nearestStartPoint.vertexIndex + 1; i < nearestEndPoint.vertexIndex + 1; i++) {
        newPath.push([parent.paths[0][i][0], parent.paths[0][i][1]]);
    }

    newPath.push([childEndPoint.x, childEndPoint.y]);

    return new Polyline({
        paths: [newPath],
        spatialReference: srs,
    });
}

function pointToEastWestLine(longitude: number, latitude: number, srs: SpatialReference) {
    return new Polyline({
        paths: [[
            [longitude - ONE_ARC_SECOND, latitude],
            [longitude, latitude],
            [longitude + ONE_ARC_SECOND, latitude]
        ]],
        spatialReference: srs
    })
}

function pointToNorthSouthLine(longitude: number, latitude: number, srs: SpatialReference) {
    return new Polyline({
        paths: [[
            [longitude, latitude - ONE_ARC_SECOND],
            [longitude, latitude],
            [longitude, latitude + ONE_ARC_SECOND]
        ]],
        spatialReference: srs
    })
}