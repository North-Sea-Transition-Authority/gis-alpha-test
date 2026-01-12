package uk.co.fivium.gisalphatest.grpc;

import com.example.grpc.ArcGisServiceGrpc;
import com.example.grpc.BuildPolygonRequest;
import com.example.grpc.EsriJsonPolygon;
import com.example.grpc.EsriJsonResponse;
import com.example.grpc.GeoJsonRequest;
import com.example.grpc.SplitPolygonRequest;
import com.example.grpc.SplitPolygonResponse;
import java.util.List;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

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
}