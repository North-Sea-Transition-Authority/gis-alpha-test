package uk.co.fivium.gisalphatest.featuremap;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.FeatureService;
import uk.co.fivium.gisalphatest.feature.Polygon;
import uk.co.fivium.gisalphatest.feature.PolygonRepository;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.mvc.ReverseRouter;
import uk.co.fivium.gisalphatest.transformations.command.CommandJourneyService;

@Controller
@RequestMapping("/map")
class MapController {

  private final PolygonService polygonService;
  private final ObjectMapper objectMapper;
  private final CommandJourneyService commandJourneyService;
  private final PolygonRepository polygonRepository;
  private final FeatureService featureService;
  private final FeatureRepository featureRepository;

  MapController(
      PolygonService polygonService,
      ObjectMapper objectMapper,
      CommandJourneyService commandJourneyService, PolygonRepository polygonRepository, FeatureService featureService,
      FeatureRepository featureRepository) {
    this.polygonService = polygonService;
    this.objectMapper = objectMapper;
    this.commandJourneyService = commandJourneyService;
    this.polygonRepository = polygonRepository;
    this.featureService = featureService;
    this.featureRepository = featureRepository;
  }

  @GetMapping("/split/point-and-click/{commandJourneyId}")
  public ModelAndView getFeatureMap(@PathVariable UUID commandJourneyId) {
    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(commandJourneyId);
    var feature = commandJourneyService.getActiveFeatures(commandJourney).getFirst();
    return new ModelAndView("gis-alpha-test/map/splitByPointAndClickPage")
        .addObject("journeyId", commandJourneyId)
        .addObject("srsWkid", feature.getSrs())
        .addObject("backUrl", ReverseRouter.route(on(FeatureSelectionController.class).renderSelectFeatures()));
  }

  @GetMapping("/split/coordinate-entry/{commandJourneyId}")
  public ModelAndView getFeatureMapWithCoordinateEntry(@PathVariable UUID commandJourneyId) {
    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(commandJourneyId);
    var feature = commandJourneyService.getActiveFeatures(commandJourney).getFirst();
    return new ModelAndView("gis-alpha-test/map/splitByCoordinateEntryPage")
        .addObject("journeyId", commandJourneyId)
        .addObject("srsWkid", feature.getSrs())
        .addObject("backUrl", ReverseRouter.route(on(FeatureSelectionController.class).renderSelectFeatures()))
        .addObject("userTestingExtentText", getUserTestingShapeExtentText(feature));
  }

  @GetMapping("/esrijson/{commandJourneyId}")
  @ResponseBody
  public Map<String, Object> getFeaturesEsriJson(@PathVariable UUID commandJourneyId) throws JsonProcessingException {
    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(commandJourneyId);
    var features = commandJourneyService.getActiveFeatures(commandJourney);

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

  @GetMapping("/block-and-subareas/esrijson/{parentId}")
  @ResponseBody
  public Map<String, Object> getBlockAndSubareaEsriJson(@PathVariable UUID parentId) throws JsonProcessingException {
    var parent = featureRepository.findById(parentId).orElseThrow();
    var children = featureRepository.findAllByParentFeatureId(parentId);

    var features = new ArrayList<Feature>(children);
    features.add(parent);

    List<Map<String, Object>> geoJsonFeatures = new ArrayList<>();

    for (var feature : features) {
      var polygons = polygonRepository.findAllByFeature(feature);
      for (Polygon polygon : polygons) {
        var esriJsonPolygons = polygonService.getPolygonAsEsriJson(polygon, feature.getSrs(), false);
        var projectedEsriJsonPolygons = polygonService.getPolygonsAsEsriJsonProjected(List.of(esriJsonPolygons));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("featureId", feature.getId().toString());
        attributes.put("featureName", feature.getFeatureName());
        attributes.put("startDepth", polygon.getStartDepth());
        attributes.put("endDepth", polygon.getEndDepth());

        Map<String, Object> esriJsonFeature = new HashMap<>();
        esriJsonFeature.put("geometry", objectMapper.readValue(projectedEsriJsonPolygons.getFirst(), Map.class));
        esriJsonFeature.put("attributes", attributes);

        geoJsonFeatures.add(esriJsonFeature);
      }
    }

    Map<String, Object> featureSet = new HashMap<>();
    featureSet.put("features", geoJsonFeatures);

    return featureSet;
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
