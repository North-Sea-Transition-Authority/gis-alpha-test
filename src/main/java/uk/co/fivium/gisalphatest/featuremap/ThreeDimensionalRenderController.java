package uk.co.fivium.gisalphatest.featuremap;

import static uk.co.fivium.gisalphatest.feature.LineUtils.getLinesFromFeature;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.FeatureService;
import uk.co.fivium.gisalphatest.feature.Line;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Profile("development")
@Controller
public class ThreeDimensionalRenderController {

  private final FeatureRepository featureRepository;
  private final FeatureService featureService;
  private final GrpcClientService grpcClientService;

  public ThreeDimensionalRenderController(
      FeatureRepository featureRepository, FeatureService featureService,
      GrpcClientService grpcClientService
  ) {
    this.featureRepository = featureRepository;
    this.featureService = featureService;
    this.grpcClientService = grpcClientService;
  }

  @GetMapping("/3d")
  public ModelAndView renderShapes() {
    return new ModelAndView("gis-alpha-test/map/interactiveRenderPage")
        .addObject("shapes", getShapes());
  }

  @GetMapping("/2d")
  public ModelAndView renderStaticShapes() {
    return new ModelAndView("gis-alpha-test/map/staticRenderPage")
        .addObject("shapes", getShapes());
  }

  public List<RenderableShape> getShapes() {
    var shapes = featureRepository.findAllByTestCase("GISA-115")
        .stream()
        .map(feature -> featureService.getEntityBackedFeature(feature))
        .map(entityBackedFeature -> {
              var lines =  getLinesFromFeature(entityBackedFeature);
              var feature = entityBackedFeature.feature();
              var entitypolygon = entityBackedFeature.polygonToLines().keySet().stream().findFirst().orElseThrow();
              var polygon = grpcClientService.buildPolygon(lines.stream().map(Line::getLineJson).toList(), feature.getSrs());

              var startDepth = (entitypolygon.getStartDepth() > 0 ? 0 : entitypolygon.getStartDepth());
              var endDepth = (entitypolygon.getEndDepth() < -10000  ? -10000 : entitypolygon.getEndDepth());
              return new RenderableShape(polygon,  startDepth, endDepth, feature.getFeatureName());
            }
        )
        .toList();

    return shapes;
  }

  public record RenderableShape(
      String esriJsonPolygon,
      Long depthStart,
      Long depthEnd,
      String name
  ){};
}
