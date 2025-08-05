package uk.co.fivium.gisalphatest.arcgis;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCLineString;
import com.esri.core.geometry.ogc.OGCPolygon;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.util.MathUtil;

@Service
public class ArcGisService {

  private static final int GEODESIC_DENSIFY_MAX_SEGMENT_LENGTH_METERS = 100;
  // When densifying with geodesic set to false, the maxSegmentLength unit is derived from the SR.
  // For ED50, the unit is degrees (see https://epsg.io/4230).
  private static final double PLANAR_DENSIFY_MAX_SEGMENT_LENGTH_DEGREES = MathUtil.roundDecimalPlaces(20.0 / 3600, 11);

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public OGCLineString densifyLine(OGCLineString line, boolean geodesic) throws Exception {
    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/densify"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(line.getEsriSpatialReference().getID()),
                        "geometries", "{\"geometryType\":\"%s\",\"geometries\":[%s]}"
                            .formatted(getGeometryType(line), line.asJson()),
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
    return (OGCLineString) OGCGeometry.fromJson(parsedResponse.get("geometries").get(0).toString());
  }

  public List<OGCPolygon> cutPolygon(OGCPolygon target, OGCLineString cutter) throws Exception {
    var request = HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .uri(URI.create("https://data.nstauthority.co.uk/arcgis/rest/services/Utilities/Geometry/GeometryServer/cut"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(
            HttpRequest.BodyPublishers.ofString(
                getFormDataAsString(
                    Map.of(
                        "sr", String.valueOf(target.getEsriSpatialReference().getID()),
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

    var geometries = new ArrayList<OGCPolygon>();
    parsedResponse.get("geometries").forEach(node ->
        geometries.add((OGCPolygon) OGCGeometry.fromJson(node.toString())));
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
