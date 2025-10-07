import { expect, test } from 'vitest'
import * as densifyOperator from "@arcgis/core/geometry/operators/densifyOperator";
import * as geodeticDensifyOperator from "@arcgis/core/geometry/operators/geodeticDensifyOperator";
import * as generalizeOperator from "@arcgis/core/geometry/operators/generalizeOperator";
import * as geodeticAreaOperator from "@arcgis/core/geometry/operators/geodeticAreaOperator";
import * as areaOperator from "@arcgis/core/geometry/operators/areaOperator";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator";
import * as intersectionOperator from '@arcgis/core/geometry/operators/intersectionOperator';
import * as cutOperator from '@arcgis/core/geometry/operators/cutOperator';
import Polyline from "@arcgis/core/geometry/Polyline";
import Polygon from "@arcgis/core/geometry/Polygon";
import fs from 'node:fs';
import * as Terraformer from "@terraformer/arcgis"

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
