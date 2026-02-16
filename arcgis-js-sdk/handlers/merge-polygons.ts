import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";

export const mergePolygons: ArcGisServiceHandlers["mergePolygons"] = async (call, callback) => {
    const {inputPolygon1, inputPolygon2} = call.request;
    const polygon1 = Polygon.fromJSON(JSON.parse(inputPolygon1));
    const polygon2 = Polygon.fromJSON(JSON.parse(inputPolygon2));

    const result = unionOperator.execute(polygon1, polygon2) as Polygon;

    callback(null, {
        resultPolygon: JSON.stringify(result.toJSON())
    });
}
