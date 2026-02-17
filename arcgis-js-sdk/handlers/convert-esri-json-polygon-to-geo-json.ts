import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import SpatialReference from "@arcgis/core/geometry/SpatialReference.js";
import * as projectOperator from "@arcgis/core/geometry/operators/projectOperator.js";
import * as Terraformer from "@terraformer/arcgis";

export const convertEsriJsonPolygonToGeoJson: ArcGisServiceHandlers["convertEsriJsonPolygonToGeoJson"] = async (call, callback) => {
  console.log("projectPolygon");

  if (!projectOperator.isLoaded()) {
    await projectOperator.load();
  }

  const sourcePolygon = Polygon.fromJSON(JSON.parse(call.request.esriJsonPolygon));

  //GeoJSON only supports WGS84, the World Geodetic System 1984. wkid: 4326
  const targetSrs = new SpatialReference({wkid: 4326});

  const projectedPolygon = projectOperator.execute(sourcePolygon, targetSrs) as Polygon;

  const geoJson = Terraformer.arcgisToGeoJSON(projectedPolygon.toJSON());
  callback(null, {geoJsonPolygon: JSON.stringify(geoJson)});
}
