package uk.co.fivium.gisalphatest.feature;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;
import uk.co.fivium.gisalphatest.migration.Srs;
import uk.co.fivium.gisalphatest.oracle.OracleService;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Service
public class FeatureAreaService {

  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;
  private final FeatureRepository featureRepository;
  private final OracleService oracleService;

  public FeatureAreaService(PolygonService polygonService,
                            GrpcClientService grpcClientService,
                            FeatureRepository featureRepository,
                            OracleService oracleService) {
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
    this.featureRepository = featureRepository;
    this.oracleService = oracleService;
  }

  @Transactional
  public void calculateFeatureArea(Feature newFeature) {
    List<String> polygonsAsEsriJson = polygonService.getPolygonsAsEsriJson(newFeature, true);
    String combinedPolygon = grpcClientService.unionPolygons(polygonsAsEsriJson);
    double areaProcessed = Math.abs(grpcClientService.calculatePolygonArea(combinedPolygon, Srs.BNG.getValue().equals(newFeature.getSrs())));
    newFeature.setFeatureArea(BigDecimal.valueOf(areaProcessed));
    featureRepository.save(newFeature);
  }

  @Transactional
  public void calculateAreaDifference(Feature feature,
                                      OracleShapeCompositeKey oracleIdToCompareAgainst) {
    var oracleArea = oracleService.getOracleShapeArea(oracleIdToCompareAgainst);
    // bigger number means the new area is bigger than the oracle area
    var difference = feature.getFeatureArea().subtract(BigDecimal.valueOf(oracleArea));
    feature.setAreaDifference(difference);
    featureRepository.save(feature);
  }
}
