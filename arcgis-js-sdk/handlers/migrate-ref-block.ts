import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService.ts";
import {
    findPointOfIntersectionBetweenChildPointOnBearingAndParentLine,
    GEODESIC_DENSE_POINT_METERS_INTERVAL,
    geoJsonLineInputToLinesWithNavigationTypeAndId,
    getIndexOfPointOnLine,
    isMatching,
    ONE_ARC_SECOND,
} from "./migration-utils";
import {findLineConnectingToPoint, getStartAndEndNodes, ONE_HUNDRED_METERS_ED50} from "../utils/line-utils.js";
import {
    LineWithNavigationTypeAndId,
    NavigationType,
    pointConnectsToLoxodromeLineFollowingSetBearing,
} from "./point-connects-to-line-on-set-bearing";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import Point from "@arcgis/core/geometry/Point.js";
import * as geodeticDensifyOperator from "@arcgis/core/geometry/operators/geodeticDensifyOperator.js";
import * as proximityOperator from "@arcgis/core/geometry/operators/proximityOperator.js";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";
import {GeoJsonLineInput} from "../generated/arcgisjs/GeoJsonLineInput";
import * as generalizeOperator from "@arcgis/core/geometry/operators/generalizeOperator.js";
import {GENERALIZE_TOLERANCE_DEGREES} from "./generalizePolygon";

export const migrateReferenceBlock: ArcGisServiceHandlers["migrateReferenceBlock"] = async (call, callback) => {
    console.log(`migrateReferenceBlock: starting`);
    const {referenceBlockGeoJsonLinesWithType, wkid, licenseBlockLines} = call.request;
    console.log(`referenceBlockGeoJsonLinesWithType.length=${referenceBlockGeoJsonLinesWithType.length}, licenseBlockLines.length=${licenseBlockLines.length}`);

    // Convert all ref block lines to polylines
    const idToLineWithNavigationWrapper = geoJsonLineInputToLinesWithNavigationTypeAndId(
        referenceBlockGeoJsonLinesWithType,
        wkid
    );

    // Combine consecutive geodesic lines into single line, loxodromes remain unchanged.
    const idToConnectionOrder = new Map(Array.from(referenceBlockGeoJsonLinesWithType).map((wrapper: GeoJsonLineInput) => [wrapper.oracleLineSsid, wrapper.connectionOrder]))
    const combinedGeodesicAndLoxodromes = await mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers(
        idToLineWithNavigationWrapper,
        idToConnectionOrder
    );

    // Get geodesic license lines
    const geodesicLicenseLines = licenseBlockLines
        .filter(line => line.isGeodesic)
        .map(line => Polyline.fromJSON(JSON.parse(line.esriJsonPolyline)));

    // loop through all lines and shift points
    console.log(`Starting to loop through ${combinedGeodesicAndLoxodromes?.length} combined lines`);
    for (const refBlockLineWrapper of combinedGeodesicAndLoxodromes) {
        console.log(`Processing refBlockLineWrapper id=${refBlockLineWrapper.id}, navigationType=${refBlockLineWrapper.navigationType}`);
        if (refBlockLineWrapper.navigationType == NavigationType.LOXODROME) {
            continue;
        }

        // Densify geodesics
        if (!geodeticDensifyOperator.isLoaded()) {
            await geodeticDensifyOperator.load();
        }

        refBlockLineWrapper.line = geodeticDensifyOperator.execute(
            refBlockLineWrapper.line,
            GEODESIC_DENSE_POINT_METERS_INTERVAL,
            {curveType: "geodesic", unit: "meters"}
        ) as Polyline;

        refBlockLineWrapper.line = generalizeOperator.execute(refBlockLineWrapper.line, GENERALIZE_TOLERANCE_DEGREES) as Polyline;

        const {childStartPoint: refBlockGeodesicStartPoint, childEndPoint: refBlockGeodesicEndPoint} = getStartAndEndNodes(refBlockLineWrapper.line);
        console.log(`Original start point: ${JSON.stringify([refBlockGeodesicStartPoint.x, refBlockGeodesicStartPoint.y])}`);
        console.log(`Original end point: ${JSON.stringify([refBlockGeodesicEndPoint.x, refBlockGeodesicEndPoint.y])}`);

        // For each geodesic license line that the ref block contains
        for (const licenseLine of geodesicLicenseLines) {

            // Save original start/end points before processing
            const {childStartPoint: licenseStartPoint, childEndPoint: licenseEndPoint} = getStartAndEndNodes(licenseLine);
            console.log(`license start point: ${JSON.stringify([licenseStartPoint.x, licenseStartPoint.y])}`);
            console.log(`license end point: ${JSON.stringify([licenseEndPoint.x, licenseEndPoint.y])}`);

            // Find where the license block start and end nodes intersect with the ref block
            // Taking bearing into account if the node connects to a loxodrome line following a bearing
            // Determine which license point is closest to which ref block point (handles opposite directions)
            const startToStartDistance = proximityOperator.getNearestCoordinate(licenseStartPoint, refBlockGeodesicStartPoint).distance;
            const startToEndDistance = proximityOperator.getNearestCoordinate(licenseEndPoint, refBlockGeodesicStartPoint).distance;

            // Match ref block endpoints to closest license endpoints
            const isLinesGoingSameDirection = startToStartDistance <= startToEndDistance;
            const licensePointForRefStart = isLinesGoingSameDirection ? licenseStartPoint : licenseEndPoint;
            const licensePointForRefEnd = isLinesGoingSameDirection ? licenseEndPoint : licenseStartPoint;
            console.log(`Lines direction matching: ${isLinesGoingSameDirection}`);

            let startIntersection: Point, endIntersection: Point;
            startIntersection = findIntersectionPoint(
                refBlockGeodesicStartPoint,
                licensePointForRefStart,
                licenseLine,
                refBlockLineWrapper.line,
                combinedGeodesicAndLoxodromes
            );
            endIntersection = findIntersectionPoint(
                refBlockGeodesicEndPoint,
                licensePointForRefEnd,
                licenseLine,
                refBlockLineWrapper.line,
                combinedGeodesicAndLoxodromes
            );

            if (startIntersection && endIntersection) {
                console.log("start and end intersection found, replacing segment");
                // Replace the ref block segment between the start and end nodes with the license block line
                refBlockLineWrapper.line = replaceSegment(
                    refBlockLineWrapper.line,
                    licenseLine,
                    startIntersection,
                    endIntersection,
                    isLinesGoingSameDirection
                );
            } else {
                console.log(`Intersections not found, not replacing segment startIntersection: '${startIntersection}' endIntersection: '${endIntersection}'`);
            }
        }

        // After processing, get new start/end points and update connected loxodrome lines
        const {childStartPoint: newStartPoint, childEndPoint: newEndPoint} = getStartAndEndNodes(refBlockLineWrapper.line);
        console.log(`New start point: ${JSON.stringify([newStartPoint.x, newStartPoint.y])}`);
        console.log(`New end point: ${JSON.stringify([newEndPoint.x, newEndPoint.y])}`);

        // Update connected loxodrome lines if start point changed
        if (!isMatching(refBlockGeodesicStartPoint, newStartPoint)) {
            console.log(`Start point shifted, updating connected loxodrome line`);
            const connectedLine = findLineConnectingToPoint(refBlockGeodesicStartPoint, refBlockLineWrapper.id, combinedGeodesicAndLoxodromes);
            if (connectedLine) {
                const index = getIndexOfPointOnLine(refBlockGeodesicStartPoint, connectedLine.line);
                connectedLine.line.setPoint(0, index, newStartPoint);
                console.log(`Updated loxodrome line ${connectedLine.id} at index ${index}`);
            }
        }

        // Update connected loxodrome lines if end point changed
        if (!isMatching(refBlockGeodesicEndPoint, newEndPoint)) {
            console.log(`End point shifted, updating connected loxodrome line`);
            const connectedLine = findLineConnectingToPoint(refBlockGeodesicEndPoint, refBlockLineWrapper.id, combinedGeodesicAndLoxodromes);
            if (connectedLine) {
                const index = getIndexOfPointOnLine(refBlockGeodesicEndPoint, connectedLine.line);
                connectedLine.line.setPoint(0, index, newEndPoint);
                console.log(`Updated loxodrome line ${connectedLine.id} at index ${index}`);
            }
        }
    }

    console.log(`Building result from ${combinedGeodesicAndLoxodromes.length} lines`);
    const result: { esriJsonString: string; oracleLineSsid: number }[] = [];
    combinedGeodesicAndLoxodromes.forEach((lineWrapper) => {
        console.log(`oracleLineSsid: ${lineWrapper.id} json: ${JSON.stringify(lineWrapper.line.toJSON())} `);
        result.push({
            esriJsonString: JSON.stringify(lineWrapper.line),
            oracleLineSsid: lineWrapper.id
        });
    });

    console.log(`migrateReferenceBlock: completed, returning ${result.length} lines`);
    callback(null, {esriJsonLineWithId: result});
}

async function mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers(
    idToLineWrapper: Map<number, LineWithNavigationTypeAndId>,
    idToConnectionOrder: Map<number, number>,
): Promise<LineWithNavigationTypeAndId[]> {
    console.log(`mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers: starting`);
    const geodesicEntries = Array.from(idToLineWrapper.values())
        .filter((wrapper => wrapper.navigationType === NavigationType.GEODESIC))
        .sort((a, b) =>  idToConnectionOrder.get(a.id) - idToConnectionOrder.get(b.id));
    console.log(`geodesicEntries.length=${geodesicEntries.length}`);

    if (geodesicEntries.length <= 1) {
        console.log(`1 or less geodesic lines, no adjacent lines merged.`);
        return Array.from(idToLineWrapper.values());
    }

    const firstOrder = Math.min(...idToConnectionOrder.values());
    const lastOrder = Math.max(...idToConnectionOrder.values());

    const processedLines: LineWithNavigationTypeAndId[] = [];
    const mergedIds: number[] = [];

    for (const currentWrapper of geodesicEntries) {
        const currentConnectionOrder = idToConnectionOrder.get(currentWrapper.id);

        if (mergedIds.includes(currentWrapper.id)) {
            continue;
        }

        let mergedLine = currentWrapper.line;
        let maxConnectionOrder = currentConnectionOrder;

        for (const nextWrapper of geodesicEntries) {
            const nextConnectionOrder = idToConnectionOrder.get(nextWrapper.id);

            if (mergedIds.includes(nextWrapper.id) || nextWrapper.id === currentWrapper.id) {
                continue;
            }

            if (nextConnectionOrder === maxConnectionOrder + 1
                || (firstOrder === currentConnectionOrder && nextConnectionOrder === lastOrder)
            ) {
                mergedLine = unionOperator.execute(mergedLine, nextWrapper.line) as Polyline;
                mergedIds.push(nextWrapper.id);
                maxConnectionOrder = nextConnectionOrder;
            }
        }

        processedLines.push({
            line: mergedLine,
            navigationType: NavigationType.GEODESIC,
            id: currentWrapper.id,
        });
    }

    // Add any non-merged and loxodrome lines to our processedLine list
    Array.from(idToLineWrapper.values()).forEach(wrapper => {
        if (processedLines.find(processedLine => processedLine.id === wrapper.id)) {
            return;
        }
        if (mergedIds.includes(wrapper.id)) {
            return;
        }
        processedLines.push(wrapper);
    })

    console.log(`mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers: completed, returning ${processedLines.length} lines`);
    return processedLines;
}

function findIntersectionPoint(
    refBlockPoint: Point,
    licensePoint: Point,
    licenseLine: Polyline,
    refBlockLine: Polyline,
    allLines: LineWithNavigationTypeAndId[]
): Point | undefined {
    console.log(`refblockPoint: ${refBlockPoint.x}, ${refBlockPoint.y} licensePoint: ${licensePoint.x}, ${licensePoint.y}`);
    // Check if the point connects to a loxodrome line following a bearing
    const lineOnBearing = pointConnectsToLoxodromeLineFollowingSetBearing(refBlockPoint, allLines);

    if (lineOnBearing) {
        // Find intersection using bearing
        const intersectionWithRefPoint =  findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(
            refBlockPoint,
            lineOnBearing.setBearing,
            licenseLine,
            ONE_ARC_SECOND * 30
        );

        if (intersectionWithRefPoint) {
            console.log(`intersectionWithRefPoint ${intersectionWithRefPoint.x}, ${intersectionWithRefPoint.y}`);
            return intersectionWithRefPoint;
        }

        const intersectionWithLicensePoint =  findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(
            licensePoint,
            lineOnBearing.setBearing,
            refBlockLine,
            ONE_ARC_SECOND * 30
        );
        if (intersectionWithLicensePoint) {
            console.log(`intersectionWithLicensePoint ${intersectionWithLicensePoint.x}, ${intersectionWithLicensePoint.y}`);
        }
        return intersectionWithLicensePoint;
    }

    // Find the nearest point on the ref block line
    const nearestToRefBlockPoint = proximityOperator.getNearestCoordinate(licenseLine, refBlockPoint);
    const nearestToLicensePoint = proximityOperator.getNearestCoordinate(refBlockLine, licensePoint);
    const nearest = nearestToRefBlockPoint.distance < nearestToLicensePoint.distance ? nearestToRefBlockPoint : nearestToLicensePoint;
    if (nearest.distance > ONE_HUNDRED_METERS_ED50) {
        console.log(`nearest distance is ${nearest.distance}`);
        return undefined
    }
    console.log("nearest non bearing point used");

    return nearest.coordinate;
}

export function replaceSegment(
    refBlockLine: Polyline,
    licenseLine: Polyline,
    startPoint: Point,
    endPoint: Point,
    isLinesGoingSameDirection: boolean
): Polyline {

    const [fromIndex, toIndex] = [getIndexOfPointOnLine(startPoint, refBlockLine), getIndexOfPointOnLine(endPoint, refBlockLine)]
        .sort((a, b) => a - b);

    // Only part of the license line might cover the ref block so we only want to copy that section.
    const licenseStartIndex = getIndexOfPointOnLine(startPoint, licenseLine);
    const licenseEndIndex = getIndexOfPointOnLine(endPoint, licenseLine);
    const [licenseFromIndex, licenseToIndex] = [licenseStartIndex, licenseEndIndex]
        .sort((a, b) => a - b);

    const licensePoints = licenseLine.paths[0];
    const fromPoint = licenseStartIndex <= licenseEndIndex ? startPoint : endPoint;
    const toPoint = licenseStartIndex <= licenseEndIndex ? endPoint : startPoint;

    const licenseSegment = licensePoints.slice(licenseFromIndex, licenseToIndex + 1);
    const licenseMiddle = isLinesGoingSameDirection
        ? [[fromPoint.x, fromPoint.y], ...licenseSegment, [toPoint.x, toPoint.y]]
        : [[toPoint.x, toPoint.y], ...licenseSegment.reverse(), [fromPoint.x, fromPoint.y]];

    const newPath = [
        ...refBlockLine.paths[0].slice(0, fromIndex),
        ...licenseMiddle,
        ...refBlockLine.paths[0].slice(toIndex + 1)
    ].filter((currentPoint, index, points) => {
        return index === 0
            || currentPoint[0] !== points[index - 1][0]
            || currentPoint[1] !== points[index - 1][1]
    });

    return new Polyline({
        paths: [newPath],
        spatialReference: refBlockLine.spatialReference
    });
}
