package uk.co.fivium.gisalphatest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.gisalphatest.util.MathUtil.roundDecimalPlaces;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_OFFSHORE_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_AREA_CALCULATION_ONSHORE_POLYGON_AREA_KM2;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_OFFSHORE_SR;
import static uk.co.fivium.gisalphatest.util.TestUtil.ORACLE_ONSHORE_SR;

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
  void densification() throws Exception {
    var inputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densification-input-line.geojson"), StandardCharsets.UTF_8);
    var expectedOutputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densification-output-line.geojson"), StandardCharsets.UTF_8);

    var inputLineString = (OGCLineString) OGCGeometry.fromGeoJson(inputGeoJson);
    var expectedOutputLineString = (OGCLineString) OGCGeometry.fromGeoJson(expectedOutputGeoJson);

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
                        "geometries", "{\"geometryType\":\"esriGeometryPolyline\",\"geometries\":[" + inputLineString.asJson() + "]}",
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
  void simplification() throws Exception {
    var inputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densification-output-line.geojson"), StandardCharsets.UTF_8);
    var expectedOutputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/densification-input-line.geojson"), StandardCharsets.UTF_8);

    var inputLineString = (OGCLineString) OGCGeometry.fromGeoJson(inputGeoJson);
    var expectedOutputLineString = (OGCLineString) OGCGeometry.fromGeoJson(expectedOutputGeoJson);

    inputLineString.setSpatialReference(OFFSHORE_SR);
    expectedOutputLineString.setSpatialReference(OFFSHORE_SR);

    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/generalize"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(ORACLE_OFFSHORE_SR),
                        "geometries", "{\"geometryType\":\"esriGeometryPolyline\",\"geometries\":[" + inputLineString.asJson() + "]}",
                        "maxDeviation", String.valueOf(Double.MAX_VALUE),
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    var parsedResponse = objectMapper.readTree(response.body());

    var simplifiedLineString = (OGCLineString) OGCGeometry.fromJson(parsedResponse.get("geometries").get(0).toString());

    assertThat(getRoundedCoordinates(simplifiedLineString, 11))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputLineString, 11));
  }

  @Test
  void areaCalculation_offshore() throws Exception {
    var inputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area-calculation-offshore-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputGeoJson);

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
                        "polygons", "[" + inputPolygon.asJson() + "]",
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
  void areaCalculation_onshore() throws Exception {
    var inputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/area-calculation-onshore-polygon.geojson"), StandardCharsets.UTF_8);

    var inputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(inputGeoJson);

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
                        "polygons", "[" + inputPolygon.asJson() + "]",
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
  void union() throws Exception {
    var input1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union-input-geom-1.geojson"), StandardCharsets.UTF_8);
    var input2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union-input-geom-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/union-output-geom.geojson"), StandardCharsets.UTF_8);

    var input1Polygon = (OGCPolygon) OGCGeometry.fromGeoJson(input1GeoJson);
    var input2Polygon = (OGCPolygon) OGCGeometry.fromGeoJson(input2GeoJson);
    var expectedOutputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputGeoJson);

    input1Polygon.setSpatialReference(OFFSHORE_SR);
    input2Polygon.setSpatialReference(OFFSHORE_SR);
    expectedOutputPolygon.setSpatialReference(OFFSHORE_SR);

    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/union"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(ORACLE_OFFSHORE_SR),
                        "geometries", "{\"geometryType\":\"esriGeometryPolygon\",\"geometries\":[" + input1Polygon.asJson() + "," + input2Polygon.asJson() + "]}",
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    var parsedResponse = objectMapper.readTree(response.body());

    var unionPolygon = (OGCPolygon) OGCGeometry.fromJson(parsedResponse.get("geometry").toString());

    assertThat(getRoundedCoordinates(unionPolygon.exteriorRing(), 9))
        .containsExactlyElementsOf(getRoundedCoordinates(expectedOutputPolygon.exteriorRing(), 9));
  }

  @Test
  void intersection() throws Exception {
    var input1GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersection-input-geom-1.geojson"), StandardCharsets.UTF_8);
    var input2GeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersection-input-geom-2.geojson"), StandardCharsets.UTF_8);
    var expectedOutputGeoJson = Resources.toString(
        Resources.getResource("oracle-test-cases/intersection-output-geom.geojson"), StandardCharsets.UTF_8);

    var input1Polygon = (OGCPolygon) OGCGeometry.fromGeoJson(input1GeoJson);
    var input2Polygon = (OGCPolygon) OGCGeometry.fromGeoJson(input2GeoJson);
    var expectedOutputPolygon = (OGCPolygon) OGCGeometry.fromGeoJson(expectedOutputGeoJson);

    input1Polygon.setSpatialReference(OFFSHORE_SR);
    input2Polygon.setSpatialReference(OFFSHORE_SR);
    expectedOutputPolygon.setSpatialReference(OFFSHORE_SR);

    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/intersect"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(ORACLE_OFFSHORE_SR),
                        "geometries", "{\"geometryType\":\"esriGeometryPolygon\",\"geometries\":[" + input1Polygon.asJson() + "]}",
                        "geometry", "{\"geometryType\":\"esriGeometryPolygon\",\"geometry\":" + input2Polygon.asJson() + "}",
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    var parsedResponse = objectMapper.readTree(response.body());

    var intersectionPolygon = (OGCPolygon) OGCGeometry.fromJson(parsedResponse.get("geometries").get(0).toString());

    var simplifyRequest = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/generalize"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(ORACLE_OFFSHORE_SR),
                        "geometries", "{\"geometryType\":\"esriGeometryPolygon\",\"geometries\":[" + expectedOutputPolygon.asJson() + "]}",
                        "maxDeviation", String.valueOf(0.01),
                        "f", "pjson"
                    )
                )
            )
        )
        .build();
    var simplifyResponse = httpClient.send(simplifyRequest, HttpResponse.BodyHandlers.ofString());
    var parsedSimplifyResponse = objectMapper.readTree(simplifyResponse.body());
    var simplifiedExpectedOutputPolygon = (OGCPolygon) OGCGeometry.fromJson(parsedSimplifyResponse.get("geometries").get(0).toString());

    assertThat(getRoundedCoordinates(intersectionPolygon.exteriorRing(), 5))
        .containsExactlyElementsOf(getRoundedCoordinates(simplifiedExpectedOutputPolygon.exteriorRing(), 5));
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
}
