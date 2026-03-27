import grpc from '@grpc/grpc-js';
import Polyline from "@arcgis/core/geometry/Polyline.js";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";
import * as containsOperator from "@arcgis/core/geometry/operators/containsOperator.js";
import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import * as multiPartToSinglePartOperator from "@arcgis/core/geometry/operators/multiPartToSinglePartOperator.js";

export const findParentLine: ArcGisServiceHandlers["findParentLine"] = (call, callback) => {
    try {
        const { parents, children } = call.request;

        // --- PHASE 1: Prepare the data ---
        const parentGeoms = parents.map(p => ({
            id: p.id,
            geometry: Polyline.fromJSON(JSON.parse(p.esriJsonPolyline))
        }));

        // Map<ParentID, Polyline[]>
        const groups = new Map<string, Polyline[]>();
        const orphans = [];

        // --- PHASE 2: Match each child line to a parent line ---
        children.forEach(childJson => {
            const lineSegment = Polyline.fromJSON(JSON.parse(childJson));

            let foundParentId = null;

            for (const parent of parentGeoms) {
                if (containsOperator.execute(parent.geometry, lineSegment)) {
                    foundParentId = parent.id;
                    break;
                }
            }

            if (foundParentId) {
                if (!groups.has(foundParentId)) {
                    groups.set(foundParentId, []);
                }
                groups.get(foundParentId).push(lineSegment);
            } else {
                orphans.push(childJson);
            }
        });

        // --- PHASE 3: Merge grouped polylines into a single geometry ---
        const reconstructedLines = [];

        groups.forEach((childPolylines, parentId) => {
            try {
                const mergedPolyline = unionOperator.executeMany(childPolylines);

                // Separate lines with multiple paths into single-path lines
                const singlePathPolylines = multiPartToSinglePartOperator.executeMany([mergedPolyline]) as Polyline[];

                if (singlePathPolylines) {
                    singlePathPolylines.forEach(polyline => {
                        reconstructedLines.push({
                            parentId: parentId,
                            esriJsonPolyline: JSON.stringify(polyline.toJSON())
                        });
                    })
                }
            } catch (err) {
                console.error(`Batch union failed for parent ${parentId}`, err);
            }
        });

        callback(null, {
            lines: reconstructedLines,
            orphanedChildrenJson: orphans
        });

    } catch (error) {
        console.error("Error in reconstructParentLines:", error);
        callback({
            code: grpc.status.INTERNAL,
            message: error instanceof Error ? error.message : "Reconstruction failed"
        }, null);
    }
};