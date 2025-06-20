package uk.co.fivium.gisalphatest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.gisalphatest.util.MathUtil.roundDecimalPlaces;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_OFFSHORE_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_ONSHORE_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_OFFSHORE_SR;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_ONSHORE_SR;

import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import uk.co.fivium.gisalphatest.util.Coordinate;

class JtsTest {

  private static final GeoJsonReader OFFSHORE_GEO_JSON_READER =
      new GeoJsonReader(new GeometryFactory(new PrecisionModel(), ORACLE_OFFSHORE_SR));
  private static final GeoJsonReader ONSHORE_GEO_JSON_READER =
      new GeoJsonReader(new GeometryFactory(new PrecisionModel(), ORACLE_ONSHORE_SR));

  @Test
  void densify() throws Exception {
    var inputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/input-line-string.geojson"), StandardCharsets.UTF_8);
    var expectedOutputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/output-line-string.geojson"), StandardCharsets.UTF_8);

    var inputLineString = (LineString) OFFSHORE_GEO_JSON_READER.read(inputLineStringGeoJson);
    var expectedOutputLineString = (LineString) OFFSHORE_GEO_JSON_READER.read(expectedOutputLineStringGeoJson);

    var densifier = new Densifier(inputLineString);
    densifier.setDistanceTolerance(roundDecimalPlaces(20.0 / 3600, 11));
    var densifiedLineString = (LineString) densifier.getResultGeometry();

    assertThat(getRoundedCoordinates(densifiedLineString, 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 11));
  }

  @Test
  void simplify() throws Exception {
    var inputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/output-line-string.geojson"), StandardCharsets.UTF_8);
    var expectedOutputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densify/input-line-string.geojson"), StandardCharsets.UTF_8);

    var inputLineString = (LineString) OFFSHORE_GEO_JSON_READER.read(inputLineStringGeoJson);
    var expectedOutputLineString = (LineString) OFFSHORE_GEO_JSON_READER.read(expectedOutputLineStringGeoJson);

    var simplifiedLineString = (LineString) DouglasPeuckerSimplifier.simplify(inputLineString, 0.01);

    assertThat(getRoundedCoordinates(simplifiedLineString, 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 11));
  }

  @Test
  void area_offshore() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area/input-offshore-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (Polygon) OFFSHORE_GEO_JSON_READER.read(inputPolygonGeoJson);

    assertThat(roundDecimalPlaces(inputPolygon.getArea() / 1000000, 11))
        .isEqualTo(roundDecimalPlaces(ORACLE_AREA_CALCULATION_OFFSHORE_POLYGON_AREA_KM2, 11));
  }

  @Test
  void area_onshore() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area/input-onshore-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (Polygon) ONSHORE_GEO_JSON_READER.read(inputPolygonGeoJson);

    assertThat(roundDecimalPlaces(inputPolygon.getArea() / 1000000, 11))
        .isEqualTo(roundDecimalPlaces(ORACLE_AREA_CALCULATION_ONSHORE_POLYGON_AREA_KM2, 11));
  }

  @Test
  void union_polygons() throws Exception {
    var inputPolygon1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/polygons/input-polygon-1.geojson"), StandardCharsets.UTF_8);
    var inputPolygon2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/polygons/input-polygon-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/polygons/output-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon1 = (Polygon) OFFSHORE_GEO_JSON_READER.read(inputPolygon1GeoJson);
    var inputPolygon2 = (Polygon) OFFSHORE_GEO_JSON_READER.read(inputPolygon2GeoJson);
    var expectedOutputPolygon = (Polygon) OFFSHORE_GEO_JSON_READER.read(expectedOutputPolygonGeoJson);

    var unionPolygon = (Polygon) inputPolygon1.union(inputPolygon2);

    assertThat(getRoundedCoordinates(unionPolygon.getExteriorRing(), 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputPolygon.getExteriorRing(), 11));
  }

  @Test
  void union_lineStrings() throws Exception {
    var inputLineString1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/line-strings/input-line-string-1.geojson"), StandardCharsets.UTF_8);
    var inputLineString2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/line-strings/input-line-string-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/line-strings/output-line-string.geojson"), StandardCharsets.UTF_8);

    var inputLineString1 = (LineString) OFFSHORE_GEO_JSON_READER.read(inputLineString1GeoJson);
    var inputLineString2 = (LineString) OFFSHORE_GEO_JSON_READER.read(inputLineString2GeoJson);
    var expectedOutputLineString = (LineString) OFFSHORE_GEO_JSON_READER.read(expectedOutputLineStringGeoJson);

    var unionLineString = (LineString) inputLineString1.union(inputLineString2);

    assertThat(getRoundedCoordinates(unionLineString, 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 11));
  }

  @Test
  void intersect_polygons() throws Exception {
    var inputPolygon1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/polygons/input-polygon-1.geojson"), StandardCharsets.UTF_8);
    var inputPolygon2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/polygons/input-polygon-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/polygons/output-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon1 = (Polygon) OFFSHORE_GEO_JSON_READER.read(inputPolygon1GeoJson);
    var inputPolygon2 = (Polygon) OFFSHORE_GEO_JSON_READER.read(inputPolygon2GeoJson);
    var expectedOutputPolygon = (Polygon) OFFSHORE_GEO_JSON_READER.read(expectedOutputPolygonGeoJson);

    var intersectionPolygon = (Polygon) inputPolygon1.intersection(inputPolygon2);

    var simplifiedExpectedOutputPolygon = (Polygon) DouglasPeuckerSimplifier.simplify(expectedOutputPolygon, 0.01);

    assertThat(getRoundedCoordinates(intersectionPolygon.getExteriorRing(), 5))
        .containsExactlyElementsOf(getRoundedCoordinates(simplifiedExpectedOutputPolygon.getExteriorRing(), 5));
  }

  @Test
  void intersect_lineStrings() throws Exception {
    var inputLineString1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/line-strings/input-line-string-1.geojson"), StandardCharsets.UTF_8);
    var inputLineString2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/line-strings/input-line-string-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersect/line-strings/output-line-string.geojson"), StandardCharsets.UTF_8);

    var inputLineString1 = (LineString) OFFSHORE_GEO_JSON_READER.read(inputLineString1GeoJson);
    var inputLineString2 = (LineString) OFFSHORE_GEO_JSON_READER.read(inputLineString2GeoJson);
    var expectedOutputLineString = (LineString) OFFSHORE_GEO_JSON_READER.read(expectedOutputLineStringGeoJson);

    var intersectionLineString = (LineString) inputLineString1.intersection(inputLineString2);

    assertThat(getRoundedCoordinates(intersectionLineString, 5))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 5));
  }

  private List<Coordinate> getRoundedCoordinates(LineString lineString, int places) {
    var coordinatesSet = new HashSet<Coordinate>();
    for (var i = 0; i < lineString.getNumPoints(); i++) {
      var point = lineString.getPointN(i);
      coordinatesSet.add(new Coordinate(roundDecimalPlaces(point.getX(), places), roundDecimalPlaces(point.getY(), places)));
    }
    var coordinates = new ArrayList<>(coordinatesSet);
    coordinates.sort(Comparator.comparing(Coordinate::x).thenComparing(Coordinate::z));
    return coordinates;
  }
}
