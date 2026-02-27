import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import * as generalizeOperator from "@arcgis/core/geometry/operators/generalizeOperator.js";

// Tolerance for generalizing line geometries in degrees (~1 mm on ED50).
export const GENERALIZE_TOLERANCE_DEGREES = 0.00000001;

export const generalizePolygon: ArcGisServiceHandlers["generalizePolygon"] = async (call, callback) => {
    const polygonJson = call.request.esriPolygon;
    const polygon = Polygon.fromJSON(JSON.parse(polygonJson));

    //remove vertices that are on straight lines.
    const result = generalizeOperator.execute(polygon, GENERALIZE_TOLERANCE_DEGREES) as Polygon;

    callback(null, {
        esriPolygon: JSON.stringify(result.toJSON())
    });
}
