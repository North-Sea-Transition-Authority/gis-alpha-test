import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polygon from "@arcgis/core/geometry/Polygon";
import * as geodeticAreaOperator from "@arcgis/core/geometry/operators/geodeticAreaOperator";

export const calculatePolygonArea: ArcGisServiceHandlers["calculatePolygonArea"] = async (call, callback) => {
    const polygon = Polygon.fromJSON(JSON.parse(call.request.esriJsonPolygon));

    // TODO GISA-80: update once we make the resources available offline
    if (!geodeticAreaOperator.isLoaded()) {
      await geodeticAreaOperator.load();
    }

    // We tried loxodrome and the other curvetypes but geodesic curvetype gives us the closest results to the oracle,
    // both for loxodrome and geodesic shapes
    const area = geodeticAreaOperator.execute(polygon, { curveType: "geodesic", unit: "square-meters" });

    console.log(`Area calculated: ${area}`);
    callback(null, {area: area});
}