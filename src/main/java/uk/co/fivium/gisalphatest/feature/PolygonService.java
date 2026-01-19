package uk.co.fivium.gisalphatest.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Service
public class PolygonService {

  private final FeatureService featureService;
  private final GrpcClientService grpcClientService;

  public PolygonService(
      FeatureService featureService,
      GrpcClientService grpcClientService
  ) {
    this.featureService = featureService;

    this.grpcClientService = grpcClientService;
  }

  /**
   * Generates all the EsriJSON polygons for a given feature.
   *
   * @param shapeSidId the shapeSidId of the feature
   * @param testCase the testcase of the feature
   * @param densifyLoxodromeLines true if the loxodrome lines on the feature should be densified before the polygon is built.
   * @return a list of EsriJSON polygons for the given feature
   */
  public List<String> getPolygonsAsEsriJson(Integer shapeSidId, String testCase, boolean densifyLoxodromeLines) {
    List<String> polygonsAsEsriJson = new ArrayList<>();

    var entityBackedFeature = featureService.getEntityBackedFeature(shapeSidId, testCase);
    var srs = entityBackedFeature.feature().getSrs();

    for (var entry : entityBackedFeature.polygonToLines().entrySet()) {
      var lineJsons = entry.getValue()
          .stream()
          .sorted(Comparator.comparing(Line::getRingConnectionOrder))
          .map(line -> {
            if (densifyLoxodromeLines && LineNavigationType.LOXODROME.equals(line.getNavigationType())) {
              return grpcClientService.densifyLoxodromePolyline(line.getLineJson());
            }
            return line.getLineJson();
          })
          .toList();
      polygonsAsEsriJson.add(grpcClientService.buildPolygon(lineJsons, srs));

    }

    return polygonsAsEsriJson;
  }
}
