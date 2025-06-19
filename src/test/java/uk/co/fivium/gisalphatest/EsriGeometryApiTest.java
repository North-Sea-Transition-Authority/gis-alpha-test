package uk.co.fivium.gisalphatest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.gisalphatest.util.MathUtil.roundDecimalPlaces;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_OFFSHORE_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_ONSHORE_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_OFFSHORE_SR;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_ONSHORE_SR;

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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.fivium.gisalphatest.util.Coordinate;

class EsriGeometryApiTest {

  private static final SpatialReference OFFSHORE_SR = SpatialReference.create(ORACLE_OFFSHORE_SR);
  private static final SpatialReference ONSHORE_SR = SpatialReference.create(ORACLE_ONSHORE_SR);

  @Test
  void densify() throws Exception {
    var inputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/input-line-string.geojson"), StandardCharsets.UTF_8);
    var expectedOutputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/output-line-string.geojson"), StandardCharsets.UTF_8);

    var inputLineString = (OGCLineString) OGCGeometry.fromGeoJson(inputLineStringGeoJson);
    var expectedOutputLineString = (OGCLineString) OGCGeometry.fromGeoJson(expectedOutputLineStringGeoJson);

    inputLineString.setSpatialReference(OFFSHORE_SR);
    expectedOutputLineString.setSpatialReference(OFFSHORE_SR);

    var densifiedEsriPolyline = (Polyline) OperatorDensifyByLength.local()
        .execute(inputLineString.getEsriGeometry(), roundDecimalPlaces(20.0 / 3600, 11), null);
    var densifiedLineString = (OGCLineString) OGCGeometry
        .createFromEsriGeometry(densifiedEsriPolyline, OFFSHORE_SR);

    assertThat(getRoundedCoordinates(densifiedLineString, 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 11));
  }

  @Test
  void simplify() throws Exception {
    var inputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/output-line-string.geojson"), StandardCharsets.UTF_8);
    var expectedOutputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/input-line-string.geojson"), StandardCharsets.UTF_8);

    var inputLineString = (OGCLineString) OGCGeometry.fromGeoJson(inputLineStringGeoJson);
    var expectedOutputLineString = (OGCLineString) OGCGeometry.fromGeoJson(expectedOutputLineStringGeoJson);

    inputLineString.setSpatialReference(OFFSHORE_SR);
    expectedOutputLineString.setSpatialReference(OFFSHORE_SR);

    var simplifiedEsriPolyline = (Polyline) OperatorGeneralize.local()
        .execute(inputLineString.getEsriGeometry(), 0.01, false, null);
    var simplifiedLineString = (OGCLineString) OGCGeometry
        .createFromEsriGeometry(simplifiedEsriPolyline, OFFSHORE_SR);

    assertThat(getRoundedCoordinates(simplifiedLineString, 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 11));
  }

  @Test
  void area_offshore() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area/input-offshore-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);

    inputPolygon.setSpatialReference(OFFSHORE_SR);

    // There is an OperatorGeodeticArea, however, it is not implemented
    assertThat(roundDecimalPlaces(inputPolygon.area() / 1000000, 11))
        .isEqualTo(roundDecimalPlaces(ORACLE_AREA_CALCULATION_OFFSHORE_POLYGON_AREA_KM2, 11));
  }

  @Test
  void area_onshore() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area/input-onshore-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);

    inputPolygon.setSpatialReference(ONSHORE_SR);

    // There is an OperatorGeodeticArea, however, it is not implemented
    assertThat(roundDecimalPlaces(inputPolygon.area() / 1000000, 11))
        .isEqualTo(roundDecimalPlaces(ORACLE_AREA_CALCULATION_ONSHORE_POLYGON_AREA_KM2, 11));
  }

  @Test
  void union() throws Exception {
    var inputPolygon1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/input-polygon-1.geojson"), StandardCharsets.UTF_8);
    var inputPolygon2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/input-polygon-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/output-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon1 = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygon1GeoJson);
    var inputPolygon2 = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygon2GeoJson);
    var expectedOutputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygonGeoJson);

    inputPolygon1.setSpatialReference(OFFSHORE_SR);
    inputPolygon2.setSpatialReference(OFFSHORE_SR);
    expectedOutputPolygon.setSpatialReference(OFFSHORE_SR);

    var unionPolygon = (OGCPolygon) inputPolygon1.union(inputPolygon2);

    assertThat(getRoundedCoordinates(unionPolygon.exteriorRing(), 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputPolygon.exteriorRing(), 11));
  }

  @Test
  void intersect() throws Exception {
    var inputPolygon1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/input-polygon-1.geojson"), StandardCharsets.UTF_8);
    var inputPolygon2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/input-polygon-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/output-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon1 = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygon1GeoJson);
    var inputPolygon2 = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygon2GeoJson);
    var expectedOutputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygonGeoJson);

    inputPolygon1.setSpatialReference(OFFSHORE_SR);
    inputPolygon2.setSpatialReference(OFFSHORE_SR);
    expectedOutputPolygon.setSpatialReference(OFFSHORE_SR);

    var intersectionPolygon = (OGCPolygon) inputPolygon1.intersection(inputPolygon2);

    var simplifiedExpectedOutputEsriPolygon = (Polygon) OperatorGeneralize.local()
        .execute(expectedOutputPolygon.getEsriGeometry(), 0.01, false, null);
    var simplifiedExpectedOutputPolygon = (OGCPolygon) OGCGeometry
        .createFromEsriGeometry(simplifiedExpectedOutputEsriPolygon, OFFSHORE_SR);

    assertThat(getRoundedCoordinates(intersectionPolygon.exteriorRing(), 5))
        .containsExactlyElementsOf(getRoundedCoordinates(simplifiedExpectedOutputPolygon.exteriorRing(), 5));
  }

  private List<Coordinate> getRoundedCoordinates(OGCLineString lineString, int places) {
    var coordinatesSet = new HashSet<Coordinate>();
    for (var i = 0; i < lineString.numPoints(); i++) {
      var point = lineString.pointN(i);
      coordinatesSet.add(new Coordinate(roundDecimalPlaces(point.X(), places), roundDecimalPlaces(point.Y(), places)));
    }
    var coordinates = new ArrayList<>(coordinatesSet);
    coordinates.sort(Comparator.comparing(Coordinate::x).thenComparing(Coordinate::z));
    return coordinates;
  }
}
