package uk.co.fivium.gisalphatest.featuremap;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.PolygonRepository;
import uk.co.fivium.gisalphatest.mvc.ReverseRouter;

@Profile("development")
@Controller
public class DepthMapController {

  private final FeatureRepository featureRepository;
  private final PolygonRepository polygonRepository;
  private final ObjectMapper objectMapper;

  public DepthMapController(
      FeatureRepository featureRepository,
      PolygonRepository polygonRepository,
      ObjectMapper objectMapper
  ) {
    this.featureRepository = featureRepository;
    this.polygonRepository = polygonRepository;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/split/{parentFeatureId}")
  public ModelAndView renderDepthMap(@PathVariable("parentFeatureId") UUID parentFeatureId) {
    var parent = featureRepository.findById(parentFeatureId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));

    var children = featureRepository.findAllByParentFeatureId(parentFeatureId);

    var featureIdAndDepths = polygonRepository.findAllByFeatureIn(children)
        .stream()
        .map(polygon -> new FeatureDepthDto(
                polygon.getFeature().getId().toString(),
                polygon.getStartDepth(),
                polygon.getEndDepth()
            )
        )
        .collect(Collectors.toCollection(ArrayList::new));

    featureIdAndDepths.add(new FeatureDepthDto(parent.getId().toString(), null, null));

    return new ModelAndView("gis-alpha-test/map/splitDepthByPointAndClickPage")
        .addObject("srsWkid", parent.getSrs())
        .addObject("featureId", parent.getId().toString())
        .addObject("backUrl", ReverseRouter.route(on(FeatureSelectionController.class).renderSelectFeatures()))
        .addObject("featureIdAndDepthsJson", getFeatureIdAndDepthsJson(featureIdAndDepths));
  }

  public record FeatureDepthDto(
      String featureId,
      Long startDepth,
      Long endDepth
  ) {
  }

  private String getFeatureIdAndDepthsJson(List<FeatureDepthDto> featureIdAndDepths) {
    try {
      return objectMapper.writeValueAsString(featureIdAndDepths);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize feature depth data", exception);
    }
  }
}