import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import path from 'path';
import Polyline from "@arcgis/core/geometry/Polyline.js";
import type {ProtoGrpcType} from "./generated/ArcGisJs.ts";
import type {ArcGisServiceHandlers} from "./generated/arcgisjs/ArcGisService.ts";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";
import * as cutOperator from '@arcgis/core/geometry/operators/cutOperator.js';
import {unionPolygons} from "./handlers/union-polygons";
import {calculatePolygonArea} from "./handlers/calculate-polygon-area";
import {densifyLoxodromePolyline} from "./handlers/densify-loxodrome-polyline";

import {findParentLine} from './handlers/lineTools.js';
import {explodePolygon} from './handlers/polygonTools.js';
import {checkParentContainsChild} from './handlers/check-parent-contains-child';
import {getStartAndEndPoints} from './handlers/get-start-and-end-points'
import {validatePolygonReconstruction} from './handlers/validate-polygon-reconstruction.js';
import esriConfig from "@arcgis/core/config.js";
import express from "express";
import {convertGeoJsonLineToEsriJsonLine} from "./handlers/convert-geo-json-line-to-esri-json.js";

//We need to host a version of the ESRI CDN so the library can run offline.
//https://developers.arcgis.com/javascript/latest/faq/#can-i-host-the-arcgis-cdn-modules-locally
//https://developers.arcgis.com/javascript/latest/working-with-assets/
const assetApp = express();
const ASSET_PORT = 3000;
const assetFolder = path.resolve(process.cwd(), "public/assets");
console.log(`[Asset Server] serving files in: ${assetFolder}`);
assetApp.use("/assets", express.static(assetFolder));
assetApp.listen(ASSET_PORT, () => {
  console.log(`[Asset Server] Running at http://localhost:${ASSET_PORT}/assets`);
});
esriConfig.assetsPath = `http://localhost:${ASSET_PORT}/assets`;

const PROTO_PATH = path.join("../src/main/proto", 'ArcGisJs.proto');
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true
});

const arcGisJsProto: ProtoGrpcType["arcgisjs"] = grpc.loadPackageDefinition(packageDefinition).arcgisjs as any;

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
    convertGeoJsonLineToEsriJsonLine,
    buildPolygon,
    splitPolygon,
    explodePolygon,
    findParentLine,
    densifyLoxodromePolyline,
    unionPolygons,
    calculatePolygonArea,
    checkParentContainsChild,
    getStartAndEndPoints,
    validatePolygonReconstruction
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