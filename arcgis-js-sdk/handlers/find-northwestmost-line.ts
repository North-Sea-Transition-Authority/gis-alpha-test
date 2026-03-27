import {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polyline from "@arcgis/core/geometry/Polyline";
import Point from "@arcgis/core/geometry/Point";
import * as proximityOperator from "@arcgis/core/geometry/operators/proximityOperator.js";

export const findNorthwestmostLine: ArcGisServiceHandlers["findNorthwestmostLine"] = (call, callback) => {
    console.log('findNorthwestmostLine');

    const lines = call.request.lines;

    if (lines.length === 0) {
        callback(new Error("No lines provided"));
        return;
    }

    const startPoints = [];
    const polylines: Polyline[] = [];
    for (const line of lines) {
        const polyLine = Polyline.fromJSON(JSON.parse(line.polyLineEsriJson));
        const startPoint = polyLine.getPoint(0, 0);
        polylines.push(polyLine);
        startPoints.push({
            startPoint,
            id: line.id
        });
    }

    //Find the most northern (max Y) and most western (min X) coordinates
    let maxY = startPoints[0].startPoint.y;
    let minX = startPoints[0].startPoint.x;

    for (const pointWithId of startPoints) {
        if (pointWithId.startPoint.y > maxY) {
            maxY = pointWithId.startPoint.y;
        }
        if (pointWithId.startPoint.x < minX) {
            minX = pointWithId.startPoint.x;
        }
    }

    // Reference point at northwestern-most point
    const nwReferencePoint = new Point({
        x: minX,
        y: maxY,
        spatialReference: { wkid: polylines[0].spatialReference.wkid }
    });

    console.log(`NW Reference Point: (${nwReferencePoint.x}, ${nwReferencePoint.y})`);

    //Find the closest start point to NW reference using the proximity operator
    let closestId;
    let minDistance = Number.POSITIVE_INFINITY;

    for (const pointWithId of startPoints) {
        const point = pointWithId.startPoint;
        // Use ArcGIS proximity operator to calculate distance between points
        const proximityResult = proximityOperator.getNearestVertex(point, nwReferencePoint);
        const distance = proximityResult.distance;

        if (distance < minDistance) {
            minDistance = distance;
            closestId = pointWithId.id;
        }
    }

    console.log(`Northwestmost line id: ${closestId}, distance: ${minDistance}`);
    callback(null, { lineId: closestId });
};
