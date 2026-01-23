import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import Point from "@arcgis/core/geometry/Point.js";
import * as simplifyOperator from "@arcgis/core/geometry/operators/simplifyOperator.js";
import * as equalsOperator from "@arcgis/core/geometry/operators/equalsOperator.js";

export const validatePolygonReconstruction: ArcGisServiceHandlers["validatePolygonReconstruction"] = (call, callback) => {
    console.log('validatePolygonReconstruction');
    const reconstructedPolygon = validatePolygonCanBeReconstructed(call.request.lines)

    if (!reconstructedPolygon.isValid) {
        callback(null, {isValid: false});
        return;
    }

    const areSpatiallyEqual = checkSpatialEquality(reconstructedPolygon.rings, reconstructedPolygon.wkid, call.request.originalPolygonEsriJson);

    callback(null, {isValid: areSpatiallyEqual});
};

function validatePolygonCanBeReconstructed(lines: any[]): {isValid: boolean, rings: number[][][], wkid: number} {
    const wkid = Polyline.fromJSON(JSON.parse(lines[0].esriJsonPolyline)).spatialReference.wkid;

    const ringToLines = new Map<number, any>();
    for (const line of lines) {
        if (!ringToLines.has(line.ringNumber)) {
            ringToLines.set(line.ringNumber, []);
        }
        ringToLines.get(line.ringNumber)!.push(line);
    }

    let satisfiesContinuity = true;
    const constructedRings = []
    for (const [ringNumber, ringLines] of ringToLines) {
        ringLines.sort((a, b) => a.connectionOrder - b.connectionOrder);
        const ringPath = [];
        let previousEndPoint: Point = null;

        for (let i = 0; i < ringLines.length; i++) {
            const currentLine = ringLines[i];
            const polyline = Polyline.fromJSON(JSON.parse(currentLine.esriJsonPolyline))
            const points = polyline.paths[0]; //line should have 1 path

            // check line continuity
            if (i > 0 && previousEndPoint) {
                const currentLineStart = points[0]; // [x, y]
                if (currentLineStart[0] !== previousEndPoint.x ||
                    currentLineStart[1] !== previousEndPoint.y) {

                    console.error(`Gap in Ring ${ringNumber}: Line ${i-1} End [${previousEndPoint.x}, ${previousEndPoint.y}] != Line ${i} Start [${currentLineStart[0]}, ${currentLineStart[1]}]`);
                    satisfiesContinuity = false;
                    break;
                }
            }

            //Need to remove the first coordinate from each line. Because it would be the same as the last coordinate
            //of the previous line.
            if (i == 0) {
                ringPath.push(...points);
            } else {
                ringPath.push(...points.slice(1));
            }

            previousEndPoint = polyline.getPoint(0, polyline.paths[0].length - 1);
        }

        if (!satisfiesContinuity) {
            break;
        }
        constructedRings.push(ringPath);
    }

    if (!satisfiesContinuity) {
        console.log('Line segments failed continuity check.')
        return { isValid: false, rings: [], wkid: 0 };
    }

    const polygon = new Polygon({
        rings: constructedRings,
        spatialReference: {wkid: wkid}
    });
    let result;

    try {
        const simplifiedPolygon = simplifyOperator.execute(polygon) as Polygon;
        // Check if it exists and has at least one ring with points
        result = !!(simplifiedPolygon && simplifiedPolygon.rings && simplifiedPolygon.rings.length > 0);
    } catch (e) {
        console.error("Polygon is not valid");
        result = false;
    }

    console.log('Polygon can be reconstructed:');
    console.log(result);
    return { isValid: result, rings: constructedRings, wkid: wkid };
}

function checkSpatialEquality(rings: number[][][], wkid: number, originalPolygonEsriJson: string): boolean {
    const newPolygon = new Polygon({
        rings: rings,
        spatialReference: { wkid: wkid }
    });
    const newPolygonSimplified = simplifyOperator.execute(newPolygon) as Polygon;

    const originalPolygon = Polygon.fromJSON(JSON.parse(originalPolygonEsriJson));
    const originalPolygonSimplified = simplifyOperator.execute(originalPolygon) as Polygon;

    //checks topological equality Ignores number of lines/vertices.
    const result = equalsOperator.execute(newPolygonSimplified, originalPolygonSimplified);
    console.log("Spatially equal:");
    console.log(result);
    return result;
}