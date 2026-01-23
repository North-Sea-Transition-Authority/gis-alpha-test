package uk.co.fivium.gisalphatest.transformations;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;
import uk.co.fivium.gisalphatest.migration.MigrationService;
import uk.co.fivium.gisalphatest.oracle.OracleCutLineRepository;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Controller
public class SplitTestController {

  private final SplitService splitService;
  private final FeatureRepository featureRepository;
  private final GrpcClientService grpcClientService;
  private final OracleCutLineRepository oracleCutLineRepository;
  private final MigrationService migrationService;

  SplitTestController(
      SplitService splitService,
      FeatureRepository featureRepository,
      GrpcClientService grpcClientService,
      OracleCutLineRepository oracleCutLineRepository,
      MigrationService migrationService) {
    this.splitService = splitService;
    this.featureRepository = featureRepository;
    this.grpcClientService = grpcClientService;
    this.oracleCutLineRepository = oracleCutLineRepository;
    this.migrationService = migrationService;
  }

  @GetMapping("/split")
  public ModelAndView splitPolygon() {
    migrationService.migrate(
        List.of(
            new OracleShapeCompositeKey(57005318, "Simple split test")
        )
    );
    var migratedPolygon = featureRepository.findAllByShapeSidId(57005318).getFirst();

    String geoJsonSplitLine = oracleCutLineRepository.findByTestCase("Simple split test")
        .get()
        .getCutLineGeojson();

    String esriJsonCutterLine = grpcClientService.convertLineToEsriJson(geoJsonSplitLine, migratedPolygon.getSrs(), false);
    var result = splitService.splitPolygon(migratedPolygon, esriJsonCutterLine);

    System.out.println("Results:");
    result.forEach(System.out::println);

    return new ModelAndView("gis-alpha-test/layout/layout");
  }
}
