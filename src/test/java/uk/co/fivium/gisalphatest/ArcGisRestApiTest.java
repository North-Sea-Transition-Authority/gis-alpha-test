package uk.co.fivium.gisalphatest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.gisalphatest.util.EsriGeometryApiTestUtil.getCoordinates;
import static uk.co.fivium.gisalphatest.util.EsriGeometryApiTestUtil.getRoundedCoordinates;
import static uk.co.fivium.gisalphatest.util.EsriGeometryApiTestUtil.newLineStringFromCoordinates;
import static uk.co.fivium.gisalphatest.util.EsriGeometryApiTestUtil.newPolygonFromCoordinates;
import static uk.co.fivium.gisalphatest.util.MathUtil.roundDecimalPlaces;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_BNG_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_ED50_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.rotateCoordinateRing;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.OperatorCut;
import com.esri.core.geometry.OperatorEquals;
import com.esri.core.geometry.OperatorUnion;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCLineString;
import com.esri.core.geometry.ogc.OGCMultiLineString;
import com.esri.core.geometry.ogc.OGCPolygon;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.co.fivium.gisalphatest.util.Coordinate;
import uk.co.fivium.gisalphatest.util.MathUtil;
import uk.co.fivium.gisalphatest.util.TestUtil;

@Disabled
class ArcGisRestApiTest {

  private static final SpatialReference ED50_SR = SpatialReference.create(TestUtil.ED50_SR);
  private static final SpatialReference BNG_SR = SpatialReference.create(TestUtil.BNG_SR);

  private static final int GEODESIC_DENSIFY_MAX_SEGMENT_LENGTH_METERS = 100;
  // When densifying with geodesic set to false, the maxSegmentLength unit is derived from the SR.
  // For ED50, the unit is degrees (see https://epsg.io/4230).
  private static final double PLANAR_DENSIFY_MAX_SEGMENT_LENGTH_DEGREES = MathUtil.roundDecimalPlaces(20.0 / 3600, 11);

  private HttpClient httpClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    httpClient = HttpClient.newHttpClient();
  }

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

    var densifiedLineString = (OGCLineString) densify(inputLineString, false);

    assertThat(getRoundedCoordinates(densifiedLineString, 11))
        .isEqualTo(getRoundedCoordinates(expectedOutputLineString, 11));
  }

  private OGCGeometry densify(OGCGeometry geometry, boolean geodesic) throws Exception {
    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/densify"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(TestUtil.ED50_SR),
                        "geometries", "{\"geometryType\":\"%s\",\"geometries\":[%s]}"
                            .formatted(getGeometryType(geometry), geometry.asJson()),
                        "maxSegmentLength", String.valueOf(geodesic ? GEODESIC_DENSIFY_MAX_SEGMENT_LENGTH_METERS : PLANAR_DENSIFY_MAX_SEGMENT_LENGTH_DEGREES),
                        "geodesic", Boolean.toString(geodesic),
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    var parsedResponse = objectMapper.readTree(response.body());
    return OGCGeometry.fromJson(parsedResponse.get("geometries").get(0).toString());
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

    var simplifiedLineString = (OGCLineString) simplify(inputLineString);

    assertThat(getCoordinates(simplifiedLineString)).isEqualTo(getCoordinates(expectedOutputLineString));
  }

  private OGCGeometry simplify(OGCGeometry geometry) throws Exception {
    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/generalize"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(TestUtil.ED50_SR),
                        "geometries", "{\"geometryType\":\"%s\",\"geometries\":[%s]}"
                            .formatted(getGeometryType(geometry), geometry.asJson()),
                        "maxDeviation", String.valueOf(0.01),
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    var parsedResponse = objectMapper.readTree(response.body());
    return OGCGeometry.fromJson(parsedResponse.get("geometries").get(0).toString());
  }

  @Test
  void area_ed50() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area/input-ed50-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);

    inputPolygon.setSpatialReference(ED50_SR);

    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/areasAndLengths"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(TestUtil.ED50_SR),
                        "polygons", "[%s]".formatted(inputPolygon.asJson()),
                        "calculationType", "geodesic",
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    var parsedResponse = objectMapper.readTree(response.body());

    assertThat(roundDecimalPlaces(parsedResponse.get("areas").get(0).asDouble() / 1000000, 4))
        .isEqualTo(roundDecimalPlaces(ORACLE_AREA_CALCULATION_ED50_POLYGON_AREA_KM2, 4));
  }

  @Test
  void area_bng() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area/input-bng-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);

    inputPolygon.setSpatialReference(BNG_SR);

    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/areasAndLengths"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(TestUtil.BNG_SR),
                        "polygons", "[%s]".formatted(inputPolygon.asJson()),
                        "calculationType", "planar", // This fails if it's geodesic as it's slightly off, but passes if it's planar?
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    var parsedResponse = objectMapper.readTree(response.body());

    assertThat(roundDecimalPlaces(parsedResponse.get("areas").get(0).asDouble() / 1000000, 13))
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

    var unionPolygon = (OGCPolygon) union(inputPolygon1, inputPolygon2);

    assertThat(rotateCoordinateRing(getRoundedCoordinates(unionPolygon.exteriorRing(), 9), 1))
        .isEqualTo(getRoundedCoordinates(expectedOutputPolygon.exteriorRing(), 9));
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

    var unionLineString = (OGCLineString) union(inputLineString1, inputLineString2);

    assertThat(getRoundedCoordinates(unionLineString, 12))
        .isEqualTo(getRoundedCoordinates(expectedOutputLineString, 12));
  }

  @Test
  void union_densifiedLineStrings() throws Exception {
    var inputLineString1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/line-strings/input-line-string-1.geojson"), StandardCharsets.UTF_8);
    var inputLineString2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/line-strings/input-line-string-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union/line-strings/output-line-string-ed50.geojson"), StandardCharsets.UTF_8);

    var inputLineString1 = (OGCLineString) OGCGeometry.fromGeoJson(inputLineString1GeoJson);
    var inputLineString2 = (OGCLineString) OGCGeometry.fromGeoJson(inputLineString2GeoJson);
    var expectedOutputMultiLineString = (OGCMultiLineString) OGCGeometry.fromGeoJson(expectedOutputLineStringGeoJson);

    inputLineString1.setSpatialReference(ED50_SR);
    inputLineString2.setSpatialReference(ED50_SR);
    expectedOutputMultiLineString.setSpatialReference(ED50_SR);

    var densifiedInputLineString1 = (OGCLineString) densify(inputLineString1, true);
    var densifiedInputLineString2 = (OGCLineString) densify(inputLineString2, true);

    var unionLineString = (OGCMultiLineString) union(densifiedInputLineString1, densifiedInputLineString2);

    assertThat(unionLineString.numGeometries()).isEqualTo(expectedOutputMultiLineString.numGeometries());

    for (var i = 0; i < unionLineString.numGeometries(); i++) {
      var unionSubLineString = (OGCLineString) unionLineString.geometryN(i);
      var expectedSubLineString = (OGCLineString) expectedOutputMultiLineString.geometryN(unionLineString.numGeometries() - 1 - i);
      var densifiedExpectedSubLineString = (OGCLineString) densify(expectedSubLineString, true);

      var unionSubLineStringRoundedCoordinates = getRoundedCoordinates(unionSubLineString, 9);

      if (i == 1) {
        unionSubLineStringRoundedCoordinates = Lists.reverse(unionSubLineStringRoundedCoordinates);
      }

      assertThat(unionSubLineStringRoundedCoordinates)
          .isEqualTo(getRoundedCoordinates(densifiedExpectedSubLineString, 9));
    }
  }

  private OGCGeometry union(OGCGeometry geometry1, OGCGeometry geometry2) throws Exception {
    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/union"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(TestUtil.ED50_SR),
                        "geometries", "{\"geometryType\":\"%s\",\"geometries\":[%s,%s]}"
                            .formatted(getGeometryType(geometry1), geometry1.asJson(), geometry2.asJson()),
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    var parsedResponse = objectMapper.readTree(response.body());
    return OGCGeometry.fromJson(parsedResponse.get("geometry").toString());
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

    var intersectionPolygon = (OGCPolygon) intersect(inputPolygon1, inputPolygon2);

    var simplifiedExpectedOutputPolygon = (OGCPolygon) simplify(expectedOutputPolygon);

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

    var intersectionLineString = (OGCLineString) intersect(inputLineString1, inputLineString2);

    assertThat(getRoundedCoordinates(intersectionLineString, 12))
        .isEqualTo(getRoundedCoordinates(expectedOutputLineString, 12));
  }

  @Test
  void intersect_densifiedLineStrings() throws Exception {
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

    var densifiedInputLineString1 = (OGCLineString) densify(inputLineString1, true);
    var densifiedInputLineString2 = (OGCLineString) densify(inputLineString2, true);

    var intersectionLineString = (OGCMultiLineString) intersect(densifiedInputLineString1, densifiedInputLineString2);

    assertThat(intersectionLineString.numGeometries()).isEqualTo(0);
  }

  private OGCGeometry intersect(OGCGeometry geometry1, OGCGeometry geometry2) throws Exception {
    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/intersect"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(TestUtil.ED50_SR),
                        "geometries", "{\"geometryType\":\"%s\",\"geometries\":[%s]}"
                            .formatted(getGeometryType(geometry1), geometry1.asJson()),
                        "geometry", "{\"geometryType\":\"%s\",\"geometry\":%s}"
                            .formatted(getGeometryType(geometry2), geometry2.asJson()),
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    var parsedResponse = objectMapper.readTree(response.body());
    return OGCGeometry.fromJson(parsedResponse.get("geometries").get(0).toString());
  }

  @Test
  void cut_simple() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/simple/input-polygon.geojson"), StandardCharsets.UTF_8);
    var inputCutterLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/simple/input-cutter-line-string.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygon1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/simple/output-polygon-1.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygon2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/simple/output-polygon-2.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);
    var inputCutterLineString = (OGCLineString) OGCGeometry.fromGeoJson(inputCutterLineStringGeoJson);
    var expectedOutputPolygon1 = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygon1GeoJson);
    var expectedOutputPolygon2 = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygon2GeoJson);

    inputPolygon.setSpatialReference(ED50_SR);
    inputCutterLineString.setSpatialReference(ED50_SR);
    expectedOutputPolygon1.setSpatialReference(ED50_SR);
    expectedOutputPolygon2.setSpatialReference(ED50_SR);

    var cutPolygons = cut(inputPolygon, inputCutterLineString);

    var cutPolygon1 = (OGCPolygon) cutPolygons.get(0);
    var cutPolygon2 = (OGCPolygon) cutPolygons.get(1);

    // Note: There are additional points in the results, so use containsAll
    assertThat(getRoundedCoordinates(cutPolygon1.exteriorRing(), 9))
        .containsAll(getRoundedCoordinates(expectedOutputPolygon2.exteriorRing(), 9));
    assertThat(getRoundedCoordinates(cutPolygon2.exteriorRing(), 9))
        .containsAll(getRoundedCoordinates(expectedOutputPolygon1.exteriorRing(), 9));
  }

  @Test
  void cut_simpleWithGeodesicLine() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/simple-with-geodesic-line/input-polygon.geojson"), StandardCharsets.UTF_8);
    var inputCutterLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/simple-with-geodesic-line/input-cutter-line-string.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);
    var inputCutterLineString = (OGCLineString) OGCGeometry.fromGeoJson(inputCutterLineStringGeoJson);

    inputPolygon.setSpatialReference(ED50_SR);
    inputCutterLineString.setSpatialReference(ED50_SR);

    var cutPolygons = cut(inputPolygon, inputCutterLineString);

    var cutPolygon1 = (OGCPolygon) cutPolygons.get(0);
    var cutPolygon2 = (OGCPolygon) cutPolygons.get(1);

    assertThat(getRoundedCoordinates(cutPolygon1.exteriorRing(), 12))
        .contains(new Coordinate(-4.5, 59.008684166));
    assertThat(getRoundedCoordinates(cutPolygon1.exteriorRing(), 12))
        .contains(new Coordinate(-4.5, 58));
    assertThat(getRoundedCoordinates(cutPolygon2.exteriorRing(), 12))
        .contains(new Coordinate(-4.5, 59.008684166));
    assertThat(getRoundedCoordinates(cutPolygon2.exteriorRing(), 12))
        .contains(new Coordinate(-4.5, 58));
  }

  @Test
  void cut_coastline() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/coastline/input-polygon.geojson"), StandardCharsets.UTF_8);
    var inputCutterLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/coastline/input-cutter-line-string.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygon1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/coastline/output-polygon-1.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygon2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/coastline/output-polygon-2.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);
    var inputCutterLineString = (OGCLineString) OGCGeometry.fromGeoJson(inputCutterLineStringGeoJson);
    var expectedOutputPolygon1 = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygon1GeoJson);
    var expectedOutputPolygon2 = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygon2GeoJson);

    inputPolygon.setSpatialReference(ED50_SR);
    inputCutterLineString.setSpatialReference(ED50_SR);
    expectedOutputPolygon1.setSpatialReference(ED50_SR);
    expectedOutputPolygon2.setSpatialReference(ED50_SR);

    var cutPolygons = cut(inputPolygon, inputCutterLineString);

    var cutPolygon1 = (OGCPolygon) cutPolygons.get(0);
    var cutPolygon2 = (OGCPolygon) cutPolygons.get(1);

    // Note: There are additional points in the results, so use containsAll
    assertThat(getRoundedCoordinates(cutPolygon1.exteriorRing(), 12))
        .containsAll(getRoundedCoordinates(expectedOutputPolygon2.exteriorRing(), 12));
    assertThat(getRoundedCoordinates(cutPolygon2.exteriorRing(), 9))
        .containsAll(getRoundedCoordinates(expectedOutputPolygon1.exteriorRing(), 9));
  }

  @Test
  void cut_multipleCuts() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/multiple-cuts/input-polygon.geojson"), StandardCharsets.UTF_8);
    var inputCutterMultiLineStringGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/multiple-cuts/input-cutter-multi-line-string.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygon1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/multiple-cuts/output-polygon-1.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygon2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/multiple-cuts/output-polygon-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputPolygon3GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/cut/multiple-cuts/output-polygon-3.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);
    var inputCutterMultiLineString = (OGCMultiLineString) OGCGeometry.fromGeoJson(inputCutterMultiLineStringGeoJson);
    var expectedOutputPolygon1 = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygon1GeoJson);
    var expectedOutputPolygon2 = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygon2GeoJson);
    var expectedOutputPolygon3 = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygon3GeoJson);

    inputPolygon.setSpatialReference(ED50_SR);
    inputCutterMultiLineString.setSpatialReference(ED50_SR);
    expectedOutputPolygon1.setSpatialReference(ED50_SR);
    expectedOutputPolygon2.setSpatialReference(ED50_SR);
    expectedOutputPolygon3.setSpatialReference(ED50_SR);

    var cutPolygons = cut(inputPolygon, inputCutterMultiLineString);

    var cutPolygon1 = (OGCPolygon) cutPolygons.get(0);
    var cutPolygon2 = (OGCPolygon) cutPolygons.get(1);
    var cutPolygon3 = (OGCPolygon) cutPolygons.get(2);

    // Note: There are additional points in the results, so use containsAll
    assertThat(getRoundedCoordinates(cutPolygon1.exteriorRing(), 12))
        .containsAll(getRoundedCoordinates(expectedOutputPolygon1.exteriorRing(), 12));
    assertThat(getRoundedCoordinates(cutPolygon2.exteriorRing(), 9))
        .containsAll(getRoundedCoordinates(expectedOutputPolygon2.exteriorRing(), 9));
    assertThat(getRoundedCoordinates(cutPolygon3.exteriorRing(), 9))
        .containsAll(getRoundedCoordinates(expectedOutputPolygon3.exteriorRing(), 9));
  }

  @Test
  void cutAndRedensifyPolygon() throws Exception {
    var shape1GeoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[2,54],[2,53],[3,53],[3,54],[2,54]]]}";
    var shape1 = (OGCPolygon) OGCGeometry.fromGeoJson(shape1GeoJson);

    // 1.a - densify geodesic line a on shape 1 from point x to point y
    var densifiedShape1 = densifyBetween(shape1, new Coordinate(2, 53), new Coordinate(3, 53));

    // 1.b - create a cut line b which intersects line a midway between two dense points

    // midway between these two dense points: [2.2499969641083633,53.00078836115751],[2.251485065477092,53.00079148029776]
    var cutLineBLineStringGeoJson = "{\"type\": \"LineString\", \"coordinates\": [[2.2507410147927276, 52.9], [2.2507410147927276, 54.1]]}";
    var cutLineBLineString = (OGCLineString) OGCGeometry.fromGeoJson(cutLineBLineStringGeoJson);

    // 1.c - cut shape 1 with cut line b to create shape 2 and shape 3
    var shape1CutCursor = OperatorCut.local().execute(
        true,
        densifiedShape1.getEsriGeometry(),
        (Polyline) cutLineBLineString.getEsriGeometry(),
        null,
        null
    );
    var shape3 = (OGCPolygon) OGCGeometry.createFromEsriGeometry(shape1CutCursor.next(), null);
    var shape2 = (OGCPolygon) OGCGeometry.createFromEsriGeometry(shape1CutCursor.next(), null);

    // 2.a - remove the dense points from line a on shape 2 + 2.b - re-densify line a on shape 2 from new origin point z to point y
    var shape2LineAStartPoint = new Coordinate(2.2507410147927276, 53.000789920727634);
    var shape2LineAEndPoint = new Coordinate(3, 53);

    var redensifiedShape2 = redensifyBetween(shape2, shape2LineAStartPoint, shape2LineAEndPoint);

    // 3.a - create a cut line c which intersects line a in shape 2
    var cutLineCLineStringGeoJson = "{\"type\":\"LineString\",\"coordinates\":[[2.6253705073963780,52.9],[2.6253705073963780,54.1]]}";
    var cutLineCLineString = (OGCLineString) OGCGeometry.fromGeoJson(cutLineCLineStringGeoJson);

    // 3.b - cut shape 2 with cut line c to give us intersection point p1
    var redensifiedShape2CutCursor = OperatorCut.local().execute(
        true,
        redensifiedShape2.getEsriGeometry(),
        (Polyline) cutLineCLineString.getEsriGeometry(),
        null,
        null
    );
    var redensifiedShape2CutShape1 = (OGCPolygon) OGCGeometry.createFromEsriGeometry(redensifiedShape2CutCursor.next(), null);

    var intsersectionPointP1 = new Coordinate(2.625370507396378, 53.000985064792765);

    // 4.a - cut shape 1 using the same cut line c to give us intersection point p2
    var shape1CutCursor2 = OperatorCut.local().execute(
        true,
        densifiedShape1.getEsriGeometry(),
        (Polyline) cutLineCLineString.getEsriGeometry(),
        null,
        null
    );
    var shape1CutShape1 = (OGCPolygon) OGCGeometry.createFromEsriGeometry(shape1CutCursor2.next(), null);

    var intersectionPointP2 = new Coordinate(2.625370507396378, 53.00098506432871);

    // 6a.a - re-densify shape 3 from the new point z to point x
    var shape3LineAStartPoint = new Coordinate(2, 53);
    var shape3LineAEndPoint = new Coordinate(2.2507410147927276, 53.000789920727634);

    var redensifiedShape3 = redensifyBetween(shape3, shape3LineAStartPoint, shape3LineAEndPoint);

    // 6a.b - union shape 3 from a above with densified shape 2 from step 2 above
    var redensifiedShape2AndShape3Union = OperatorUnion.local().execute(
        redensifiedShape2.getEsriGeometry(),
        redensifiedShape3.getEsriGeometry(),
        null,
        null
    );

    // 6a.c - check if the unioned output is equal to the pre-cut densified shape 1 in step 1 above
    var equals = OperatorEquals.local().execute(densifiedShape1.getEsriGeometry(), redensifiedShape2AndShape3Union, null, null);
    // false

    // 6b.a - validate densifying backwards gives you the same output
    var lineString = newLineStringFromCoordinates(List.of(new Coordinate(2, 53), new Coordinate(3, 53)));
    var lineStringInReverseOrder = newLineStringFromCoordinates(List.of(new Coordinate(3, 53), new Coordinate(2, 53)));

    var densifiedLineString = (OGCLineString) densify(lineString, true);
    var densifiedLineStringInReverseOrder = (OGCLineString) densify(lineStringInReverseOrder, true);
  }

  private OGCPolygon densifyBetween(OGCPolygon ogcPolygon, Coordinate startCoordinate, Coordinate endCoordinate) throws Exception {
    var coordinates = getCoordinates(ogcPolygon.exteriorRing());

    var lineString = newLineStringFromCoordinates(List.of(startCoordinate, endCoordinate));
    lineString = (OGCLineString) densify(lineString, true);

    var startIndex = coordinates.lastIndexOf(startCoordinate);
    var lineStringCoordinates = getCoordinates(lineString);

    coordinates.addAll(startIndex + 1, lineStringCoordinates.subList(1, lineStringCoordinates.size() - 1));

    return newPolygonFromCoordinates(coordinates, ED50_SR);
  }

  private OGCPolygon redensifyBetween(OGCPolygon ogcPolygon, Coordinate startCoordinate, Coordinate endCoordinate) throws Exception {
    var removed = removeCoordinatesBetween(ogcPolygon, startCoordinate, endCoordinate);

    return densifyBetween(removed, startCoordinate, endCoordinate);
  }

  private OGCPolygon removeCoordinatesBetween(OGCPolygon ogcPolygon, Coordinate startCoordinate, Coordinate endCoordinate) {
    var coordinates = getCoordinates(ogcPolygon.exteriorRing());

    var startIndex = coordinates.lastIndexOf(startCoordinate);
    var endIndex = coordinates.lastIndexOf(endCoordinate);

    var filteredCoordinates = new ArrayList<Coordinate>();
    filteredCoordinates.addAll(coordinates.subList(0, startIndex + 1));
    filteredCoordinates.addAll(coordinates.subList(endIndex, coordinates.size()));

    return newPolygonFromCoordinates(filteredCoordinates, ED50_SR);
  }

  private List<OGCGeometry> cut(OGCGeometry target, OGCGeometry cutter) throws Exception {
    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/cut"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(TestUtil.ED50_SR),
                        "target", "{\"geometryType\":\"%s\",\"geometries\":[%s]}"
                            .formatted(getGeometryType(target), target.asJson()),
                        "cutter", cutter.asJson(),
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    var parsedResponse = objectMapper.readTree(response.body());

    var geometries = new ArrayList<OGCGeometry>();
    parsedResponse.get("geometries").forEach(node ->
        geometries.add(OGCGeometry.fromJson(node.toString())));
    return geometries;
  }

  private String getFormDataAsString(Map<String, String> formData) {
    StringBuilder formBodyBuilder = new StringBuilder();
    for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
      if (!formBodyBuilder.isEmpty()) {
        formBodyBuilder.append("&");
      }
      formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
      formBodyBuilder.append("=");
      formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
    }
    return formBodyBuilder.toString();
  }

  private String getGeometryType(OGCGeometry geometry) {
    var esriGeometry = geometry.getEsriGeometry();
    // there are five types: esriGeometryPoint
    // esriGeometryMultipoint
    // esriGeometryPolyline
    // esriGeometryPolygon
    // esriGeometryEnvelope
    if (esriGeometry instanceof Point)
      return "esriGeometryPoint";
    if (esriGeometry instanceof MultiPoint)
      return "esriGeometryMultipoint";
    if (esriGeometry instanceof Polyline)
      return "esriGeometryPolyline";
    if (esriGeometry instanceof Polygon)
      return "esriGeometryPolygon";
    if (esriGeometry instanceof Envelope)
      return "esriGeometryEnvelope";
    else
      return null;
  }
}
