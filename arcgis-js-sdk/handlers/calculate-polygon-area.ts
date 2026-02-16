import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polygon from "@arcgis/core/geometry/Polygon";
import * as geodeticAreaOperator from "@arcgis/core/geometry/operators/geodeticAreaOperator";
import * as areaOperator from "@arcgis/core/geometry/operators/areaOperator.js";

export const calculatePolygonArea: ArcGisServiceHandlers["calculatePolygonArea"] = async (call, callback) => {
    const polygon = Polygon.fromJSON(JSON.parse(call.request.esriJsonPolygon));

    let area: number;

    if (call.request.isOnshore) {
        area = areaOperator.execute(polygon, {unit: "square-meters"});
    } else {
        if (!geodeticAreaOperator.isLoaded()) {
            await geodeticAreaOperator.load();
        }

        // We tried loxodrome and the other curvetypes but geodesic curvetype gives us the closest results to the oracle,
        // both for loxodrome and geodesic shapes
        area = geodeticAreaOperator.execute(polygon, {curveType: "geodesic"});
    }

    console.log(`Area calculated: ${area}`);
    callback(null, {area: area});
}