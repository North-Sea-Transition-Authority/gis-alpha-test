import {expect, test} from 'vitest'
import * as densifyOperator from "@arcgis/core/geometry/operators/densifyOperator";
import * as geodeticDensifyOperator from "@arcgis/core/geometry/operators/geodeticDensifyOperator";
import * as generalizeOperator from "@arcgis/core/geometry/operators/generalizeOperator";
import * as geodeticAreaOperator from "@arcgis/core/geometry/operators/geodeticAreaOperator";
import * as areaOperator from "@arcgis/core/geometry/operators/areaOperator";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator";
import * as intersectionOperator from '@arcgis/core/geometry/operators/intersectionOperator';
import * as cutOperator from '@arcgis/core/geometry/operators/cutOperator';
import * as equalsOperator from '@arcgis/core/geometry/operators/equalsOperator';
import Polyline from "@arcgis/core/geometry/Polyline";
import Polygon from "@arcgis/core/geometry/Polygon";
import fs from 'node:fs';
import * as Terraformer from "@terraformer/arcgis"
// Tests for findNorthwestmostLine handler
import {findNorthwestmostLine} from "./handlers/find-northwestmost-line";

const WKID_ED50 = 4230;
const WKID_BNG = 27700;

// When densifying with geodesic set to false, the maxSegmentLength unit is derived from the SR.
// For ED50, the unit is degrees (see https://epsg.io/4230).
const PLANAR_DENSIFY_MAX_SEGMENT_LENGTH_DEGREES = parseFloat((20.0 / 3600).toFixed(11));
const GEODESIC_DENSIFY_MAX_SEGMENT_LENGTH_METERS = 100;


test("Densify planar", () => {
  const inputGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/densify/input-line-string.geojson', 'utf8'));
  const inputGeom = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputGeoJson));
  inputGeom.spatialReference = { wkid: WKID_ED50 };
  const expectedGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/densify/output-line-string.geojson', 'utf8'));
  const expectedGeom = Polyline.fromJSON(Terraformer.geojsonToArcGIS(expectedGeoJson));
  expectedGeom.spatialReference = { wkid: WKID_ED50 };

  const densifiedGeom = densifyOperator.execute(inputGeom, PLANAR_DENSIFY_MAX_SEGMENT_LENGTH_DEGREES) as Polyline;

  densifiedGeom.paths[0].forEach((densifiedPoint, index) => {
    const expectedPoint = expectedGeom.paths[0][index];
    expect(densifiedPoint[0]).toBeCloseTo(expectedPoint[0], 11);
    expect(densifiedPoint[1]).toBeCloseTo(expectedPoint[1], 11);
  });
});

test("Generalise (aka simplify)", () => {
  // Flip input and output for simplification
  const inputGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/densify/output-line-string.geojson', 'utf8'));
  const inputGeom = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputGeoJson));
  inputGeom.spatialReference = { wkid: WKID_ED50 };
  const expectedGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/densify/input-line-string.geojson', 'utf8'));
  const expectedGeom = Polyline.fromJSON(Terraformer.geojsonToArcGIS(expectedGeoJson));
  expectedGeom.spatialReference = { wkid: WKID_ED50 };

  const simplifiedGeom = generalizeOperator.execute(inputGeom, 0.01) as Polyline;

  expect(simplifiedGeom.paths[0]).toEqual(expectedGeom.paths[0]); // No rounding necessary
});

test("Area calculation - ED50", async () => {
  const inputGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/area/input-ed50-polygon.geojson', 'utf8'));
  const inputGeom = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputGeoJson));
  inputGeom.spatialReference = { wkid: WKID_ED50 };

  if (!geodeticAreaOperator.isLoaded()) {
    await geodeticAreaOperator.load();
  }

  const area = geodeticAreaOperator.execute(inputGeom, { curveType: "geodesic", unit: "square-kilometers" });

  expect(area).toBeCloseTo(201.514163007667, 5);
});

test("Area calculation - BNG", () => {
  const inputGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/area/input-bng-polygon.geojson', 'utf8'));
  const inputGeom = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputGeoJson));
  inputGeom.spatialReference = { wkid: WKID_BNG };

  const area = areaOperator.execute(inputGeom, { unit: "square-kilometers" });

  expect(area).toBeCloseTo(21.1926213500511, 11);
});

test("Union polygons", () => {
  const inputPoly1GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/union/polygons/input-polygon-1.geojson', 'utf8'));
  const inputPoly1 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputPoly1GeoJson));
  inputPoly1.spatialReference = { wkid: WKID_ED50 };
  const inputPoly2GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/union/polygons/input-polygon-2.geojson', 'utf8'));
  const inputPoly2 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputPoly2GeoJson));
  inputPoly2.spatialReference = { wkid: WKID_ED50 };
  const expectedPolyGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/union/polygons/output-polygon.geojson', 'utf8'));
  const expectedPoly = Polygon.fromJSON(Terraformer.geojsonToArcGIS(expectedPolyGeoJson));
  expectedPoly.spatialReference = { wkid: WKID_ED50 };

  const unionedPoly = unionOperator.execute(inputPoly1, inputPoly2) as Polygon;

  // No rounding necessary, but ArcGIS has rotated the points, so rotate back to compare
  expect(rotatePoints(unionedPoly.rings[0], 26)).toEqual(expectedPoly.rings[0]);
});

test("Union line strings", () => {
  const inputLine1GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/union/line-strings/input-line-string-1.geojson', 'utf8'));
  const inputLine1 = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputLine1GeoJson));
  inputLine1.spatialReference = { wkid: WKID_ED50 };
  const inputLine2GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/union/line-strings/input-line-string-2.geojson', 'utf8'));
  const inputLine2 = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputLine2GeoJson));
  inputLine2.spatialReference = { wkid: WKID_ED50 };
  const expectedLineGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/union/line-strings/output-line-string.geojson', 'utf8'));
  const expectedLine = Polyline.fromJSON(Terraformer.geojsonToArcGIS(expectedLineGeoJson));
  expectedLine.spatialReference = { wkid: WKID_ED50 };

  const unionedLine = unionOperator.execute(inputLine1, inputLine2) as Polyline;

  expect(unionedLine.paths[0]).toEqual(expectedLine.paths[0]); // No rounding necessary
});

test("Union densified line strings", async () => {
  const inputLine1GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/union/line-strings/input-line-string-1.geojson', 'utf8'));
  const inputLine1 = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputLine1GeoJson));
  inputLine1.spatialReference = { wkid: WKID_ED50 };
  const inputLine2GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/union/line-strings/input-line-string-2.geojson', 'utf8'));
  const inputLine2 = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputLine2GeoJson));
  inputLine2.spatialReference = { wkid: WKID_ED50 };
  const expectedLineGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/union/line-strings/output-line-string-ed50.geojson', 'utf8'));
  const expectedLine = Polyline.fromJSON(Terraformer.geojsonToArcGIS(expectedLineGeoJson));
  expectedLine.spatialReference = { wkid: WKID_ED50 };

  if (!geodeticDensifyOperator.isLoaded()) {
    await geodeticDensifyOperator.load();
  }

  const densifiedInputLine1 = geodeticDensifyOperator.execute(inputLine1, GEODESIC_DENSIFY_MAX_SEGMENT_LENGTH_METERS, { curveType: "geodesic", unit: "meters" }) as Polyline;
  const densifiedInputLine2 = geodeticDensifyOperator.execute(inputLine2, GEODESIC_DENSIFY_MAX_SEGMENT_LENGTH_METERS, { curveType: "geodesic", unit: "meters" }) as Polyline;

  const unionedLine = unionOperator.execute(densifiedInputLine1, densifiedInputLine2) as Polyline;

  unionedLine.paths.forEach((path, index) => {
    const unionedLinePath = path;
    const expectedLinePath = expectedLine.paths[index];

    // Can only density geometries, so build new polyline from the single line path
    const expectedLinePathAsPolyline = new Polyline();
    expectedLinePathAsPolyline.addPath(expectedLinePath);
    expectedLinePathAsPolyline.spatialReference = { wkid: WKID_ED50 };

    const densifiedExpectedLinePath = (geodeticDensifyOperator.execute(expectedLinePathAsPolyline, GEODESIC_DENSIFY_MAX_SEGMENT_LENGTH_METERS, { curveType: "geodesic", unit: "meters" }) as Polyline).paths[0];

    if (index === 0) {
      unionedLinePath.reverse();
    }

    expect(unionedLinePath).toEqual(densifiedExpectedLinePath); // No rounding necessary
  });
});

test('Intersect polygons', async () => {
  const inputPolygon1GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/intersect/polygons/input-polygon-1.geojson', 'utf8'));
  const inputPolygon1 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputPolygon1GeoJson));
  inputPolygon1.spatialReference = { wkid: WKID_ED50 };

  const inputPolygon2GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/intersect/polygons/input-polygon-2.geojson', 'utf8'));
  const inputPolygon2 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputPolygon2GeoJson));
  inputPolygon2.spatialReference = { wkid: WKID_ED50 };

  const expectedOutputPolygonGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/intersect/polygons/output-polygon.geojson', 'utf8'));
  const expectedOutputPolygon = Polygon.fromJSON(Terraformer.geojsonToArcGIS(expectedOutputPolygonGeoJson));
  expectedOutputPolygon.spatialReference = { wkid: WKID_ED50 };

  const intersectionPolygon = intersectionOperator.execute(inputPolygon1, inputPolygon2) as Polygon;

  const simplifiedExpectedOutputPolygon = generalizeOperator.execute(expectedOutputPolygon, 0.01) as Polygon;

  expect(rotatePoints(roundPoints(intersectionPolygon.rings[0], 5), 2)).toEqual(roundPoints(simplifiedExpectedOutputPolygon.rings[0], 5));
});

test('Intersect line strings', async () => {
  const inputLineString1GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/intersect/line-strings/input-line-string-1.geojson', 'utf8'));
  const inputLine1 = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputLineString1GeoJson));
  inputLine1.spatialReference = { wkid: WKID_ED50 };

  const inputLineString2GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/intersect/line-strings/input-line-string-2.geojson', 'utf8'));
  const inputLine2 = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputLineString2GeoJson));
  inputLine2.spatialReference = { wkid: WKID_ED50 };

  const expectedOutputLineStringGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/intersect/line-strings/output-line-string.geojson', 'utf8'));
  const expectedOutputLine = Polyline.fromJSON(Terraformer.geojsonToArcGIS(expectedOutputLineStringGeoJson));
  expectedOutputLine.spatialReference = { wkid: WKID_ED50 };

  const intersectionLine = intersectionOperator.execute(inputLine1, inputLine2) as Polyline;

  expect(intersectionLine.paths[0]).toEqual(expectedOutputLine.paths[0]);
});

test('Intersect densified line strings', async () => {
  const inputLineString1GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/intersect/line-strings/input-line-string-1.geojson', 'utf8'));
  const inputLine1 = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputLineString1GeoJson));
  inputLine1.spatialReference = { wkid: WKID_ED50 };

  const inputLineString2GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/intersect/line-strings/input-line-string-2.geojson', 'utf8'));
  const inputLine2 = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputLineString2GeoJson));
  inputLine2.spatialReference = { wkid: WKID_ED50 };

  const expectedOutputLineStringGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/intersect/line-strings/output-line-string.geojson', 'utf8'));
  const expectedOutputLine = Polyline.fromJSON(Terraformer.geojsonToArcGIS(expectedOutputLineStringGeoJson));
  expectedOutputLine.spatialReference = { wkid: WKID_ED50 };

  if (!geodeticDensifyOperator.isLoaded()) {
    await geodeticDensifyOperator.load();
  }

  const densifiedInputLine1 = geodeticDensifyOperator.execute(inputLine1, GEODESIC_DENSIFY_MAX_SEGMENT_LENGTH_METERS, { curveType: "geodesic", unit: "meters" }) as Polyline;
  const densifiedInputLine2 = geodeticDensifyOperator.execute(inputLine2, GEODESIC_DENSIFY_MAX_SEGMENT_LENGTH_METERS, { curveType: "geodesic", unit: "meters" }) as Polyline;

  const intersectionLine = intersectionOperator.execute(densifiedInputLine1, densifiedInputLine2) as Polyline;

  expect(intersectionLine).toBeNull();
});

test('Cut simple polygon', async () => {
  const inputPolygonGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/simple/input-polygon.geojson', 'utf8'));
  const inputPolygon = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputPolygonGeoJson));
  inputPolygon.spatialReference = { wkid: WKID_ED50 };

  const inputCutterLineStringGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/simple/input-cutter-line-string.geojson', 'utf8'));
  const inputCutterLine = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputCutterLineStringGeoJson));
  inputCutterLine.spatialReference = { wkid: WKID_ED50 };
  const expectedOutputPolygon1GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/simple/output-polygon-1.geojson', 'utf8'));
  const expectedOutputPolygon1 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(expectedOutputPolygon1GeoJson));
  expectedOutputPolygon1.spatialReference = { wkid: WKID_ED50 };

  const expectedOutputPolygon2GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/simple/output-polygon-2.geojson', 'utf8'));
  const expectedOutputPolygon2 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(expectedOutputPolygon2GeoJson));
  expectedOutputPolygon2.spatialReference = { wkid: WKID_ED50 };

  const cutPolygons = cutOperator.execute(inputPolygon, inputCutterLine) as Polygon[];

  expect(cutPolygons.length).toEqual(2);

  const cutPolygon1 = cutPolygons[0];
  const cutPolygon2 = cutPolygons[1];

  // Note: There are additional points in the results, so use include
  expect(cutPolygon1.rings[0]).to.deep.include.members(expectedOutputPolygon1.rings[0]);
  expect(cutPolygon2.rings[0]).to.deep.include.members(expectedOutputPolygon2.rings[0]);
});

test('Cut simple polygon with geodesic line', async () => {
  const inputPolygonGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/simple-with-geodesic-line/input-polygon.geojson', 'utf8'));
  const inputPolygon = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputPolygonGeoJson));
  inputPolygon.spatialReference = { wkid: WKID_ED50 };

  const inputCutterLineStringGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/simple-with-geodesic-line/input-cutter-line-string.geojson', 'utf8'));
  const inputCutterLine = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputCutterLineStringGeoJson));
  inputCutterLine.spatialReference = { wkid: WKID_ED50 };

  const cutPolygons = cutOperator.execute(inputPolygon, inputCutterLine) as Polygon[];

  expect(cutPolygons.length).toEqual(2);

  const cutPolygon1 = cutPolygons[0];
  const cutPolygon2 = cutPolygons[1];

  expect(roundPoints(cutPolygon1.rings[0], 9)).to.deep.include([-4.5, 59.008684166]);
  expect(cutPolygon1.rings[0]).to.deep.include([-4.5, 58]);
  expect(roundPoints(cutPolygon2.rings[0], 9)).to.deep.include([-4.5, 59.008684166]);
  expect(cutPolygon2.rings[0]).to.deep.include([-4.5, 58]);
});

test('Cut complex polygon with coastline', async () => {
  const inputPolygonGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/coastline/input-polygon.geojson', 'utf8'));
  const inputPolygon = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputPolygonGeoJson));
  inputPolygon.spatialReference = { wkid: WKID_ED50 };
  const inputCutterLineStringGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/coastline/input-cutter-line-string.geojson', 'utf8'));
  const inputCutterLine = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputCutterLineStringGeoJson));
  inputCutterLine.spatialReference = { wkid: WKID_ED50 };

  const expectedOutputPolygon1GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/coastline/output-polygon-1.geojson', 'utf8'));
  const expectedOutputPolygon1 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(expectedOutputPolygon1GeoJson));
  expectedOutputPolygon1.spatialReference = { wkid: WKID_ED50 };

  const expectedOutputPolygon2GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/coastline/output-polygon-2.geojson', 'utf8'));
  const expectedOutputPolygon2 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(expectedOutputPolygon2GeoJson));
  expectedOutputPolygon2.spatialReference = { wkid: WKID_ED50 };

  const cutPolygons = cutOperator.execute(inputPolygon, inputCutterLine) as Polygon[];

  expect(cutPolygons.length).toEqual(2);

  const cutPolygon1 = cutPolygons[0];
  const cutPolygon2 = cutPolygons[1];

  // Note: There are additional points in the results, so use include
  expect(cutPolygon1.rings[0]).to.deep.include.members(expectedOutputPolygon2.rings[0]);
  expect(cutPolygon2.rings[0]).to.deep.include.members(expectedOutputPolygon1.rings[0]);
});

test('Cut polygon multiple times', async () => {
  const inputPolygonGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/multiple-cuts/input-polygon.geojson', 'utf8'));
  const inputPolygon = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputPolygonGeoJson));
  inputPolygon.spatialReference = { wkid: WKID_ED50 };

  const inputCutterMultiLineStringGeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/multiple-cuts/input-cutter-multi-line-string.geojson', 'utf8'));
  const inputCutterLine = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputCutterMultiLineStringGeoJson));
  inputCutterLine.spatialReference = { wkid: WKID_ED50 };

  const expectedOutputPolygon1GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/multiple-cuts/output-polygon-1.geojson', 'utf8'));
  const expectedOutputPolygon1 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(expectedOutputPolygon1GeoJson));
  expectedOutputPolygon1.spatialReference = { wkid: WKID_ED50 };

  const expectedOutputPolygon2GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/multiple-cuts/output-polygon-2.geojson', 'utf8'));
  const expectedOutputPolygon2 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(expectedOutputPolygon2GeoJson));
  expectedOutputPolygon2.spatialReference = { wkid: WKID_ED50 };

  const expectedOutputPolygon3GeoJson = JSON.parse(fs.readFileSync('../src/test/resources/oracle-test-cases/cut/multiple-cuts/output-polygon-3.geojson', 'utf8'));
  const expectedOutputPolygon3 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(expectedOutputPolygon3GeoJson));
  expectedOutputPolygon3.spatialReference = { wkid: WKID_ED50 };

  const cutPolygons = cutOperator.execute(inputPolygon, inputCutterLine) as Polygon[];

  expect(cutPolygons.length).toEqual(3);

  const cutPolygon1 = cutPolygons[0];
  const cutPolygon2 = cutPolygons[1];
  const cutPolygon3 = cutPolygons[2];

  // Note: There are additional points in the results, so use include
  expect(cutPolygon1.rings[0]).to.deep.include.members(expectedOutputPolygon3.rings[0]);
  expect(cutPolygon2.rings[0]).to.deep.include.members(expectedOutputPolygon1.rings[0]);
  expect(cutPolygon3.rings[0]).to.deep.include.members(expectedOutputPolygon2.rings[0]);
});

test('Compare polygon equality to same polygon cut and unioned', async () => {
  const inputPolygonGeoJson = JSON.parse('{"type":"Polygon","coordinates":[[[2,54],[2,53],[2.0014880472655476,53.00000624745712],[2.002976094959984,53.00001247629393],[2.0044641430820858,53.000018686510444],[2.005952191630573,53.00002487810668],[2.007440240604165,53.00003105108261],[2.008928290001582,53.00003720543823],[2.010416339821544,53.000043341173544],[2.0119043900627704,53.000049458288515],[2.0133924407239814,53.00005555678316],[2.014880491803897,53.00006163665746],[2.0163685433012364,53.0000676979114],[2.0178565952147203,53.00007374054497],[2.0193446475430674,53.00007976455817],[2.020832700284999,53.000085769950985],[2.022320753439233,53.000091756723414],[2.023808807004491,53.00009772487544],[2.0252968609794917,53.00010367440704],[2.0267849153629562,53.000109605318244],[2.028272970153603,53.000115517609],[2.029761025350152,53.00012141127932],[2.031249080951324,53.0001272863292],[2.0327371369558382,53.00013314275863],[2.0342251933624143,53.000138980567584],[2.0357132501697723,53.00014479975607],[2.0372013073766317,53.000150600324076],[2.0386893649817126,53.00015638227159],[2.0401774229837346,53.0001621455986],[2.041665481381418,53.0001678903051],[2.0431535401734817,53.00017361639109],[2.0446415993586466,53.00017932385654],[2.046129658935632,53.000185012701465],[2.047617718903157,53.00019068292585],[2.049105779259942,53.00019633452966],[2.050593840004707,53.000201967512936],[2.0520819011361713,53.00020758187563],[2.0535699626530555,53.000213177617745],[2.0550580245540777,53.00021875473928],[2.05654608683796,53.000224313240224],[2.05803414950342,53.000229853120544],[2.0595222125491786,53.000235374380274],[2.0610102759739557,53.00024087701936],[2.0624983397764702,53.00024636103783],[2.0639864039554427,53.000251826435665],[2.065474468509592,53.00025727321286],[2.0669625334376396,53.00026270136938],[2.0684505987383033,53.00026811090525],[2.0699386644103037,53.00027350182045],[2.0714267304523606,53.00027887411497],[2.072914796863194,53.00028422778881],[2.0744028636415233,53.000289562841935],[2.075890930786068,53.00029487927438],[2.077378998295548,53.00030017708609],[2.0788670661686837,53.00030545627711],[2.080355134404194,53.00031071684737],[2.0818432030007985,53.00031595879692],[2.0833312719572175,53.0003211821257],[2.0848193412721705,53.00032638683375],[2.086307410944378,53.00033157292104],[2.0877954809725585,53.00033674038755],[2.089283551355432,53.00034188923329],[2.090771622091719,53.00034701945825],[2.092259693180138,53.00035213106242],[2.0937477646194096,53.00035722404579],[2.0952358364082535,53.00036229840834],[2.0967239085453895,53.0003673541501],[2.0982119810295368,53.00037239127102],[2.099700053859415,53.00037740977111],[2.101188127033744,53.00038240965038],[2.1026762005512443,53.00038739090879],[2.104164274410634,53.000392353546346],[2.1056523486106347,53.00039729756305],[2.107140423149964,53.00040222295888],[2.1086284980273438,53.000407129733844],[2.1101165732414926,53.00041201788791],[2.1116046487911295,53.00041688742109],[2.1130927246749756,53.00042173833338],[2.1145808008917495,53.000426570624754],[2.116068877440171,53.00043138429522],[2.1175569543189603,53.000436179344774],[2.119045031526837,53.0004409557734],[2.1205331090625203,53.00044571358108],[2.12202118692473,53.00045045276783],[2.1235092651121867,53.00045517333363],[2.1249973436236087,53.000459875278466],[2.1264854224577165,53.00046455860233],[2.1279735016132295,53.000469223305245],[2.1294615810888673,53.00047386938718],[2.13094966088335,53.00047849684812],[2.1324377409953965,53.00048310568807],[2.133925821423727,53.00048769590701],[2.135413902167061,53.00049226750496],[2.1369019832241185,53.0004968204819],[2.1383900645936187,53.00050135483782],[2.139878146274281,53.00050587057271],[2.1413662282648263,53.00051036768655],[2.1428543105639726,53.00051484617938],[2.144342393170441,53.00051930605114],[2.14583047608295,53.000523747301855],[2.1473185593002198,53.00052816993151],[2.1488066428209702,53.0005325739401],[2.1502947266439207,53.00053695932761],[2.1517828107677905,53.00054132609403],[2.1532708951913,53.000545674239376],[2.154758979913168,53.00055000376362],[2.156247064932115,53.000554314666765],[2.1577351502468596,53.00055860694879],[2.1592232358561225,53.00056288060973],[2.1607113217586225,53.00056713564954],[2.16219940795308,53.000571372068215],[2.1636874944382143,53.00057558986577],[2.165175581212744,53.00057978904216],[2.1666636682753904,53.00058396959742],[2.1681517556248715,53.00058813153152],[2.1696398432599087,53.00059227484447],[2.1711279311792206,53.000596399536256],[2.1726160193815263,53.00060050560687],[2.1741041078655465,53.000604593056295],[2.17559219663,53.00060866188455],[2.1770802856736067,53.000612712091595],[2.178568374995086,53.00061674367746],[2.180056464593158,53.00062075664212],[2.181544554466542,53.00062475098556],[2.1830326446139576,53.000628726707795],[2.1845207350341247,53.00063268380881],[2.1860088257257617,53.000636622288596],[2.18749691668759,53.000640542147146],[2.188985007918328,53.00064444338446],[2.1904730994166957,53.00064832600053],[2.1919611911814125,53.00065218999534],[2.193449283211198,53.000656035368905],[2.1949373755047716,53.0006598621212],[2.1964254680608537,53.00066367025223],[2.197913560878163,53.00066745976199],[2.199401653955419,53.00067123065047],[2.2008897472913422,53.00067498291765],[2.2023778408846515,53.00067871656355],[2.2038659347340666,53.00068243158815],[2.2053540288383067,53.00068612799144],[2.2068421231960924,53.00068980577343],[2.2083302178061426,53.00069346493411],[2.209818312667177,53.000697105473456],[2.2113064077779145,53.00070072739149],[2.2127945031370757,53.00070433068819],[2.2142825987433796,53.00070791536355],[2.2157706945955455,53.00071148141757],[2.217258790692294,53.000715028850244],[2.2187468870323435,53.000718557661564],[2.2202349836144144,53.00072206785153],[2.2217230804372257,53.00072555942012],[2.2232111774994974,53.00072903236735],[2.224699274799949,53.0007324866932],[2.226187372337299,53.00073592239769],[2.2276754701102686,53.00073933948077],[2.2291635681175763,53.000742737942474],[2.2306516663579425,53.00074611778278],[2.232139764830086,53.00074947900168],[2.233627863532726,53.00075282159919],[2.235115962464583,53.00075614557528],[2.236604061624376,53.000759450929955],[2.238092161010825,53.000762737663216],[2.239580260622649,53.000766005775034],[2.2410683604585677,53.00076925526544],[2.2425564605173003,53.0007724861344],[2.244044560797567,53.00077569838192],[2.2455326612980873,53.000778892008],[2.24702076201758,53.00078206701263],[2.248508862954766,53.0007852233958],[2.2499969641083633,53.00078836115751],[2.251485065477092,53.00079148029776],[2.252973167059672,53.00079458081654],[2.254461268854823,53.00079766271384],[2.2559493708612632,53.00080072598966],[2.257437473077713,53.00080377064401],[2.2589255755028925,53.00080679667687],[2.26041367813552,53.00080980408822],[2.2619017809743163,53.00081279287809],[2.2633898840180002,53.00081576304645],[2.264877987265291,53.0008187145933],[2.2663660907149086,53.00082164751864],[2.2678541943655723,53.00082456182247],[2.269342298216002,53.000827457504776],[2.270830402264917,53.00083033456556],[2.2723185065110365,53.00083319300482],[2.2738066109530806,53.00083603282255],[2.2752947155897685,53.000838854018724],[2.2767828204198195,53.000841656593366],[2.2782709254419533,53.000844440546466],[2.2797590306548896,53.00084720587802],[2.2812471360573476,53.00084995258801],[2.282735241648047,53.000852680676445],[2.2842233474257068,53.00085539014331],[2.2857114533890472,53.00085808098862],[2.2871995595367878,53.00086075321236],[2.2886876658676476,53.00086340681451],[2.290175772380346,53.0008660417951],[2.291663879073603,53.00086865815408],[2.293151985946137,53.00087125589149],[2.2946400929966693,53.000873835007305],[2.296128200223918,53.00087639550153],[2.297616307626603,53.000878937374154],[2.299104415203444,53.00088146062518],[2.3005925229531603,53.000883965254594],[2.302080630874471,53.00088645126239],[2.3035687389660966,53.00088891864859],[2.305056847226755,53.00089136741316],[2.306544955655167,53.00089379755611],[2.3080330642500524,53.00089620907742],[2.3095211730101295,53.00089860197712],[2.311009281934118,53.00090097625518],[2.312497391020738,53.00090333191161],[2.313985500268709,53.000905668946395],[2.3154736096767494,53.00090798735953],[2.31696171924358,53.00091028715103],[2.3184498289679194,53.000912568320864],[2.3199379388484873,53.00091483086906],[2.3214260488840037,53.000917074795574],[2.322914159073187,53.00091930010046],[2.3244022694147577,53.00092150678367],[2.3258903799074346,53.0009236948452],[2.327378490549938,53.000925864285065],[2.328866601340986,53.000928015103256],[2.3303547122792994,53.000930147299776],[2.3318428233635973,53.000932260874606],[2.3333309345925985,53.00093435582776],[2.3348190459650233,53.000936432159214],[2.336307157479591,53.00093848986899],[2.337795269135021,53.00094052895707],[2.3392833809300324,53.000942549423435],[2.340771492863345,53.00094455126812],[2.342259604933678,53.0009465344911],[2.3437477171397516,53.000948499092374],[2.3452358294802846,53.00095044507193],[2.3467239419539965,53.00095237242978],[2.348212054559607,53.000954281165924],[2.3497001672958353,53.000956171280336],[2.351188280161401,53.00095804277303],[2.3526763931550234,53.00095989564401],[2.354164506275422,53.00096172989325],[2.355652619521317,53.00096354552076],[2.3571407328914264,53.00096534252655],[2.358628846384471,53.0009671209106],[2.3601169599991696,53.00096888067291],[2.3616050737342418,53.00097062181348],[2.363093187588407,53.00097234433232],[2.364581301560385,53.0009740482294],[2.3660694156488944,53.000975733504745],[2.367557529852655,53.00097740015833],[2.369045644170387,53.00097904819016],[2.3705337586008097,53.00098067760023],[2.3720218731426415,53.000982288388556],[2.3735099877946024,53.00098388055511],[2.3749981025554123,53.000985454099904],[2.37648621742379,53.00098700902294],[2.377974332398455,53.000988545324205],[2.379462447478127,53.00099006300369],[2.3809505626615257,53.000991562061415],[2.3824386779473707,53.000993042497356],[2.3839267933343806,53.000994504311514],[2.3854149088212755,53.00099594750392],[2.386903024406774,53.00099737207452],[2.3883911400895963,53.000998778023344],[2.3898792558684616,53.001000165350376],[2.3913673717420894,53.00100153405563],[2.392855487709199,53.00100288413908],[2.394343603768511,53.00100421560076],[2.395831719918743,53.00100552844063],[2.3973198361586148,53.00100682265871],[2.3988079524868473,53.00100809825498],[2.400296068902158,53.00100935522946],[2.401784185403268,53.00101059358214],[2.403272301988896,53.00101181331301],[2.4047604186577614,53.00101301442208],[2.406248535408583,53.001014196909345],[2.407736652240082,53.001015360774794],[2.4092247691509754,53.00101650601844],[2.410712886139985,53.001017632640256],[2.412201003205829,53.00101874064027],[2.413689120347227,53.001019830018464],[2.415177237562898,53.001020900774854],[2.4166653548515624,53.0010219529094],[2.4181534722119395,53.00102298642214],[2.419641589642748,53.00102400131306],[2.4211297071427076,53.00102499758214],[2.422617824710538,53.00102597522941],[2.424105942344958,53.00102693425485],[2.4255940600446877,53.001027874658455],[2.4270821778084466,53.00102879644023],[2.4285702956349535,53.00102969960019],[2.4300584135229286,53.0010305841383],[2.4315465314710907,53.001031450054576],[2.433034649478159,53.00103229734902],[2.434522767542854,53.00103312602163],[2.436010885663894,53.001033936072396],[2.4374990038399993,53.00103472750133],[2.438987122069889,53.00103550030841],[2.440475240352282,53.00103625449366],[2.441963358685898,53.00103699005707],[2.443451477069457,53.00103770699862],[2.444939595501678,53.00103840531833],[2.446427713981281,53.00103908501621],[2.447915832506984,53.00103974609223],[2.4494039510775076,53.0010403885464],[2.4508920696915713,53.00104101237873],[2.4523801883478935,53.0010416175892],[2.453868307045195,53.00104220417782],[2.455356425782194,53.00104277214459],[2.45684454455761,53.001043321489504],[2.4583326633701637,53.00104385221257],[2.459820782218573,53.00104436431378],[2.461308901101558,53.00104485779314],[2.4627970200178386,53.00104533265063],[2.4642851389661335,53.00104578888627],[2.465773257945162,53.00104622650006],[2.4672613769536444,53.001046645491975],[2.468749495990299,53.00104704586205],[2.470237615053846,53.00104742761026],[2.4717257341430052,53.0010477907366],[2.473213853256495,53.001048135241085],[2.474701972393035,53.0010484611237],[2.476190091551345,53.00104876838447],[2.4776782107301445,53.00104905702337],[2.479166329928153,53.00104932704041],[2.480654449144089,53.001049578435584],[2.4821425683766725,53.00104981120891],[2.4836306876246237,53.00105002536035],[2.485118806886661,53.00105022088992],[2.486606926161504,53.00105039779765],[2.4880950454478725,53.0010505560835],[2.489583164744485,53.0010506957475],[2.491071284050062,53.00105081678963],[2.492559403363323,53.00105091920988],[2.494047522682986,53.001051003008286],[2.4955356420077712,53.001051068184815],[2.4970237613363993,53.001051114739475],[2.4985118806675874,53.001051142672274],[2.5000000000000573,53.0010511519832],[2.501488119332526,53.001051142672274],[2.502976238663715,53.001051114739475],[2.504464357992342,53.00105106818482],[2.505952477317128,53.001051003008286],[2.507440596636791,53.00105091920989],[2.5089287159500517,53.00105081678962],[2.5104168352556284,53.0010506957475],[2.5119049545522416,53.0010505560835],[2.51339307383861,53.00105039779765],[2.514881193113453,53.00105022088992],[2.51636931237549,53.00105002536035],[2.517857431623441,53.0010498112089],[2.5193455508560247,53.001049578435584],[2.520833670071961,53.00104932704041],[2.5223217892699688,53.00104905702337],[2.5238099084487686,53.00104876838447],[2.525298027607079,53.0010484611237],[2.526786146743619,53.001048135241085],[2.528274265857109,53.00104779073659],[2.5297623849462676,53.00104742761026],[2.5312505040098148,53.00104704586205],[2.5327386230464697,53.00104664549198],[2.5342267420549516,53.00104622650006],[2.5357148610339806,53.00104578888627],[2.537202979982275,53.00104533265063],[2.5386910988985556,53.00104485779314],[2.540179217781541,53.00104436431378],[2.5416673366299505,53.00104385221257],[2.5431554554425038,53.001043321489504],[2.5446435742179205,53.00104277214459],[2.546131692954919,53.00104220417782],[2.54761981165222,53.0010416175892],[2.549107930308543,53.00104101237873],[2.5505960489226065,53.0010403885464],[2.55208416749313,53.00103974609223],[2.5535722860188335,53.00103908501621],[2.5550604044984357,53.00103840531833],[2.5565485229306564,53.00103770699862],[2.5580366413142155,53.00103699005707],[2.5595247596478323,53.00103625449366],[2.5610128779302253,53.00103550030841],[2.562500996160115,53.001034727501334],[2.5639891143362195,53.001033936072396],[2.56547723245726,53.00103312602163],[2.5669653505219543,53.00103229734902],[2.5684534685290235,53.001031450054576],[2.5699415864771855,53.0010305841383],[2.57142970436516,53.00102969960019],[2.572917822191667,53.00102879644023],[2.574405939955426,53.001027874658455],[2.575894057655156,53.00102693425485],[2.577382175289576,53.00102597522942],[2.5788702928574065,53.00102499758214],[2.5803584103573662,53.00102400131306],[2.5818465277881746,53.00102298642214],[2.5833346451485513,53.0010219529094],[2.5848227624372155,53.001020900774854],[2.586310879652887,53.001019830018464],[2.5877989967942847,53.001018740640276],[2.589287113860129,53.001017632640256],[2.590775230849138,53.00101650601844],[2.5922633477600323,53.001015360774794],[2.5937514645915303,53.001014196909345],[2.5952395813423528,53.00101301442208],[2.5967276980112177,53.001011813313006],[2.5982158145968457,53.001010593582144],[2.5997039310979555,53.00100935522946],[2.601192047513267,53.001008098254985],[2.602680163841499,53.00100682265871],[2.604168280081371,53.00100552844063],[2.605656396231603,53.00100421560076],[2.6071445122909145,53.001002884139076],[2.6086326282580243,53.00100153405563],[2.610120744131652,53.001000165350376],[2.611608859910518,53.00099877802334],[2.61309697559334,53.00099737207452],[2.614585091178839,53.00099594750392],[2.6160732066657335,53.000994504311514],[2.6175613220527434,53.000993042497356],[2.619049437338588,53.000991562061415],[2.620537552521986,53.00099006300369],[2.6220256676016587,53.000988545324205],[2.623513782576324,53.00098700902294],[2.6250018974447014,53.000985454099904],[2.6264900122055113,53.00098388055511],[2.6279781268574722,53.000982288388556],[2.6294662413993044,53.00098067760023],[2.630954355829726,53.00097904819015],[2.632442470147458,53.00097740015833],[2.6339305843512193,53.000975733504745],[2.6354186984397288,53.000974048229395],[2.6369068124117065,53.00097234433232],[2.638394926265872,53.00097062181349],[2.639883040000944,53.00096888067291],[2.6413711536156432,53.0009671209106],[2.6428592671086872,53.00096534252655],[2.644347380478797,53.00096354552076],[2.6458354937246917,53.00096172989325],[2.6473236068450903,53.00095989564401],[2.6488117198387133,53.00095804277303],[2.650299832704279,53.000956171280336],[2.651787945440507,53.000954281165924],[2.6532760580461177,53.00095237242978],[2.6547641705198295,53.00095044507193],[2.656252282860362,53.000948499092374],[2.6577403950664356,53.000946534491106],[2.659228507136769,53.00094455126812],[2.6607166190700813,53.000942549423435],[2.662204730865093,53.00094052895706],[2.663692842520523,53.000938489868986],[2.6651809540350904,53.000936432159214],[2.6666690654075156,53.00093435582776],[2.6681571766365164,53.000932260874606],[2.6696452877208143,53.000930147299776],[2.6711333986591272,53.000928015103256],[2.672621509450176,53.000925864285065],[2.674109620092679,53.0009236948452],[2.6755977305853555,53.00092150678367],[2.6770858409269267,53.00091930010046],[2.6785739511161104,53.00091707479558],[2.680062061151627,53.00091483086906],[2.6815501710321943,53.000912568320864],[2.6830382807565343,53.00091028715102],[2.6845263903233643,53.00090798735953],[2.6860144997314053,53.000905668946395],[2.6875026089793757,53.0009033319116],[2.688990718065996,53.00090097625518],[2.6904788269899846,53.00089860197713],[2.691966935750062,53.000896209077425],[2.6934550443449465,53.00089379755611],[2.6949431527733587,53.00089136741316],[2.6964312610340175,53.00088891864859],[2.6979193691256427,53.000886451262396],[2.699407477046954,53.000883965254594],[2.70089558479667,53.00088146062517],[2.7023836923735103,53.000878937374154],[2.7038717997761954,53.00087639550152],[2.7053599070034444,53.00087383500731],[2.7068480140539766,53.00087125589149],[2.7083361209265115,53.00086865815408],[2.709824227619768,53.0008660417951],[2.711312334132466,53.00086340681452],[2.7128004404633264,53.00086075321235],[2.7142885466110664,53.00085808098862],[2.715776652574407,53.000855390143315],[2.717264758352067,53.000852680676445],[2.7187528639427665,53.00084995258801],[2.7202409693452245,53.00084720587802],[2.72172907455816,53.000844440546466],[2.7232171795802946,53.000841656593366],[2.724705284410345,53.000838854018724],[2.726193389047033,53.00083603282255],[2.7276814934890767,53.00083319300482],[2.729169597735197,53.00083033456556],[2.730657701784111,53.000827457504776],[2.7321458056345413,53.00082456182247],[2.733633909285205,53.00082164751864],[2.7351220127348226,53.0008187145933],[2.736610115982114,53.00081576304645],[2.7380982190257974,53.00081279287808],[2.7395863218645933,53.00080980408822],[2.7410744244972216,53.00080679667686],[2.7425625269224008,53.000803770644005],[2.744050629138851,53.00080072598966],[2.7455387311452912,53.00079766271384],[2.747026832940442,53.00079458081654],[2.7485149345230218,53.00079148029776],[2.7500030358917504,53.000788361157504],[2.751491137045348,53.0007852233958],[2.752979237982533,53.00078206701263],[2.7544673387020264,53.000778892008],[2.7559554392025465,53.00077569838192],[2.757443539482814,53.000772486134395],[2.7589316395415464,53.00076925526544],[2.7604197393774648,53.000766005775034],[2.761907838989289,53.000762737663216],[2.7633959383757376,53.000759450929955],[2.7648840375355306,53.000756145575274],[2.7663721364673877,53.00075282159919],[2.767860235170028,53.00074947900168],[2.769348333642171,53.00074611778278],[2.7708364318825365,53.000742737942474],[2.772324529889845,53.00073933948077],[2.7738126276628146,53.00073592239769],[2.7753007252001654,53.0007324866932],[2.7767888225006163,53.00072903236735],[2.778276919562888,53.00072555942012],[2.7797650163856993,53.00072206785153],[2.7812531129677702,53.000718557661564],[2.7827412093078197,53.00071502885024],[2.784229305404568,53.00071148141757],[2.7857174012567345,53.00070791536355],[2.7872054968630384,53.00070433068819],[2.788693592222199,53.0007007273915],[2.7901816873329377,53.000697105473456],[2.7916697821939715,53.00069346493411],[2.7931578768040213,53.00068980577343],[2.7946459711618066,53.00068612799145],[2.7961340652660476,53.00068243158815],[2.797622159115462,53.00067871656355],[2.7991102527087723,53.00067498291765],[2.800598346044695,53.00067123065047],[2.8020864391219513,53.00066745976199],[2.80357453193926,53.00066367025223],[2.805062624495342,53.0006598621212],[2.806550716788916,53.000656035368905],[2.8080388088187016,53.00065218999534],[2.809526900583418,53.00064832600053],[2.8110149920817857,53.00064444338446],[2.8125030833125235,53.000640542147146],[2.8139911742743515,53.0006366222886],[2.815479264965989,53.00063268380881],[2.816967355386156,53.000628726707795],[2.8184554455335715,53.00062475098556],[2.8199435354069555,53.00062075664212],[2.8214316250050278,53.00061674367746],[2.8229197143265075,53.000612712091595],[2.8244078033701143,53.00060866188455],[2.8258958921345676,53.000604593056295],[2.827383980618588,53.00060050560686],[2.828872068820894,53.000596399536256],[2.830360156740205,53.00059227484447],[2.8318482443752417,53.00058813153152],[2.8333363317247238,53.00058396959742],[2.83482441878737,53.00057978904216],[2.8363125055619,53.00057558986576],[2.8378005920470337,53.000571372068215],[2.8392886782414912,53.00056713564954],[2.8407767641439916,53.00056288060973],[2.8422648497532537,53.00055860694879],[2.843752935067999,53.000554314666765],[2.8452410200869456,53.00055000376362],[2.8467291048088144,53.00054567423937],[2.8482171892323236,53.00054132609403],[2.849705273356194,53.00053695932761],[2.851193357179144,53.0005325739401],[2.852681440699894,53.000528169931506],[2.854169523917164,53.000523747301855],[2.8556576068296735,53.00051930605114],[2.857145689436141,53.000514846179385],[2.858633771735288,53.00051036768655],[2.8601218537258326,53.00050587057271],[2.861609935406495,53.00050135483782],[2.8630980167759956,53.0004968204819],[2.864586097833053,53.00049226750496],[2.866074178576387,53.00048769590701],[2.8675622590047176,53.00048310568807],[2.8690503391167645,53.00047849684812],[2.870538418911246,53.00047386938717],[2.872026498386884,53.00046922330524],[2.873514577542397,53.00046455860234],[2.875002656376505,53.000459875278466],[2.8764907348879274,53.00045517333362],[2.877978813075383,53.00045045276783],[2.8794668909375933,53.00044571358108],[2.8809549684732767,53.0004409557734],[2.8824430456811534,53.00043617934478],[2.883931122559942,53.00043138429522],[2.885419199108364,53.000426570624754],[2.8869072753251386,53.00042173833338],[2.8883953512089837,53.00041688742109],[2.889883426758622,53.00041201788791],[2.89137150197277,53.00040712973384],[2.8928595768501495,53.00040222295888],[2.894347651389479,53.00039729756305],[2.89583572558948,53.000392353546346],[2.89732379944887,53.00038739090879],[2.89881187296637,53.00038240965038],[2.900299946140699,53.00037740977111],[2.901788018970578,53.000372391271014],[2.9032760914547247,53.00036735415009],[2.9047641635918606,53.00036229840834],[2.906252235380704,53.000357224045786],[2.907740306819976,53.00035213106242],[2.9092283779083954,53.00034701945825],[2.9107164486446817,53.0003418892333],[2.9122045190275556,53.00033674038755],[2.9136925890557364,53.000331572921034],[2.915180658727943,53.000326386833756],[2.9166687280428962,53.00032118212571],[2.9181567969993156,53.00031595879691],[2.9196448655959206,53.00031071684737],[2.9211329338314305,53.0003054562771],[2.9226210017045653,53.00030017708609],[2.9241090692140457,53.00029487927438],[2.925597136358591,53.000289562841935],[2.9270852031369197,53.0002842277888],[2.9285732695477527,53.00027887411496],[2.93006133558981,53.00027350182045],[2.9315494012618104,53.00026811090525],[2.9330374665624745,53.00026270136938],[2.9345255314905216,53.00025727321285],[2.9360135960446714,53.000251826435665],[2.937501660223644,53.00024636103784],[2.938989724026159,53.000240877019365],[2.940477787450935,53.000235374380274],[2.941965850496694,53.000229853120544],[2.943453913162154,53.00022431324022],[2.944941975446036,53.00021875473927],[2.9464300373470587,53.000213177617745],[2.947918098863943,53.00020758187562],[2.949406159995407,53.00020196751293],[2.950894220740172,53.00019633452967],[2.952382281096957,53.00019068292584],[2.9538703410644827,53.00018501270146],[2.955358400641467,53.00017932385654],[2.956846459826632,53.00017361639109],[2.958334518618696,53.0001678903051],[2.959822577016379,53.000162145598594],[2.961310635018401,53.000156382271584],[2.962798692623482,53.000150600324076],[2.9642867498303413,53.00014479975607],[2.9657748066377,53.00013898056759],[2.9672628630442754,53.00013314275863],[2.9687509190487895,53.00012728632921],[2.9702389746499613,53.00012141127932],[2.971727029846511,53.000115517609],[2.973215084637158,53.00010960531824],[2.9747031390206216,53.000103674407036],[2.976191192995623,53.00009772487543],[2.977679246560881,53.000091756723414],[2.979167299715115,53.000085769950985],[2.9806553524570467,53.000079764558166],[2.982143404785394,53.00007374054497],[2.9836314566988773,53.0000676979114],[2.9851195081962167,53.000061636657456],[2.9866075592761323,53.00005555678316],[2.9880956099373437,53.000049458288515],[2.9895836601785697,53.00004334117355],[2.9910717099985322,53.00003720543823],[2.9925597593959488,53.00003105108261],[2.9940478083695417,53.00002487810667],[2.995535856918028,53.000018686510444],[2.99702390504013,53.00001247629393],[2.998511952734566,53.00000624745712],[3,53],[3,54],[2,54]]]}');
  const inputPolygon = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputPolygonGeoJson));
  inputPolygon.spatialReference = { wkid: WKID_ED50 };

  // midway between these two dense points: [2.2499969641083633,53.00078836115751],[2.251485065477092,53.00079148029776]
  const inputCutterLineGeoJson = JSON.parse('{"type": "LineString", "coordinates": [[2.2507410147927276, 52.9], [2.2507410147927276, 54.1]]}');
  const inputCutterLine = Polyline.fromJSON(Terraformer.geojsonToArcGIS(inputCutterLineGeoJson));
  inputCutterLine.spatialReference = { wkid: WKID_ED50 };

  const cutPolygons = cutOperator.execute(inputPolygon, inputCutterLine) as Polygon[];

  expect(cutPolygons.length).toBe(2);

  const unionedCutPolygons = unionOperator.execute(cutPolygons[0], cutPolygons[1]) as Polygon;

  const inputPolygonEqualsUnionedCutPolygons = equalsOperator.execute(inputPolygon, unionedCutPolygons) as boolean;

  expect(inputPolygonEqualsUnionedCutPolygons).toBe(true);
});

test('Compare polygon equality to same polygon with line shifted at high decimal precision', async () => {
  const inputPolygon1GeoJson = JSON.parse('{"type":"Polygon","coordinates":[[[2,54],[2,53],[3,53],[3,54],[2,54]]]}');
  const inputPolygon1 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputPolygon1GeoJson));
  inputPolygon1.spatialReference = { wkid: WKID_ED50 };

  const inputPolygon2GeoJson = JSON.parse('{"type":"Polygon","coordinates":[[[2,54],[2,53],[3,53],[3.000000001,54],[2,54]]]}');
  const inputPolygon2 = Polygon.fromJSON(Terraformer.geojsonToArcGIS(inputPolygon2GeoJson));
  inputPolygon2.spatialReference = { wkid: WKID_ED50 };

  const inputPolygon1EqualsInputPolygon2 = equalsOperator.execute(inputPolygon1, inputPolygon2) as boolean; // trufflehog:ignore

  expect(inputPolygon1EqualsInputPolygon2).toBe(true);
});

function rotatePoints(points: any[][], distance: number): any[][] {
  const newPoints = points.slice();
  newPoints.pop(); // Remove duplicate first point
  const rotated = newPoints.slice(-distance).concat(newPoints.slice(0, -distance)); // Rotate points by distance
  rotated.push(rotated[0]); // Put first point back
  return rotated;
}

function roundPoints(points: any[][], decimalPlaces: number): any[][] {
  const multiplier = Math.pow(10, decimalPlaces);
  return points.map(point => [
    Math.round(point[0] * multiplier) / multiplier,
    Math.round(point[1] * multiplier) / multiplier
  ]);
}

test('Find northwestmost line - simple square', async () => {
  // Create a simple square where line at (0, 10) is NW-most
  // Square corners (clockwise from SW): (0,0) -> (10,0) -> (10,10) -> (0,10) -> (0,0)
  const lines = [
    {
      id: 'line1',
      polyLineEsriJson: JSON.stringify({
        paths: [[[0, 0], [10, 0]]],
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'line2',
      polyLineEsriJson: JSON.stringify({
        paths: [[[10, 0], [10, 10]]],
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'line3',
      polyLineEsriJson: JSON.stringify({
        paths: [[[10, 10], [0, 10]]],
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'line4',
      polyLineEsriJson: JSON.stringify({
        paths: [[[0, 10], [0, 0]]],
        spatialReference: { wkid: WKID_BNG }
      })
    }
  ];

  const result = await new Promise((resolve, reject) => {
    findNorthwestmostLine(
      { request: { lines } } as any,
      (error, response) => {
        if (error) reject(error);
        else resolve(response);
      }
    );
  }) as any;

  // Line 4 starts at (0, 10) which is the NW-most point
  expect(result.lineId).toBe('line4');
});

test('Find northwestmost line - rectangle with clear NW corner', async () => {
  // Rectangle where point at (100, 500) is clearly NW-most
  const lines = [
    {
      id: 'south',
      polyLineEsriJson: JSON.stringify({
        paths: [[[100, 100], [400, 100]]],
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'east',
      polyLineEsriJson: JSON.stringify({
        paths: [[[400, 100], [400, 500]]],
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'north',
      polyLineEsriJson: JSON.stringify({
        paths: [[[400, 500], [100, 500]]],
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'west',
      polyLineEsriJson: JSON.stringify({
        paths: [[[100, 500], [100, 100]]],
        spatialReference: { wkid: WKID_BNG }
      })
    }
  ];

  const result = await new Promise((resolve, reject) => {
    findNorthwestmostLine(
      { request: { lines } } as any,
      (error, response) => {
        if (error) reject(error);
        else resolve(response);
      }
    );
  }) as any;

  // 'west' line starts at (100, 500) which is the NW-most point
  expect(result.lineId).toBe('west');
});

test('Find northwestmost line - same latitude, westernmost wins', async () => {
  // Multiple points at same latitude, westernmost should win
  const lines = [
    {
      id: 'line1',
      polyLineEsriJson: JSON.stringify({
        paths: [[[5, 50], [10, 45]]],
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'line2',
      polyLineEsriJson: JSON.stringify({
        paths: [[[10, 45], [15, 50]]],
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'line3',
      polyLineEsriJson: JSON.stringify({
        paths: [[[15, 50], [0, 50]]],
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'line4',
      polyLineEsriJson: JSON.stringify({
        paths: [[[0, 50], [5, 50]]],
        spatialReference: { wkid: WKID_BNG }
      })
    }
  ];

  const result = await new Promise((resolve, reject) => {
    findNorthwestmostLine(
      { request: { lines } } as any,
      (error, response) => {
        if (error) reject(error);
        else resolve(response);
      }
    );
  }) as any;

  // line4 starts at (0, 50) which is westernmost at the highest latitude
  expect(result.lineId).toBe('line4');
});

test('Find northwestmost line - ED50 coordinates', async () => {
  // Test with ED50 (geographic) coordinates
  // Points around UK: SW corner, SE corner, NE corner, NW corner
  const lines = [
    {
      id: 'south',
      polyLineEsriJson: JSON.stringify({
        paths: [[[-6, 50], [2, 50]]],
        spatialReference: { wkid: WKID_ED50 }
      })
    },
    {
      id: 'east',
      polyLineEsriJson: JSON.stringify({
        paths: [[[2, 50], [2, 58]]],
        spatialReference: { wkid: WKID_ED50 }
      })
    },
    {
      id: 'north',
      polyLineEsriJson: JSON.stringify({
        paths: [[[2, 58], [-6, 58]]],
        spatialReference: { wkid: WKID_ED50 }
      })
    },
    {
      id: 'west',
      polyLineEsriJson: JSON.stringify({
        paths: [[[-6, 58], [-6, 50]]],
        spatialReference: { wkid: WKID_ED50 }
      })
    }
  ];

  const result = await new Promise((resolve, reject) => {
    findNorthwestmostLine(
      { request: { lines } } as any,
      (error, response) => {
        if (error) reject(error);
        else resolve(response);
      }
    );
  }) as any;

  // 'west' line starts at (-6, 58) which is the NW-most point
  expect(result.lineId).toBe('west');
});

test('Find northwestmost line - NW reference not at any actual point', async () => {
  // Create a trapezoid where (minX, maxY) is NOT at any actual start point
  // This tests the true "closest to NW" behavior
  //
  //     (5, 100) ----------- (15, 100)  <- maxY is 100
  //       /                       \
  //   (0, 50) ------------------- (20, 50)  <- minX is 0
  //
  // NW reference would be at (0, 100) which doesn't exist
  // Line starting at (5, 100) should be closest to this theoretical NW point

  const lines = [
    {
      id: 'top',
      polyLineEsriJson: JSON.stringify({
        paths: [[[5, 100], [15, 100]]],  // Starts at (5, 100) - should be NW-most
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'right',
      polyLineEsriJson: JSON.stringify({
        paths: [[[15, 100], [20, 50]]],
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'bottom',
      polyLineEsriJson: JSON.stringify({
        paths: [[[20, 50], [0, 50]]],
        spatialReference: { wkid: WKID_BNG }
      })
    },
    {
      id: 'left',
      polyLineEsriJson: JSON.stringify({
        paths: [[[0, 50], [5, 100]]],
        spatialReference: { wkid: WKID_BNG }
      })
    }
  ];

  const result = await new Promise((resolve, reject) => {
    findNorthwestmostLine(
      { request: { lines } } as any,
      (error, response) => {
        if (error) reject(error);
        else resolve(response);
      }
    );
  }) as any;

  // 'top' line starts at (5, 100) which is closest to theoretical NW point (0, 100)
  // Distance from (5, 100) to (0, 100) = 5
  // Other candidates:
  // - (15, 100) to (0, 100) = 15 (farther)
  // - (0, 50) to (0, 100) = 50 (farther)
  // - (20, 50) to (0, 100) = sqrt(20^2 + 50^2) ≈ 53.85 (farther)
  expect(result.lineId).toBe('top');
});
