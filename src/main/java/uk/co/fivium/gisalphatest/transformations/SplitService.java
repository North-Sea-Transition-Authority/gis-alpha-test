package uk.co.fivium.gisalphatest.transformations;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;
import uk.co.fivium.gisalphatest.migration.MigrationService;
import uk.co.fivium.gisalphatest.oracle.OracleCutLineRepository;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Service
public class SplitService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SplitService.class);

  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;
  private final FeatureRepository featureRepository;
  private final MigrationService migrationService;
  private final OracleCutLineRepository oracleCutLineRepository;
  private final TransformationResultProcessingService transformationResultProcessingService;

  public SplitService(PolygonService polygonService,
                      GrpcClientService grpcClientService,
                      FeatureRepository featureRepository,
                      MigrationService migrationService,
                      OracleCutLineRepository oracleCutLineRepository,
                      TransformationResultProcessingService transformationResultProcessingService) {
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
    this.featureRepository = featureRepository;
    this.migrationService = migrationService;
    this.oracleCutLineRepository = oracleCutLineRepository;
    this.transformationResultProcessingService = transformationResultProcessingService;
  }

  @Transactional
  public void testOracleShapeSplit(OracleShapeCompositeKey oracleShapeTarget,
                                   List<OracleShapeCompositeKey> oracleExpectedShapeResults,
                                   String oracleCutLineTestCase) {
    var polygonsToMigrate = new ArrayList<>(oracleExpectedShapeResults);
    polygonsToMigrate.add(oracleShapeTarget);
    migrationService.migrate(polygonsToMigrate);
    migrationService.migrateFeatureAreas();

    var migratedTargetPolygon = featureRepository.findAllByShapeSidId(oracleShapeTarget.getShapeSidId()).getFirst();
    polygonService.getPolygonsAsEsriJson(migratedTargetPolygon, false).forEach(System.out::println);

    String geoJsonSplitLine = oracleCutLineRepository.findByTestCase(oracleCutLineTestCase)
        .get()
        .getCutLineGeojson();
    String esriJsonCutterLine = grpcClientService.convertCutLineToEsriJson(geoJsonSplitLine, migratedTargetPolygon.getSrs());

    var resultFeatures = splitPolygon(migratedTargetPolygon, esriJsonCutterLine);

    validateAreaAgainstOracleExpected(resultFeatures, oracleExpectedShapeResults);

    LOGGER.info("Results:");
    resultFeatures.forEach(feature -> LOGGER.info("feature: {}", feature.getId()));

    resultFeatures.forEach(feature -> {
      polygonService.getPolygonsAsEsriJson(feature, false).forEach(System.out::println);
    });

  }

  private void validateAreaAgainstOracleExpected(List<Feature> resultFeatures,
                                                 List<OracleShapeCompositeKey> oracleExpectedShapeResults) {
    List<Integer> oracleExpectedShapesSsid = oracleExpectedShapeResults.stream()
        .map(OracleShapeCompositeKey::getShapeSidId)
        .toList();
    List<Feature> oracleExpectedFeatures = featureRepository.findAllByShapeSidIdIn(oracleExpectedShapesSsid)
        .stream()
        .sorted(Comparator.comparing(Feature::getFeatureArea))
        .toList();
    resultFeatures.sort(Comparator.comparing(Feature::getFeatureArea));

    if (resultFeatures.size() != oracleExpectedShapesSsid.size()) {
      throw new IllegalStateException("resultFeatures.size() (%s) != oracleExpectedShapesSsid.size() (%s)".formatted(resultFeatures.size(), oracleExpectedShapesSsid.size()));
    }

    for (int i = 0; i < resultFeatures.size(); i++) {
      Feature oracleExpectedFeature = oracleExpectedFeatures.get(i);
      Feature resultFeature = resultFeatures.get(i);
      BigDecimal difference = resultFeature.getFeatureArea().subtract(oracleExpectedFeature.getFeatureArea());
      resultFeature.setAreaDifference(difference);

      if (difference.abs().compareTo(BigDecimal.valueOf(20)) > 0) {
        LOGGER.error("Feature {} has an area difference bigger than 20m^2 compared to its oracle counterpart", resultFeature.getId());
      }
    }

    featureRepository.saveAll(resultFeatures);
  }

  @Transactional
  public List<Feature> splitPolygon(Feature target, String cutterLineEsriJson) {
    var esriJsonPolygon = polygonService.getPolygonsAsEsriJson(target.getShapeSidId(), target.getTestCase(), false).getFirst();

    LOGGER.info("target polygon:");
    LOGGER.info(esriJsonPolygon);

    List<String> resultPolygons = grpcClientService.splitPolygon(esriJsonPolygon, cutterLineEsriJson);

    //test output polygon esrijson
    //19 lines that in reality are 18 (there is an extra node in a line)
    //String testerOutput = "{\"spatialReference\":{\"wkid\":4230},\"rings\":[[[2.46666666666667,53.05],[2.46666666666667,53.06],[2.46666666666667,53.0666666666667],[2.45,53.0666666666667],[2.45,53.075],[2.475,53.075],[2.475,53.1125],[2.48333333333333,53.1125],[2.48333333333333,53.1027777777778],[2.5,53.1027777777778],[2.5,53.0916666666667],[2.51666666666667,53.0916666666667],[2.51666666666667,53.0861111111111],[2.52222222222222,53.0861111111111],[2.52222222222222,53.0805555555556],[2.51666666666667,53.0805555555556],[2.51666666666667,53.0666666666667],[2.5,53.0666666666667],[2.5,53.05],[2.46666666666667,53.05]]]}";

    List<Feature> resultFeatures = new ArrayList<>();
    resultPolygons.forEach(polygon -> {
      var newFeature = transformationResultProcessingService.processOutputPolygon(List.of(target), polygon);
      resultFeatures.add(newFeature);
    });

    return resultFeatures;
  }
}
