import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polyline from "@arcgis/core/geometry/Polyline";
import SpatialReference from "@arcgis/core/geometry/SpatialReference";
import * as simplifyOperator from "@arcgis/core/geometry/operators/simplifyOperator.js";


export const coordinatesToPolyline: ArcGisServiceHandlers["coordinatesToPolyline"] = async (call, callback) => {
    console.log("coordinatesToPolyline");
    const coordinates = call.request.coordinates;
    const srsWkid = call.request.srsWkid;
    const coordinatesPath = coordinates.map(coordinate => [coordinate.x, coordinate.y]);
    const polyline = new Polyline({
        paths: [coordinatesPath],
        spatialReference: new SpatialReference({
            wkid: srsWkid
        })
    });
    const polylineSimplified = simplifyOperator.execute(polyline) as Polyline;
    callback(null, {
        polylineEsriJson: JSON.stringify(polylineSimplified.toJSON())
    });
}
