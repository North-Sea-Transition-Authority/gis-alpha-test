import Polyline from "@arcgis/core/geometry/Polyline.js";
import * as containsOperator from "@arcgis/core/geometry/operators/containsOperator.js";
import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import {findParentLine, getStartAndEndNodes} from "../utils/line-utils";

export const verifyChildGeodesicLinesOverlapParents: ArcGisServiceHandlers["verifyChildGeodesicLinesOverlapParents"] = (call, callback) => {
    const {parentLines, childLines} = call.request;

    const parentGeodesicLines = parentLines
        .filter((line) => line.isGeodesic)
        .map((line) => line.esriJsonPolyline);

    const orphanedChildLinesJson: string[] = [];
    const nonOverlappingChildLinesJson: string[] = [];

    childLines
        .filter((childLine) => childLine.isGeodesic)
        .forEach((childLine) => {
            const child = Polyline.fromJSON(JSON.parse(childLine.esriJsonPolyline));

            const {childStartPoint, childEndPoint} = getStartAndEndNodes(child);

            const parent = findParentLine(parentGeodesicLines, childStartPoint, childEndPoint);

            if (parent === undefined) {
                orphanedChildLinesJson.push(childLine.esriJsonPolyline);
                return;
            }

            if (!containsOperator.execute(parent, child)) {
                nonOverlappingChildLinesJson.push(childLine.esriJsonPolyline);
            }
        });

    orphanedChildLinesJson.forEach((line) => console.warn(`Parent not found for child line: ${line}`));
    nonOverlappingChildLinesJson.forEach((line) => console.warn(`Non-overlapping child line: ${line}`));
    callback(null, {isValid: orphanedChildLinesJson.length === 0 && nonOverlappingChildLinesJson.length === 0});
};
