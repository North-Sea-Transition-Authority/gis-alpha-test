package uk.co.fivium.gisalphatest.featuremap;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.migration.Srs;
import uk.co.fivium.gisalphatest.mvc.ReverseRouter;

@Controller
@RequestMapping("/map")
class MapController {

  private final FeatureRepository featureRepository;
  private final PolygonService polygonService;
  private final ObjectMapper objectMapper;

  MapController(
      FeatureRepository featureRepository,
      PolygonService polygonService,
      ObjectMapper objectMapper) {
    this.featureRepository = featureRepository;
    this.polygonService = polygonService;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/split/point-and-click")
  public ModelAndView getFeatureMap(@RequestParam(required = false) List<UUID> featureIds) {
    if (CollectionUtils.isEmpty(featureIds)) {
      featureIds = featureRepository.findAllByActive(true).stream()
          .filter(feature -> Srs.ED50.getWkid().equals(feature.getSrs()))
          .map(Feature::getId)
          .toList();
    }

    List<String> idsAsString = featureIds.stream()
        .map(UUID::toString)
        .toList();
    var feature = getFeatureOrThrow(featureIds.getFirst());
    return new ModelAndView("gis-alpha-test/map/splitByPointAndClickPage")
        .addObject("featureIds", idsAsString)
        .addObject("srsWkid", feature.getSrs())
        .addObject("journeyId", getJourneyId(feature))
        .addObject("backUrl", ReverseRouter.route(on(FeatureSelectionController.class).renderSelectFeatures()));
  }

  @GetMapping("/split/coordinate-entry")
  public ModelAndView getFeatureMapWithCoordinateEntry(@RequestParam(required = false) List<UUID> featureIds) {
    if (CollectionUtils.isEmpty(featureIds)) {
      featureIds = featureRepository.findAllByActive(true).stream()
          .filter(feature -> Srs.ED50.getWkid().equals(feature.getSrs()))
          .map(Feature::getId)
          .toList();
    }

    List<String> idsAsString = featureIds.stream()
        .map(UUID::toString)
        .toList();
    var feature = getFeatureOrThrow(featureIds.getFirst());
    return new ModelAndView("gis-alpha-test/map/splitByCoordinateEntryPage")
        .addObject("featureIds", idsAsString)
        .addObject("srsWkid", feature.getSrs())
        .addObject("journeyId", getJourneyId(feature))
        .addObject("backUrl", ReverseRouter.route(on(FeatureSelectionController.class).renderSelectFeatures()))
        .addObject("userTestingExtentText", getUserTestingShapeExtentText(feature));
  }

  @GetMapping("/esrijson")
  @ResponseBody
  public Map<String, Object> getFeaturesEsriJson(@RequestParam List<UUID> featureIds) throws JsonProcessingException {
    var features = featureRepository.findAllById(featureIds);

    List<Map<String, Object>> geoJsonFeatures = new ArrayList<>();

    for (var feature : features) {
      var esriJsonPolygons = polygonService.getPolygonsAsEsriJson(feature, false);
      var projectedEsriJsonPolygons = polygonService.getPolygonsAsEsriJsonProjected(esriJsonPolygons);

      for (var esriJsonPolygon : projectedEsriJsonPolygons) {

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("featureId", feature.getId().toString());
        attributes.put("featureName", feature.getFeatureName());

        Map<String, Object> esriJsonFeature = new HashMap<>();
        esriJsonFeature.put("geometry", objectMapper.readValue(esriJsonPolygon, Map.class));
        esriJsonFeature.put("attributes", attributes);

        geoJsonFeatures.add(esriJsonFeature);
      }
    }

    Map<String, Object> featureSet = new HashMap<>();
    featureSet.put("features", geoJsonFeatures);

    return featureSet;
  }

  private Feature getFeatureOrThrow(UUID featureId) {
    return featureRepository.findById(featureId)
        .orElseThrow(EntityNotFoundException::new);
  }

  private String getJourneyId(Feature feature) {
    String journeyId = feature.getCreatedByCommand() == null
        ? null
        : feature.getCreatedByCommand().getCommandJourney().getId().toString();
    return journeyId;
  }

  private String getUserTestingShapeExtentText(Feature feature) {
    if (feature.getFeatureName().contains("49/28a")) {
      return "The feature is located in the region between: 53°57'0\"N, 2°48'0\"E and 53°50'0\"N, 2°54'30\"E";
    } else if (feature.getFeatureName().contains("110/8b")) {
      return "The feature is located in the region between: 53°50'0\"N, 3°36'0\"W and 53°40'0\"N, 3°24'0\"W";
    } else {
      return null;
    }
  }
}
