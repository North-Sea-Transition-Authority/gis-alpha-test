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
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.FeatureType;
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
      featureIds = featureRepository.findAll().stream()
          .filter(feature -> Srs.ED50.getWkid().equals(feature.getSrs()))
          .map(Feature::getId)
          .toList();
    }

    List<String> idsAsString = featureIds.stream()
        .map(UUID::toString)
        .toList();
    int srsWkid = featureRepository.findById(featureIds.getFirst()).map(Feature::getSrs).orElse(Srs.ED50.getWkid());
    return new ModelAndView("gis-alpha-test/map/splitByPointAndClickPage")
        .addObject("featureIds", idsAsString)
        .addObject("srsWkid", srsWkid)
        .addObject("backUrl", ReverseRouter.route(on(FeatureSelectionController.class).renderSelectFeatures()))
        .addObject("coordinateEntryMapUrl",
            ReverseRouter.route(on(MapController.class).getFeatureMapWithCoordinateEntry(featureIds)));
  }

  @GetMapping("/split/coordinate-entry")
  public ModelAndView getFeatureMapWithCoordinateEntry(@RequestParam(required = false) List<UUID> featureIds) {
    if (CollectionUtils.isEmpty(featureIds)) {
      featureIds = featureRepository.findAll().stream()
          .filter(feature -> Srs.ED50.getWkid().equals(feature.getSrs()))
          .map(Feature::getId)
          .toList();
    }

    List<String> idsAsString = featureIds.stream()
        .map(UUID::toString)
        .toList();
    int srsWkid = featureRepository.findById(featureIds.getFirst()).map(Feature::getSrs).orElse(Srs.ED50.getWkid());
    return new ModelAndView("gis-alpha-test/map/splitByCoordinateEntryPage")
        .addObject("featureIds", idsAsString)
        .addObject("srsWkid", srsWkid)
        .addObject("backUrl", ReverseRouter.route(on(FeatureSelectionController.class).renderSelectFeatures()))
        .addObject("pointAndClickMapUrl",
            ReverseRouter.route(on(MapController.class).getFeatureMap(featureIds)));
  }

  @GetMapping("/esrijson")
  @ResponseBody
  public Map<String, Object> getFeaturesEsriJson(@RequestParam List<UUID> featureIds) throws JsonProcessingException {
    var features = featureRepository.findAllById(featureIds);

    List<Map<String, Object>> geoJsonFeatures = new ArrayList<>();

    for (var feature : features) {
      if (feature.getType() != FeatureType.POLYGON && feature.getType() != FeatureType.POLYGON_COLLECTION) {
        continue;
      }


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
}
