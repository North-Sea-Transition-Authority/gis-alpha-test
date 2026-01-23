import {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polyline from "@arcgis/core/geometry/Polyline";

export const getStartAndEndPoints: ArcGisServiceHandlers["getStartAndEndPoints"] = (call, callback) => {
    console.log('getStartEndPoints')
    const lines = call.request.lines
    const points = [];
    for (const line of lines) {
        const polyLine = Polyline.fromJSON(JSON.parse(line.polyLineEsriJson));
        const path = polyLine.paths[0];
        const startPoint = polyLine.getPoint(0, 0);
        const endPoint = polyLine.getPoint(0, path.length - 1);
        const pointResponse = {
            lineId: line.id,
            startPoint: {
                x: startPoint.x,
                y: startPoint.y
            },
            endPoint: {
                x: endPoint.x,
                y: endPoint.y
            }
        };
        points.push(pointResponse);
    }
    callback(null, {lines: points});
}