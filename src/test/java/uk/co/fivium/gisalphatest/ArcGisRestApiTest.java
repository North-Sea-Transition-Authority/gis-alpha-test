package uk.co.fivium.gisalphatest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.gisalphatest.util.MathUtil.roundDecimalPlaces;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_OFFSHORE_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_ONSHORE_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_OFFSHORE_SR;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_ONSHORE_SR;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCLineString;
import com.esri.core.geometry.ogc.OGCPolygon;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.fivium.gisalphatest.util.Coordinate;

class ArcGisRestApiTest {

  private static final SpatialReference OFFSHORE_SR = SpatialReference.create(ORACLE_OFFSHORE_SR);
  private static final SpatialReference ONSHORE_SR = SpatialReference.create(ORACLE_ONSHORE_SR);

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

    inputLineString.setSpatialReference(OFFSHORE_SR);
    expectedOutputLineString.setSpatialReference(OFFSHORE_SR);

    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/densify"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(ORACLE_OFFSHORE_SR),
                        "geometries", "{\"geometryType\":\"%s\",\"geometries\":[%s]}"
                            .formatted(getGeometryType(inputLineString), inputLineString.asJson()),
                        "maxSegmentLength", String.valueOf(roundDecimalPlaces(20.0 / 3600, 11)),
                        "geodesic", "false",
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    var parsedResponse = objectMapper.readTree(response.body());
    var densifiedLineString = (OGCLineString) OGCGeometry.fromJson(parsedResponse.get("geometries").get(0).toString());

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

    var simplifiedLineString = (OGCLineString) simplify(inputLineString);

    assertThat(getRoundedCoordinates(simplifiedLineString, 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 11));
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
                        "sr", String.valueOf(ORACLE_OFFSHORE_SR),
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
  void area_offshore() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area/input-offshore-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);

    inputPolygon.setSpatialReference(OFFSHORE_SR);

    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/areasAndLengths"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(ORACLE_OFFSHORE_SR),
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
        .isEqualTo(roundDecimalPlaces(ORACLE_AREA_CALCULATION_OFFSHORE_POLYGON_AREA_KM2, 4));
  }

  @Test
  void area_onshore() throws Exception {
    var inputPolygonGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area/input-onshore-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygonGeoJson);

    inputPolygon.setSpatialReference(ONSHORE_SR);

    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/areasAndLengths"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(ORACLE_ONSHORE_SR),
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

    assertThat(roundDecimalPlaces(parsedResponse.get("areas").get(0).asDouble() / 1000000, 11))
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

    var inputPolygon1 = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygon1GeoJson);
    var inputPolygon2 = (OGCPolygon) OGCGeometry.fromGeoJson(inputPolygon2GeoJson);
    var expectedOutputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputPolygonGeoJson);

    inputPolygon1.setSpatialReference(OFFSHORE_SR);
    inputPolygon2.setSpatialReference(OFFSHORE_SR);
    expectedOutputPolygon.setSpatialReference(OFFSHORE_SR);

    var unionPolygon = (OGCPolygon) union(inputPolygon1, inputPolygon2);

    assertThat(getRoundedCoordinates(unionPolygon.exteriorRing(), 9))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputPolygon.exteriorRing(), 9));
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

    inputLineString1.setSpatialReference(OFFSHORE_SR);
    inputLineString2.setSpatialReference(OFFSHORE_SR);
    expectedOutputLineString.setSpatialReference(OFFSHORE_SR);

    var unionLineString = (OGCLineString) union(inputLineString1, inputLineString2);

    assertThat(getRoundedCoordinates(unionLineString, 9))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 9));
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
                        "sr", String.valueOf(ORACLE_OFFSHORE_SR),
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

    inputPolygon1.setSpatialReference(OFFSHORE_SR);
    inputPolygon2.setSpatialReference(OFFSHORE_SR);
    expectedOutputPolygon.setSpatialReference(OFFSHORE_SR);

    var intersectionPolygon = (OGCPolygon) intersect(inputPolygon1, inputPolygon2);

    var simplifiedExpectedOutputPolygon = (OGCPolygon) simplify(expectedOutputPolygon);

    assertThat(getRoundedCoordinates(intersectionPolygon.exteriorRing(), 5))
        .containsExactlyElementsOf(getRoundedCoordinates(simplifiedExpectedOutputPolygon.exteriorRing(), 5));
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

    inputLineString1.setSpatialReference(OFFSHORE_SR);
    inputLineString2.setSpatialReference(OFFSHORE_SR);
    expectedOutputLineString.setSpatialReference(OFFSHORE_SR);

    var intersectionLineString = (OGCLineString) intersect(inputLineString1, inputLineString2);

    assertThat(getRoundedCoordinates(intersectionLineString, 5))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 5));
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
                        "sr", String.valueOf(ORACLE_OFFSHORE_SR),
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
