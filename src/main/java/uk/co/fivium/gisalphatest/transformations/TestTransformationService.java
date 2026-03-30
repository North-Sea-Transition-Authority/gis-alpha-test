package uk.co.fivium.gisalphatest.transformations;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.FeatureService;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;
import uk.co.fivium.gisalphatest.migration.MigrationService;
import uk.co.fivium.gisalphatest.oracle.OracleCutLineRepository;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Service
@Profile("development")
public class TestTransformationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestTransformationService.class);

  private final MigrationService migrationService;
  private final OracleCutLineRepository oracleCutLineRepository;

  private final FeatureRepository featureRepository;
  private final PolygonService polygonService;

  private final SplitService splitService;
  private final MergeService mergeService;

  private final GrpcClientService grpcClientService;
  private final FeatureService featureService;

  public TestTransformationService(
      MigrationService migrationService,
      FeatureRepository featureRepository, PolygonService polygonService,
      SplitService splitService,
      GrpcClientService grpcClientService,
      OracleCutLineRepository oracleCutLineRepository, MergeService mergeService,
      FeatureService featureService) {
    this.migrationService = migrationService;
    this.featureRepository = featureRepository;
    this.polygonService = polygonService;
    this.splitService = splitService;
    this.grpcClientService = grpcClientService;
    this.oracleCutLineRepository = oracleCutLineRepository;
    this.mergeService = mergeService;
    this.featureService = featureService;
  }

  @Transactional
  public void testOracleShapeSplit(OracleShapeCompositeKey oracleShapeTarget,
                                   List<OracleShapeCompositeKey> oracleExpectedShapeResults,
                                   String oracleCutLineTestCase) {
    var polygonsToMigrate = new ArrayList<>(oracleExpectedShapeResults);
    polygonsToMigrate.add(oracleShapeTarget);
    migrationService.migrate(polygonsToMigrate, true);
    migrationService.migrateFeatureAreas();

    var migratedTargetPolygon = featureRepository.findAllByShapeSidId(oracleShapeTarget.getShapeSidId()).getFirst();
    polygonService.getPolygonsAsEsriJson(migratedTargetPolygon, false).forEach(System.out::println);

    String geoJsonSplitLine = oracleCutLineRepository.findByTestCase(oracleCutLineTestCase)
        .get()
        .getCutLineGeojson();
    String esriJsonCutterLine = grpcClientService.convertCutLineToEsriJson(geoJsonSplitLine, migratedTargetPolygon.getSrs());

    var resultFeatures = splitService.splitPolygon(migratedTargetPolygon, esriJsonCutterLine);

    validateAreaAgainstOracleExpected(resultFeatures, oracleExpectedShapeResults);

    LOGGER.info("Results:");
    resultFeatures.forEach(feature -> LOGGER.info("feature: {}", feature.getId()));

    resultFeatures.forEach(feature -> {
      polygonService.getPolygonsAsEsriJson(feature, false).forEach(System.out::println);
    });

  }


  @Transactional
  public void testMultipleOracleShapeSplit(List<OracleShapeCompositeKey> oracleShapeTargets,
                                          String oracleCutLineTestCase) {
    migrationService.migrate(oracleShapeTargets, true);
    migrationService.migrateFeatureAreas();

    var migratedTargetPolygons = featureRepository.findAll();

    String geoJsonSplitLine = oracleCutLineRepository.findByTestCase(oracleCutLineTestCase)
        .get()
        .getCutLineGeojson();
    String esriJsonCutterLine = grpcClientService.convertCutLineToEsriJson(geoJsonSplitLine, migratedTargetPolygons.getFirst().getSrs());

    List<Feature> resultFeatures = new ArrayList<>();

    migratedTargetPolygons.forEach(feature -> resultFeatures.addAll(splitService.splitPolygon(feature, esriJsonCutterLine)));
  }


  @Transactional
  public void testOracleMerge(List<OracleShapeCompositeKey> oracleShapeInputs,
                              OracleShapeCompositeKey oracleExpectedShapeResult) {
    var polygonsToMigrate = new ArrayList<>(oracleShapeInputs);
    polygonsToMigrate.add(oracleExpectedShapeResult);
    migrationService.migrate(polygonsToMigrate, true);
    migrationService.migrateFeatureAreas();

    Feature featureInput1 = featureRepository.findAllByShapeSidId(oracleShapeInputs.getFirst().getShapeSidId()).getFirst();
    Feature featureInput2 = featureRepository.findAllByShapeSidId(oracleShapeInputs.getLast().getShapeSidId()).getFirst();

    Feature result = mergeService.mergePolygons(featureInput1, featureInput2);

    LOGGER.info("Results:");
    LOGGER.info("Feature id: {} {}",result.getId(), polygonService.getPolygonsAsEsriJson(result, false).getFirst());
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
}
