import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import SpatialReference from "@arcgis/core/geometry/SpatialReference.js";
import * as projectOperator from "@arcgis/core/geometry/operators/projectOperator.js";

export const projectPolygons: ArcGisServiceHandlers["projectPolygons"] = async (call, callback) => {
  console.log("projectPolygon");

  if (!projectOperator.isLoaded()) {
    await projectOperator.load();
  }

  const sourcePolygons = call.request.esriJsonPolygons.map(jsonPolygon => Polygon.fromJSON(JSON.parse(jsonPolygon)));

  //GeoJSON only supports WGS84, the World Geodetic System 1984. wkid: 4326
  const targetSrs = new SpatialReference({wkid: 4326});

  const projectedPolygons = sourcePolygons.map(polygon => projectOperator.execute(polygon, targetSrs) as Polygon);
  callback(
      null,
      {projectedPolygons: projectedPolygons.map(polygon  => JSON.stringify(polygon.toJSON()))}
  )
}
