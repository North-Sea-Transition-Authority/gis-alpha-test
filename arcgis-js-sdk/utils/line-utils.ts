import Polyline from "@arcgis/core/geometry/Polyline.js";
import Point from "@arcgis/core/geometry/Point.js";
import * as proximityOperator from "@arcgis/core/geometry/operators/proximityOperator.js";
import {LineWithNavigationTypeAndId} from "../handlers/point-connects-to-line-on-set-bearing";

export const FIVE_CM_IN_DEGREES_AT_48N_ED50 = 0.000000670;

export function getStartAndEndNodes(polyline: Polyline) {
    const childStartPoint = polyline.getPoint(0, 0);
    const childEndPoint = polyline.getPoint(0, polyline.paths[0].length - 1);
    return {childStartPoint, childEndPoint};
}

export function findParentLine(
    parentLines: string[],
    childStartPoint: Point,
    childEndPoint: Point
): Polyline | undefined {
    let parent: Polyline | undefined = undefined;
    let closestStartDistance = 999999;
    let closestEndDistance = 999999;

    parentLines.forEach((line) => {
        const possibleParent = Polyline.fromJSON(JSON.parse(line));

        const {
            nearestStartPoint,
            nearestEndPoint
        } = getNearestParentStartAndEndNodes(possibleParent, childStartPoint, childEndPoint)

        if (nearestStartPoint.distance <= closestStartDistance && nearestEndPoint.distance <= closestEndDistance) {
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

export function getNearestParentStartAndEndNodes(
    parent: Polyline,
    childStartPoint: Point,
    childEndPoint: Point,
) {
    const nearestStartPoint = proximityOperator.getNearestCoordinate(parent, childStartPoint);
    const nearestEndPoint = proximityOperator.getNearestCoordinate(parent, childEndPoint);
    return {nearestStartPoint, nearestEndPoint};
}

export const findLineConnectingToPoint = (point: Point, targetLineId: number, lines: LineWithNavigationTypeAndId[]): LineWithNavigationTypeAndId => {
    return lines.find((lineWrapper: LineWithNavigationTypeAndId) => {
        if (lineWrapper.id === targetLineId) {
            return false;
        }
        const line = lineWrapper.line;
        const startPoint = line.getPoint(0, 0);
        const endPoint = line.getPoint(0, line.paths[0].length - 1);

        if (point.equals(startPoint)) {
            return true;
        } else return point.equals(endPoint);
    });
}