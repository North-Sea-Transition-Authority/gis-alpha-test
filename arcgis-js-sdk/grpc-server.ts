import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import path from 'path';
import Polyline from "@arcgis/core/geometry/Polyline.js";
import type {ProtoGrpcType} from "./generated/ArcGisJs.ts";
import type {ArcGisServiceHandlers} from "./generated/arcgisjs/ArcGisService.ts";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import * as cutOperator from '@arcgis/core/geometry/operators/cutOperator.js';
import {unionPolygons} from "./handlers/union-polygons";
import {calculatePolygonArea} from "./handlers/calculate-polygon-area";
import {densifyLoxodromePolyline} from "./handlers/densify-loxodrome-polyline";
import {findParentLine} from './handlers/lineTools.js';
import {explodePolygon} from './handlers/polygonTools.js';
import {checkParentContainsChild} from './handlers/check-parent-contains-child';
import {getStartAndEndPoints} from './handlers/get-start-and-end-points'
import {validatePolygonReconstruction} from './handlers/validate-polygon-reconstruction.js';
import {verifyChildGeodesicLinesOverlapParents} from './handlers/verify-child-geodesic-lines-overlap-parents';
import {mergePolygons} from './handlers/merge-polygons.js';
import {generalizePolygon} from './handlers/generalizePolygon.js';
import {mergeAndGeneralizeLines} from './handlers/merge-and-generalize-lines';
import esriConfig from "@arcgis/core/config.js";
import express from "express";
import {
  batchConvertGeoJsonLinesToEsriJsonLines,
  convertGeoJsonLineToEsriJsonLine
} from "./handlers/convert-geo-json-line-to-esri-json.js";
import {projectPolygons} from "./handlers/project-polygons";
import * as linesToPolygonsOperator from "@arcgis/core/geometry/operators/linesToPolygonsOperator.js";
import * as simplifyOperator from "@arcgis/core/geometry/operators/simplifyOperator.js";
import {verifyPolygonsAreTopologicallyEqual} from "./handlers/verify-polygons-are-topologically-equal.js";
import {convertEsriJsonPolygonToGeoJson} from "./handlers/convert-esri-json-polygon-to-geo-json.js";
import * as multiPartToSinglePartOperator from "@arcgis/core/geometry/operators/multiPartToSinglePartOperator.js";
import * as disjointOperator from "@arcgis/core/geometry/operators/disjointOperator.js";

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

  const polygons = linesToPolygonsOperator.executeMany(polylines);

  // We only want the first polygon, if there is more than one polygon, then they will be the holes of a polygon with holes
  // and will already be included in the first polygon.
  const polygon = polygons[0];
  polygon.spatialReference = {wkid: call.request.srs};

  console.log(`Built ${polygons.length} polygons`);

  const simplifiedPolygon = simplifyOperator.execute(polygon) as Polygon;


  callback(null, {esriJsonString: JSON.stringify(simplifiedPolygon.toJSON())})
}

const splitPolygon: ArcGisServiceHandlers["splitPolygon"] = (call, callback) => {
  console.log("Split polygon");

  const target = Polygon.fromJSON(JSON.parse(call.request.target.esriJsonPolygon));
  const cutter = Polyline.fromJSON(JSON.parse(call.request.esriJsonCutter));

  const cutResults = cutOperator.execute(target, cutter) as Polygon[];

  // cutresults may contain disjointed polygons, (one polygon that should actually be multiple polygons)
  // this operation splits those disjointed polygons into separate polygons
  const polygons: Polygon[] = multiPartToSinglePartOperator.executeMany(cutResults) as Polygon[];

  console.log(`Created ${polygons.length} pieces`);

  const response = (polygons || []).map((poly) => {
    return {
      esriJsonPolygon: JSON.stringify(poly.toJSON())
    };
  });

  callback(null, { polygons: response });
}

function main() {
  const server = new grpc.Server();

  server.addService(arcGisJsProto.ArcGisService.service, {
    convertGeoJsonLineToEsriJsonLine,
    batchConvertGeoJsonLinesToEsriJsonLines,
    buildPolygon,
    splitPolygon,
    explodePolygon,
    findParentLine,
    densifyLoxodromePolyline,
    unionPolygons,
    calculatePolygonArea,
    checkParentContainsChild,
    verifyChildGeodesicLinesOverlapParents,
    getStartAndEndPoints,
    validatePolygonReconstruction,
    mergePolygons,
    generalizePolygon,
    mergeAndGeneralizeLines,
    convertEsriJsonPolygonToGeoJson,
    verifyPolygonsAreTopologicallyEqual,
    projectPolygons
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