package uk.co.fivium.gisalphatest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.gisalphatest.util.EsriGeometryApiTestUtil.getCoordinates;
import static uk.co.fivium.gisalphatest.util.MathUtil.roundDecimalPlaces;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_BNG_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_ED50_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.rotateCoordinateRing;

import com.esri.core.geometry.OperatorDensifyByLength;
import com.esri.core.geometry.OperatorGeneralize;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCLineString;
import com.esri.core.geometry.ogc.OGCPolygon;
import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.fivium.gisalphatest.util.Coordinate;
import uk.co.fivium.gisalphatest.util.TestUtil;

class EsriGeometryApiTest {

  private static final SpatialReference ED50_SR = SpatialReference.create(TestUtil.ED50_SR);
  private static final SpatialReference BNG_SR = SpatialReference.create(TestUtil.BNG_SR);

  @Test
  void densify() throws Exception {
    var inputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/input-line-string.geojson"), StandardCharsets.UTF_8);
    var expectedOutputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/output-line-string.geojson"), StandardCharsets.UTF_8);

    var inputLineString = (OGCLineString) OGCGeometry.fromGeoJson(inputLineStringGeoJson);
    var expectedOutputLineString = (OGCLineString) OGCGeometry.fromGeoJson(expectedOutputLineStringGeoJson);

    inputLineString.setSpatialReference(ED50_SR);
    expectedOutputLineString.setSpatialReference(ED50_SR);

    var densifiedEsriPolyline = (Polyline) OperatorDensifyByLength.local()
        .execute(inputLineString.getEsriGeometry(), roundDecimalPlaces(20.0 / 3600, 11), null);
    var densifiedLineString = (OGCLineString) OGCGeometry
        .createFromEsriGeometry(densifiedEsriPolyline, ED50_SR);

    assertThat(getRoundedCoordinates(densifiedLineString, 11))
        .isEqualTo(getRoundedCoordinates(expectedOutputLineString, 11));
  }

  @Test
  void simplify() throws Exception {
    var inputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/output-line-string.geojson"), StandardCharsets.UTF_8);
    var expectedOutputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/input-line-string.geojson"), StandardCharsets.UTF_8);

    var inputLineString = (OGCLineString) OGCGeometry.fromGeoJson(inputLineStringGeoJson);
    var expectedOutputLineString = (OGCLineString) OGCGeometry.fromGeoJson(expectedOutputLineStringGeoJson);

    inputLineString.setSpatialReference(ED50_SR);
    expectedOutputLineString.setSpatialReference(ED50_SR);

    var simplifiedEsriPolyline = (Polyline) OperatorGeneralize.local()
        .execute(inputLineString.getEsriGeometry(), 0.01, false, null);
    var simplifiedLineString = (OGCLineString) OGCGeometry
        .createFromEsriGeometry(simplifiedEsriPolyline, ED50_SR);

    assertThat(getCoordinates(simplifiedLineString)).isEqualTo(getCoordinates(expectedOutputLineString));
  }

  @Test
  void area_ed50() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area/input-ed50-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);

    inputPolygon.setSpatialReference(ED50_SR);

    // There is an OperatorGeodeticArea, however, it is not implemented
    assertThat(roundDecimalPlaces(inputPolygon.area() / 1000000, 11))
        .isEqualTo(roundDecimalPlaces(ORACLE_AREA_CALCULATION_ED50_POLYGON_AREA_KM2, 11));
  }

  @Test
  void area_bng() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area/input-bng-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);

    inputPolygon.setSpatialReference(BNG_SR);

    // There is an OperatorGeodeticArea, however, it is not implemented
    assertThat(roundDecimalPlaces(inputPolygon.area() / 1000000, 13))
        .isEqualTo(roundDecimalPlaces(ORACLE_AREA_CALCULATION_BNG_POLYGON_AREA_KM2, 13));
  }

  @Test
  void union_polygons() throws Exception {
    var inputPolygon1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/polygons/input-polygon-1.geojson"), StandardCharsets.UTF_8);
    var inputPolygon2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/polygons/input-polygon-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/polygons/output-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon1 = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygon1GeoJson);
    var inputPolygon2 = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygon2GeoJson);
    var expectedOutputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygonGeoJson);

    inputPolygon1.setSpatialReference(ED50_SR);
    inputPolygon2.setSpatialReference(ED50_SR);
    expectedOutputPolygon.setSpatialReference(ED50_SR);

    var unionPolygon = (OGCPolygon) inputPolygon1.union(inputPolygon2);

    assertThat(rotateCoordinateRing(getCoordinates(unionPolygon.exteriorRing()), 485))
        .isEqualTo(getCoordinates(expectedOutputPolygon.exteriorRing()));
  }

  @Test
  void union_lineStrings() throws Exception {
    var inputLineString1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/line-strings/input-line-string-1.geojson"), StandardCharsets.UTF_8);
    var inputLineString2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/line-strings/input-line-string-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/line-strings/output-line-string.geojson"), StandardCharsets.UTF_8);

    var inputLineString1 = (OGCLineString) OGCGeometry.fromGeoJson(inputLineString1GeoJson);
    var inputLineString2 = (OGCLineString) OGCGeometry.fromGeoJson(inputLineString2GeoJson);
    var expectedOutputLineString = (OGCLineString) OGCGeometry.fromGeoJson(expectedOutputLineStringGeoJson);

    inputLineString1.setSpatialReference(ED50_SR);
    inputLineString2.setSpatialReference(ED50_SR);
    expectedOutputLineString.setSpatialReference(ED50_SR);

    var unionLineString = (OGCLineString) inputLineString1.union(inputLineString2);

    assertThat(getCoordinates(unionLineString)).isEqualTo(getCoordinates(expectedOutputLineString));
  }

  @Test
  void intersect_polygons() throws Exception {
    var inputPolygon1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/polygons/input-polygon-1.geojson"), StandardCharsets.UTF_8);
    var inputPolygon2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/polygons/input-polygon-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/polygons/output-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon1 = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygon1GeoJson);
    var inputPolygon2 = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygon2GeoJson);
    var expectedOutputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygonGeoJson);

    inputPolygon1.setSpatialReference(ED50_SR);
    inputPolygon2.setSpatialReference(ED50_SR);
    expectedOutputPolygon.setSpatialReference(ED50_SR);

    var intersectionPolygon = (OGCPolygon) inputPolygon1.intersection(inputPolygon2);

    var simplifiedExpectedOutputEsriPolygon = (Polygon) OperatorGeneralize.local()
        .execute(expectedOutputPolygon.getEsriGeometry(), 0.01, false, null);
    var simplifiedExpectedOutputPolygon = (OGCPolygon) OGCGeometry
        .createFromEsriGeometry(simplifiedExpectedOutputEsriPolygon, ED50_SR);

    assertThat(rotateCoordinateRing(getRoundedCoordinates(intersectionPolygon.exteriorRing(), 5), 2))
        .isEqualTo(getRoundedCoordinates(simplifiedExpectedOutputPolygon.exteriorRing(), 5));
  }

  @Test
  void intersect_lineStrings() throws Exception {
    var inputLineString1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/line-strings/input-line-string-1.geojson"), StandardCharsets.UTF_8);
    var inputLineString2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/line-strings/input-line-string-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/line-strings/output-line-string.geojson"), StandardCharsets.UTF_8);

    var inputLineString1 = (OGCLineString) OGCGeometry.fromGeoJson(inputLineString1GeoJson);
    var inputLineString2 = (OGCLineString) OGCGeometry.fromGeoJson(inputLineString2GeoJson);
    var expectedOutputLineString = (OGCLineString) OGCGeometry.fromGeoJson(expectedOutputLineStringGeoJson);

    inputLineString1.setSpatialReference(ED50_SR);
    inputLineString2.setSpatialReference(ED50_SR);
    expectedOutputLineString.setSpatialReference(ED50_SR);

    var intersectionLineString = (OGCLineString) inputLineString1.intersection(inputLineString2);

    assertThat(getCoordinates(intersectionLineString)).isEqualTo(getCoordinates(expectedOutputLineString));
  }

  private List<Coordinate> getRoundedCoordinates(OGCLineString lineString, int places) {
    var coordinates = new ArrayList<Coordinate>();
    for (var i = 0; i < lineString.numPoints(); i++) {
      var point = lineString.pointN(i);
      coordinates.add(new Coordinate(roundDecimalPlaces(point.X(), places), roundDecimalPlaces(point.Y(), places)));
    }
    return coordinates;
  }
}
