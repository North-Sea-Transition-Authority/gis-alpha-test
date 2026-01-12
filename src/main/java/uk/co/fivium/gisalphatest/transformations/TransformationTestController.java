package uk.co.fivium.gisalphatest.transformations;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;
import uk.co.fivium.gisalphatest.oracle.OracleCutLineRepository;

@Controller
public class TransformationTestController {

  private final TransformationService transformationService;
  private final FeatureRepository featureRepository;
  private final GrpcClientService grpcClientService;
  private final OracleCutLineRepository oracleCutLineRepository;

  TransformationTestController(
      TransformationService transformationService,
      FeatureRepository featureRepository,
      GrpcClientService grpcClientService,
      OracleCutLineRepository oracleCutLineRepository) {
    this.transformationService = transformationService;
    this.featureRepository = featureRepository;
    this.grpcClientService = grpcClientService;
    this.oracleCutLineRepository = oracleCutLineRepository;
  }

  @GetMapping("/split")
  public ModelAndView splitPolygon() {
    //make sure to run the migration first
    var migratedPolygon = featureRepository.findAllByShapeSidId(57005318).getFirst();

    String geoJsonSplitLine = oracleCutLineRepository.findByTestCase("Simple split test")
        .get()
        .getCutLineGeojson();

    String esriJsonCutterLine = grpcClientService.convertLineToEsriJson(geoJsonSplitLine);
    var result = transformationService.splitPolygon(migratedPolygon, esriJsonCutterLine);

    System.out.println("Results:");
    result.forEach(System.out::println);

    return new ModelAndView("gis-alpha-test/layout/layout");
  }
}
