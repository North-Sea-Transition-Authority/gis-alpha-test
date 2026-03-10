import Polyline from "@arcgis/core/geometry/Polyline.js";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import * as containsOperator from "@arcgis/core/geometry/operators/containsOperator.js";
import * as cutOperator from "@arcgis/core/geometry/operators/cutOperator.js";
import * as multiPartToSinglePartOperator from "@arcgis/core/geometry/operators/multiPartToSinglePartOperator.js";
import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService.js";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";
import * as equalsOperator from '@arcgis/core/geometry/operators/equalsOperator';

/**
 * Explodes a polyline into individual 2-point line segments.
 */
function explodePolylineIntoSegments(polyline: Polyline): Polyline[] {
    const segments: Polyline[] = [];

    polyline.paths.forEach(path => {
        for (let i = 0; i < path.length - 1; i++) {
            const startPoint = path[i];
            const endPoint = path[i + 1];
            const segment = new Polyline({
                paths: [[startPoint, endPoint]],
                spatialReference: polyline.spatialReference
            });
            segments.push(segment);
        }
    });

    return segments;
}

/**
 * Removes overlapping segments from a polyline.
 * If segment B is completely contained within segment A, segment B is removed.
 * This handles cases where a cutter line backtracks on itself.
 */
function removeOverlappingSegments(polyline: Polyline): Polyline {
    console.log(`Original split line: \n\n${JSON.stringify(polyline.toJSON())}\n\n`)

    const segments = explodePolylineIntoSegments(polyline);

    if (segments.length <= 1) {
        return polyline;
    }

    // Track which segments should be removed (contained within another segment)
    const segmentsToRemove = new Set<number>();

    // Compare all pairs of segments
    for (let currentIndex = 0; currentIndex < segments.length; currentIndex++) {
        if (segmentsToRemove.has(currentIndex)) {
            continue;
        }

        for (let nextIndex = currentIndex + 1; nextIndex < segments.length; nextIndex++) {
            if (segmentsToRemove.has(nextIndex)) {
                continue;
            }

            if (containsOperator.execute(segments[currentIndex], segments[nextIndex])) {
                segmentsToRemove.add(nextIndex);
            }
            
            if (containsOperator.execute(segments[nextIndex], segments[currentIndex])) {
                segmentsToRemove.add(currentIndex);
            }
        }
    }

    // If no segments need to be removed, return original polyline
    if (segmentsToRemove.size === 0) {
        return polyline;
    }

    console.log(`Removing ${segmentsToRemove.size} overlapping segment(s) from cutter line`);

    // Rebuild the polyline from remaining segments
    const remainingSegments = segments.filter((_, index) => !segmentsToRemove.has(index));

    if (remainingSegments.length === 0) {
        return polyline;
    }

    const cutter = unionOperator.executeMany(remainingSegments) as Polyline;

    console.log(`After removing overlapping segments cutter line now has ${cutter.paths.length} paths`);
    console.log(`New split line: \n\n${JSON.stringify(cutter.toJSON())}\n\n`)
    console.log(`Split lines topologically equal after removing segments: ${equalsOperator.execute(polyline, cutter)}`)

    return cutter
}

export const splitPolygon: ArcGisServiceHandlers["splitPolygon"] = (call, callback) => {
    console.log("Split polygon");

    const target = Polygon.fromJSON(JSON.parse(call.request.target.esriJsonPolygon));
    const rawCutter = Polyline.fromJSON(JSON.parse(call.request.esriJsonCutter));

    // Remove any overlapping segments from the cutter line (e.g., if the line backtracks on itself)
    const cutter = removeOverlappingSegments(rawCutter);

    const cutResults = cutOperator.execute(target, cutter) as Polygon[];

    let polygons: Polygon[] = [];
    //Only separate polygons if a cut actually took place.
    if (cutResults.length > 0) {
        // cutresults may contain disjointed polygons, (one polygon that should actually be multiple polygons)
        // this operation splits those disjointed polygons into separate polygons
        polygons = multiPartToSinglePartOperator.executeMany(cutResults) as Polygon[];
    }

    console.log(`Created ${polygons.length} pieces`);

    const response = (polygons || []).map((poly) => {
        return {
            esriJsonPolygon: JSON.stringify(poly.toJSON())
        };
    });

    callback(null, { polygons: response });
};
