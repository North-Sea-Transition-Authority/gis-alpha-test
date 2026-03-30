import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService.ts";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import * as geodeticDensifyOperator from "@arcgis/core/geometry/operators/geodeticDensifyOperator.js";
import * as Terraformer from "@terraformer/arcgis";
import grpc from "@grpc/grpc-js";
import {
    NavigationType,
} from "./point-connects-to-line-on-set-bearing";
import {
    findParentLine,
    getNearestParentStartAndEndNodes,
    getStartAndEndNodes
} from "../utils/line-utils.js";
import {
    GEODESIC_DENSE_POINT_METERS_INTERVAL,
    geoJsonLineInputToLinesWithNavigationTypeAndId,
    mergeParentDensePointsIntoChildLine,
    migrateParentPolygon,
    shiftNodeAndUpdateConnectedLine,
    fixDirectionOfAllLines
} from "./migration-utils.js";

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

            polyline = geodeticDensifyOperator.execute(polyline, GEODESIC_DENSE_POINT_METERS_INTERVAL, {curveType: "geodesic", unit: "meters"}) as Polyline;
        }
    }

    const esriJsonString = JSON.stringify(polyline);
    console.log(`Output esrijson: ${esriJsonString}`)
    callback(null, {esriJsonString: esriJsonString});
}

export const batchConvertGeoJsonLinesToEsriJsonLines: ArcGisServiceHandlers['batchConvertGeoJsonLinesToEsriJsonLines'] = async (call, callback) => {
    const {linesWithType, wkid, parentLineJsons} = call.request;
    console.log(`Batch convert: ${linesWithType.length} lines, srs: ${wkid}`);

    const idToLineWithNavigationWrapper = geoJsonLineInputToLinesWithNavigationTypeAndId(linesWithType, wkid);

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
