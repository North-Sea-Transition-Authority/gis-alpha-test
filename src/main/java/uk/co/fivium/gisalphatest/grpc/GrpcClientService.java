package uk.co.fivium.gisalphatest.grpc;

import com.example.grpc.ArcGisServiceGrpc;
import com.example.grpc.BuildPolygonRequest;
import com.example.grpc.EsriJsonResponse;
import com.example.grpc.GeoJsonRequest;
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
   * @return An EsriJSON polygon in string format
   */
  public String convertLineToEsriJson(String geoJson) {
    GeoJsonRequest request = GeoJsonRequest.newBuilder()
        .setGeoJsonString(geoJson)
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
}