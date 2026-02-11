import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService.ts";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import * as geodeticDensifyOperator from "@arcgis/core/geometry/operators/geodeticDensifyOperator.js";
import * as Terraformer from "@terraformer/arcgis";
import * as proximityOperator from "@arcgis/core/geometry/operators/proximityOperator.js";
import Point from "@arcgis/core/geometry/Point";
import SpatialReference from "@arcgis/core/geometry/SpatialReference";
import grpc from "@grpc/grpc-js";

const FIVE_CM_IN_DEGREES_AT_48N_ED50 = 0.000000670;

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

function findParentLine(
    parentLines: string[],
    childStartPoint: Point,
    childEndPoint: Point
): Polyline | undefined {
    let parent: Polyline | undefined = undefined;
    let closestStartDistance = 999999;
    let closestEndDistance = 999999;

    parentLines.forEach((line) => {
        const possibleParent = Polyline.fromJSON(JSON.parse(line));

        const {nearestStartPoint, nearestEndPoint} = getNearestParentStartAndEndNodes(possibleParent, childStartPoint, childEndPoint)

        if (nearestStartPoint.distance < closestStartDistance && nearestEndPoint.distance < closestEndDistance) {
            closestStartDistance = nearestStartPoint.distance;
            closestEndDistance = nearestEndPoint.distance;
            parent = possibleParent;
        }
    })

    if (closestStartDistance > FIVE_CM_IN_DEGREES_AT_48N_ED50 || closestEndDistance > FIVE_CM_IN_DEGREES_AT_48N_ED50) {
        console.error(`parent line is too far away. start difference: ${closestStartDistance} end difference: ${closestEndDistance}`);
        return undefined;
    }

    console.log(`parent line: ${parent} start point difference: ${closestStartDistance} end point difference: ${closestEndDistance}`);
    return parent;
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

    const newPath = [[nearestStartPoint.coordinate.x, nearestStartPoint.coordinate.y]]

    // vertex index is the index of the coordinate before the nearest point.
    for (let i = nearestStartPoint.vertexIndex + 1; i < nearestEndPoint.vertexIndex + 1; i++) {
        newPath.push([parent.paths[0][i][0], parent.paths[0][i][1]]);
    }

    newPath.push([nearestEndPoint.coordinate.x, nearestEndPoint.coordinate.y]);

    return new Polyline({
        paths: [newPath],
        spatialReference: srs,
    });
}

function getStartAndEndNodes(polyline: Polyline) {
    const childStartPoint = polyline.getPoint(0, 0);
    const childEndPoint = polyline.getPoint(0, polyline.paths[0].length - 1);
    return {childStartPoint, childEndPoint};
}

function getNearestParentStartAndEndNodes(
    parent: Polyline,
    childStartPoint: Point,
    childEndPoint: Point,
) {
    const nearestStartPoint = proximityOperator.getNearestCoordinate(parent, childStartPoint);
    const nearestEndPoint = proximityOperator.getNearestCoordinate(parent, childEndPoint);
    return {nearestStartPoint, nearestEndPoint};
}