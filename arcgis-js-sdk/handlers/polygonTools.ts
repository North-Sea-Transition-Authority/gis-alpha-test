import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";

export const explodePolygon: ArcGisServiceHandlers["explodePolygon"] = (call, callback) => {
    console.log("Explode polygon");
    const polygon = Polygon.fromJSON(JSON.parse(call.request.target.esriJsonPolygon));
    const polylines = [];

    // Loop through each ring
    polygon.rings.forEach(ring => {
        for (let i = 0; i < ring.length - 1; i++) {
            const startPoint = ring[i];
            const endPoint = ring[i + 1];
            const segment = new Polyline({
                paths: [[startPoint, endPoint]],
                spatialReference: polygon.spatialReference
            });

            polylines.push(segment.toJSON());
        }
    });

    //Convert polylines to esriJson for the proto response
    const esriJsonLines = polylines.map(s => JSON.stringify(s));

    callback(null, {esriJsonLines});
};