import Polyline from "@arcgis/core/geometry/Polyline.js";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";
import * as generalizeOperator from "@arcgis/core/geometry/operators/generalizeOperator.js";
import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import {GENERALIZE_TOLERANCE_DEGREES} from "./generalizePolygon";

export const mergeAndGeneralizeLines: ArcGisServiceHandlers["mergeAndGeneralizeLines"] = (call, callback) => {
    const polylinesJson = call.request.esriPolylines;
    const polylines = polylinesJson.map(line => Polyline.fromJSON(JSON.parse(line)));
    const result = unionOperator.executeMany(polylines) as Polyline;

    //remove vertices that are on straight lines.
    const cleanResult = generalizeOperator.execute(result, GENERALIZE_TOLERANCE_DEGREES) as Polyline;
    callback(null, {
        esriPolyline: JSON.stringify(cleanResult.toJSON())
    })
};