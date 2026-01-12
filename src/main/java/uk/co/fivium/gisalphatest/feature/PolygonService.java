package uk.co.fivium.gisalphatest.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Service
public class PolygonService {

  private final  FeatureService featureService;
  private final GrpcClientService grpcClientService;

  public PolygonService(
      FeatureService featureService,
      GrpcClientService grpcClientService
  ) {
    this.featureService = featureService;

    this.grpcClientService = grpcClientService;
  }

  public List<String> getPolygonsAsEsriJson(Integer shapeSidId, String testCase) {
    List<String> polygonsAsEsriJson = new ArrayList<>();

    for (var entityBackedFeature: featureService.getEntityBackedFeatures(shapeSidId, testCase)) {
      var srs = entityBackedFeature.feature().getSrs();
      for (var entry: entityBackedFeature.polygonToLines().entrySet()) {
        var lineJsons = entry.getValue()
            .stream()
            .sorted(Comparator.comparing(Line::getRingConnectionOrder))
            .map(Line::getLineJson)
            .toList();
        polygonsAsEsriJson.add(grpcClientService.buildPolygon(lineJsons, srs));
      }
    }

    return polygonsAsEsriJson;
  }
}
