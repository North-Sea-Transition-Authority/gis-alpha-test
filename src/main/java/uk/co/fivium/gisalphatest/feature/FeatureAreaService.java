package uk.co.fivium.gisalphatest.feature;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;
import uk.co.fivium.gisalphatest.migration.Srs;

@Service
public class FeatureAreaService {

  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;
  private final FeatureRepository featureRepository;

  public FeatureAreaService(PolygonService polygonService,
                            GrpcClientService grpcClientService,
                            FeatureRepository featureRepository) {
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
    this.featureRepository = featureRepository;
  }

  @Transactional
  public void calculateFeatureArea(Feature newFeature) {
    var isOnshore = Srs.BNG.getWkid().equals(newFeature.getSrs());

    // We don't need to densify loxodrome lines that are onshore as there is no earth curvature taken into account.
    List<String> polygonsAsEsriJson = polygonService.getPolygonsAsEsriJson(newFeature, !isOnshore);
    String combinedPolygon = grpcClientService.unionPolygons(polygonsAsEsriJson);
    double areaProcessed = Math.abs(grpcClientService.calculatePolygonArea(combinedPolygon, isOnshore));
    newFeature.setFeatureArea(BigDecimal.valueOf(areaProcessed));
    featureRepository.save(newFeature);
  }

  @Transactional
  public void calculateAreaDifference(Feature feature,
                                      double oracleArea) {
    // bigger number means the new area is bigger than the oracle area
    var difference = feature.getFeatureArea().subtract(BigDecimal.valueOf(oracleArea));
    feature.setAreaDifference(difference);
    featureRepository.save(feature);
  }
}
