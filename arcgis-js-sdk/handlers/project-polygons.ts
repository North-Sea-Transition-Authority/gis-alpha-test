import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import SpatialReference from "@arcgis/core/geometry/SpatialReference.js";
import * as projectOperator from "@arcgis/core/geometry/operators/projectOperator.js";
import {ed50ToWgs84Transformation} from "../utils/projection-utils";

export const projectPolygons: ArcGisServiceHandlers["projectPolygons"] = async (call, callback) => {
  console.log("projectPolygon");

  if (!projectOperator.isLoaded()) {
    await projectOperator.load();
  }

  const sourcePolygons = call.request.esriJsonPolygons.map(jsonPolygon => Polygon.fromJSON(JSON.parse(jsonPolygon)));

  const wgs84 = new SpatialReference({wkid: 4326}); //WGS84

  const projectedPolygons = sourcePolygons.map(polygon => projectOperator.execute(polygon, wgs84, {geographicTransformation: ed50ToWgs84Transformation}) as Polygon);
  callback(
      null,
      {projectedPolygons: projectedPolygons.map(polygon  => JSON.stringify(polygon.toJSON()))}
  )
}
