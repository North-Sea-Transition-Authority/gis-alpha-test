import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polyline from "@arcgis/core/geometry/Polyline";
import * as densifyOperator from "@arcgis/core/geometry/operators/densifyOperator.js";

export const densifyLoxodromePolyline: ArcGisServiceHandlers["DensifyLoxodromePolyline"] = async (call, callback) => {
    let polyline: Polyline = Polyline.fromJSON(JSON.parse(call.request.esriJsonPolyline))
    console.log(`densifyLoxodromePolyline, input: ${polyline.toJSON()["paths"]}\n\n`);

    // The max segment length is derived from the spatial reference unit, which for ED50 is degrees.
    // There are 3600 arc seconds in 1 degrees. So this equates to 0.0055... degrees
    polyline = densifyOperator.execute(polyline, parseFloat((20 / 3600).toFixed(11))) as Polyline;

    console.log(`densifyLoxodromePolyline, output: ${polyline.toJSON()["paths"]}\n\n`);

    callback(null, {esriJsonPolyline: JSON.stringify(polyline.toJSON())});
}
