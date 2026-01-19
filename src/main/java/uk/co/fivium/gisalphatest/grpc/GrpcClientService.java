package uk.co.fivium.gisalphatest.grpc;

import com.example.grpc.ArcGisServiceGrpc;
import com.example.grpc.BuildPolygonRequest;
import com.example.grpc.CalculateAreaResponse;
import com.example.grpc.CalculatePolygonAreaRequest;
import com.example.grpc.DensifyLoxodromeLineRequest;
import com.example.grpc.DensifyLoxodromeLineResponse;
import com.example.grpc.EsriJsonPolygon;
import com.example.grpc.EsriJsonResponse;
import com.example.grpc.ExplodePolygonRequest;
import com.example.grpc.ExplodePolygonResponse;
import com.example.grpc.FindParentLineRequest;
import com.example.grpc.GeoJsonRequest;
import com.example.grpc.LineParent;
import com.example.grpc.ReconstructedLine;
import com.example.grpc.SplitPolygonRequest;
import com.example.grpc.SplitPolygonResponse;
import com.example.grpc.UnionPolygonsRequest;
import com.example.grpc.UnionPolygonsResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.feature.Line;

@Service
public class GrpcClientService {

  @GrpcClient("node-server")
  private ArcGisServiceGrpc.ArcGisServiceBlockingStub arcgisClient;

  /**
   * Converts a GeoJSON line to an EsriJSON polyline.
   * @param geoJson A GeoJSON of type "LineString" in string format
   * @param wkid the spatial reference Well Known ID of the geojson. (E.g. the number version of "ED 50")
   * @param isGeodesic true if the input GeoJSON represents a geodesic line.
   * @return An EsriJSON polyline in string format
   */
  public String convertLineToEsriJson(
      String geoJson,
      Integer wkid,
      boolean isGeodesic
  ) {
    GeoJsonRequest request = GeoJsonRequest.newBuilder()
        .setGeoJsonString(geoJson)
        .setWkid(wkid)
        .setIsGeodesic(isGeodesic)
        .build();

    EsriJsonResponse response = arcgisClient.convertGeoJsonLineToEsriJsonLine(request);
    return response.getEsriJsonString();
  }

  /**
   * Takes a list of polylines and combines them into a polygon using the arcgis js sdk
   *
   * @param polylines An ordered list of Esri JSON polylines
   * @param srs The id of the coordinate system
   * @return EsriJSON of a polygon as a string.
   */
  public String buildPolygon(List<String> polylines, Integer srs) {
    var request = BuildPolygonRequest.newBuilder()
        .addAllEsriJsonLineStrings(polylines)
        .setSrs(srs)
        .build();

    EsriJsonResponse response = arcgisClient.buildPolygon(request);
    return response.getEsriJsonString();
  }

  /**
   * Densifies an EsriJSON loxodrome polyline
   * @param polyline an EsriJSON loxodrome polyline
   * @return a densified EsriJSON polyline
   */
  public String densifyLoxodromePolyline(String polyline) {
    var request = DensifyLoxodromeLineRequest.newBuilder()
        .setEsriJsonPolyline(polyline)
        .build();

    DensifyLoxodromeLineResponse response = arcgisClient.densifyLoxodromePolyline(request);
    return response.getEsriJsonPolyline();
  }

  /**
   * Takes a list of EsriJSON polygons and combines time into a single shape.
   * @param polygons a list of EsriJSON polygons
   * @return an EsriJSON polygon or "shape" that is made up of all the inputted polygons.
   */
  public String unionPolygons(List<String> polygons) {
    var request = UnionPolygonsRequest.newBuilder()
        .addAllEsriJsonPolygons(polygons)
        .build();
    UnionPolygonsResponse response = arcgisClient.unionPolygons(request);
    return response.getEsriJsonPolygon();
  }

  /**
   * Takes an EsriJSON polygon and returns the area
   * @param esriPolygon an EsriJSON polygon
   * @return the area in metres squared
   */
  public double calculatePolygonArea(String esriPolygon) {
    var request = CalculatePolygonAreaRequest.newBuilder()
        .setEsriJsonPolygon(esriPolygon)
        .build();
    CalculateAreaResponse response = arcgisClient.calculatePolygonArea(request);
    return response.getArea();
  }

  public List<String> splitPolygon(String polygon, String cutter) {
    var request = SplitPolygonRequest.newBuilder()
        .setTarget(EsriJsonPolygon.newBuilder().setEsriJsonPolygon(polygon))
        .setEsriJsonCutter(cutter)
        .build();

    SplitPolygonResponse response = arcgisClient.splitPolygon(request);
    return response.getPolygonsList()
        .stream()
        .map(EsriJsonPolygon::getEsriJsonPolygon)
        .toList();
  }

  /**
   * Create a polyline for each coordinate pair in the polygon.
   * @param polygon Input polygon
   * @return A list of esriJson polylines for every coordinate pair of the input. These can be union together to reconstruct
   * the original input.
   */
  public List<String> explodePolygon(String polygon) {
    var request = ExplodePolygonRequest.newBuilder()
        .setTarget(EsriJsonPolygon.newBuilder().setEsriJsonPolygon(polygon))
        .build();
    ExplodePolygonResponse response = arcgisClient.explodePolygon(request);
    return response.getEsriJsonLinesList();
  }

  /**
   * Find which lines are contained by other lines
   * @param parentLines Line entities that will contain the contained lines
   * @param containedLines Child lines that are contained by the parentLines
   * @return A map of contained lines esri json to the id of the parent line that contains it.
   */
  public FindParentLineResponse findParentLine(List<Line> parentLines, List<String> containedLines) {
    var requestBuilder = FindParentLineRequest.newBuilder();

    for (Line line : parentLines) {
      requestBuilder.addParents(LineParent.newBuilder()
          .setId(line.getId())
          .setEsriJsonPolyline(line.getLineJson())
          .build());
    }
    for (String polyline : containedLines) {
      requestBuilder.addChildren(polyline);
    }

    var response = arcgisClient.findParentLine(requestBuilder.build());

    Map<String, Integer> polylineToParentLineId = new HashMap<>();

    for (ReconstructedLine line : response.getLinesList()) {
      polylineToParentLineId.put(line.getEsriJsonPolyline(), line.getParentId());
    }

    return new FindParentLineResponse(polylineToParentLineId, response.getOrphanedChildrenJsonList());
  }
}