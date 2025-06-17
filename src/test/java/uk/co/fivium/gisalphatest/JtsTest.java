package uk.co.fivium.gisalphatest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.gisalphatest.util.MathUtil.roundDecimalPlaces;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_OFFSHORE_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_ONSHORE_POLYGON_AREA_KM2;

import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import uk.co.fivium.gisalphatest.util.Coordinate;

class JtsTest {

  private static final GeoJsonReader GEO_JSON_READER = new GeoJsonReader();

  @Test
  void densification() throws Exception {
    var inputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densification-input-line.geojson"), StandardCharsets.UTF_8);
    var expectedOutputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densification-output-line.geojson"), StandardCharsets.UTF_8);

    var inputLineString = (LineString) GEO_JSON_READER.read(inputGeoJson);
    var expectedOutputLineString = (LineString) GEO_JSON_READER.read(expectedOutputGeoJson);

    var densifier = new Densifier(inputLineString);
    densifier.setDistanceTolerance(roundDecimalPlaces(20.0 / 3600, 11));
    var densifiedLineString = (LineString) densifier.getResultGeometry();

    assertThat(getRoundedCoordinates(densifiedLineString, 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 11));
  }

  @Test
  void simplification() throws Exception {
    var inputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densification-output-line.geojson"), StandardCharsets.UTF_8);
    var expectedOutputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densification-input-line.geojson"), StandardCharsets.UTF_8);

    var inputLineString = (LineString) GEO_JSON_READER.read(inputGeoJson);
    var expectedOutputLineString = (LineString) GEO_JSON_READER.read(expectedOutputGeoJson);

    var simplifiedLineString = (LineString) DouglasPeuckerSimplifier.simplify(inputLineString, Double.MAX_VALUE);

    assertThat(getRoundedCoordinates(simplifiedLineString, 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 11));
  }

  @Test
  void areaCalculation_offshore() throws Exception {
    var inputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area-calculation-offshore-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (Polygon) GEO_JSON_READER.read(inputGeoJson);

    assertThat(roundDecimalPlaces(inputPolygon.getArea() / 1000000, 11))
        .isEqualTo(roundDecimalPlaces(ORACLE_AREA_CALCULATION_OFFSHORE_POLYGON_AREA_KM2, 11));
  }

  @Test
  void areaCalculation_onshore() throws Exception {
    var inputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area-calculation-onshore-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (Polygon) GEO_JSON_READER.read(inputGeoJson);

    assertThat(roundDecimalPlaces(inputPolygon.getArea() / 1000000, 11))
        .isEqualTo(roundDecimalPlaces(ORACLE_AREA_CALCULATION_ONSHORE_POLYGON_AREA_KM2, 11));
  }

  @Test
  void union() throws Exception {
    var input1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union-input-geom-1.geojson"), StandardCharsets.UTF_8);
    var input2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union-input-geom-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union-output-geom.geojson"), StandardCharsets.UTF_8);

    var input1Polygon = (Polygon) GEO_JSON_READER.read(input1GeoJson);
    var input2Polygon = (Polygon) GEO_JSON_READER.read(input2GeoJson);
    var expectedOutputPolygon = (Polygon) GEO_JSON_READER.read(expectedOutputGeoJson);

    var unionPolygon = (Polygon) input1Polygon.union(input2Polygon);

    assertThat(getRoundedCoordinates(unionPolygon.getExteriorRing(), 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputPolygon.getExteriorRing(), 11));
  }

  @Test
  void intersection() throws Exception {
    var input1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersection-input-geom-1.geojson"), StandardCharsets.UTF_8);
    var input2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersection-input-geom-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersection-output-geom.geojson"), StandardCharsets.UTF_8);

    var input1Polygon = (Polygon) GEO_JSON_READER.read(input1GeoJson);
    var input2Polygon = (Polygon) GEO_JSON_READER.read(input2GeoJson);
    var expectedOutputPolygon = (Polygon) GEO_JSON_READER.read(expectedOutputGeoJson);

    var intersectionPolygon = (Polygon) input1Polygon.intersection(input2Polygon);

    var simplifiedExpectedOutputGeoJson = (Polygon) DouglasPeuckerSimplifier.simplify(expectedOutputPolygon, 0.01);

    assertThat(getRoundedCoordinates(intersectionPolygon.getExteriorRing(), 5))
        .containsExactlyElementsOf(getRoundedCoordinates(simplifiedExpectedOutputGeoJson.getExteriorRing(), 5));
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
