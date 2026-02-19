import {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import * as equalsOperator from "@arcgis/core/geometry/operators/equalsOperator.js";
import Polygon from "@arcgis/core/geometry/Polygon";
import * as simplifyOperator from "@arcgis/core/geometry/operators/simplifyOperator.js";

export const verifyPolygonsAreTopologicallyEqual: ArcGisServiceHandlers["verifyPolygonsAreTopologicallyEqual"] = async (call, callback) => {
    const {esriJsonPolygon1, esriJsonPolygon2 } = call.request;

    const polygon1 = simplifyOperator.execute(Polygon.fromJSON(JSON.parse(esriJsonPolygon1)));
    const polygon2 = simplifyOperator.execute(Polygon.fromJSON(JSON.parse(esriJsonPolygon2)));

    const isTopologicallyEqual = equalsOperator.execute(polygon1, polygon2);
    console.log({isTopologicallyEqual});

    callback(null, {isTopologicallyEqual});
}