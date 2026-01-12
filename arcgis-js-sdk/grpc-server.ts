import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import path from 'path';
import * as Terraformer from "@terraformer/arcgis";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import type {ProtoGrpcType} from "./generated/ArcGisJs.ts";
import type {ArcGisServiceHandlers} from "./generated/arcgisjs/ArcGisService.ts";
import Polygon from "@arcgis/core/geometry/Polygon";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";
import * as cutOperator from '@arcgis/core/geometry/operators/cutOperator.js';
import * as geodeticDensifyOperator from "@arcgis/core/geometry/operators/geodeticDensifyOperator";

const PROTO_PATH = path.join("../src/main/proto", 'ArcGisJs.proto');
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true
});

const arcGisJsProto: ProtoGrpcType["arcgisjs"] = grpc.loadPackageDefinition(packageDefinition).arcgisjs as any;

const convertGeoJsonLineToEsriJsonLine: ArcGisServiceHandlers['convertGeoJsonLineToEsriJsonLine'] = async (call, callback) => {
  const { geoJsonString, wkid, isGeodesic } = call.request;
  console.log(`Input srs: ${wkid} isGeodesic: ${isGeodesic} geojson: ${geoJsonString}`)

  let polyline: Polyline = Polyline.fromJSON(Terraformer.geojsonToArcGIS(JSON.parse(geoJsonString)));
  polyline.spatialReference = { wkid: wkid }

  if (isGeodesic) {
    if (!geodeticDensifyOperator.isLoaded()) {
      await geodeticDensifyOperator.load();
    }

    polyline = geodeticDensifyOperator.execute(polyline, 50, { curveType: "geodesic", unit: "meters" }) as Polyline;

  }
  const esriJsonString = JSON.stringify(polyline);
  console.log(`Output esrijson: ${esriJsonString}`)
  callback(null, {esriJsonString: esriJsonString});
}

const buildPolygon: ArcGisServiceHandlers["buildPolygon"] = (call, callback) => {
  const polylines: Polyline[] = [];

  call.request.esriJsonLineStrings.forEach(lineString => {
    polylines.push(Polyline.fromJSON(JSON.parse(lineString)));
  })

  const unionPolyLines = unionOperator.executeMany(polylines)

  const polygon = new Polygon({
    rings: unionPolyLines.toJSON()["paths"],
    spatialReference: {wkid: call.request.srs}
  });

  callback(null, {esriJsonString: JSON.stringify(polygon.toJSON())})
}

const splitPolygon: ArcGisServiceHandlers["splitPolygon"] = (call, callback) => {
  console.log("Split polygon");

  const target = Polygon.fromJSON(JSON.parse(call.request.target.esriJsonPolygon));
  const cutter = Polyline.fromJSON(JSON.parse(call.request.esriJsonCutter));

  const cutResults = cutOperator.execute(target, cutter) as Polygon[];
  const polygons = (cutResults || []).map((poly) => {
    return {
      esriJsonPolygon: JSON.stringify(poly.toJSON())
    };
  });

  console.log(`Created ${polygons.length} pieces`);
  callback(null, { polygons });
}

function main() {
  const server = new grpc.Server();

  server.addService(arcGisJsProto.ArcGisService.service, {
    convertGeoJsonLineToEsriJsonLine: convertGeoJsonLineToEsriJsonLine,
    buildPolygon: buildPolygon,
    splitPolygon: splitPolygon
  });

  const bindAddress = '0.0.0.0:8082';

  server.bindAsync(bindAddress, grpc.ServerCredentials.createInsecure(), (error, port) => {
    if (error) {
      console.error(error);
      return;
    }
    console.log(`gRPC Server running at ${bindAddress}`);
  });
}

main();