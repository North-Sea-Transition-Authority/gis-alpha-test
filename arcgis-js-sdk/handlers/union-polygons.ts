import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polygon from "@arcgis/core/geometry/Polygon";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";

export const unionPolygons: ArcGisServiceHandlers["unionPolygons"] = (call, callback) => {
    const polygons: Polygon[] = [];

    call.request.esriJsonPolygons.forEach(polygonString => {

        polygons.push(Polygon.fromJSON(JSON.parse(polygonString)));
    })


    if (polygons.length == 1 ) {
        console.log("Built shape from single polygon")
        callback(null, {esriJsonPolygon: call.request.esriJsonPolygons[0]})
        return;
    }

    console.log("Built shape from multi polygon")

    const unionPolygons = unionOperator.executeMany(polygons) as Polygon

    callback(null, {esriJsonPolygon: JSON.stringify(unionPolygons.toJSON())})
}
