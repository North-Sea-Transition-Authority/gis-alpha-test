package uk.co.fivium.gisalphatest.featuremap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Controller
@RequestMapping("/map")
class MapController {

  private static final Logger logger = LoggerFactory.getLogger(MapController.class);

  private final FeatureRepository featureRepository;
  private final PolygonService polygonService;
  private final ObjectMapper objectMapper;
  private final GrpcClientService grpcClientService;

  MapController(
      FeatureRepository featureRepository,
      PolygonService polygonService,
      ObjectMapper objectMapper,
      GrpcClientService grpcClientService) {
    this.featureRepository = featureRepository;
    this.polygonService = polygonService;
    this.objectMapper = objectMapper;
    this.grpcClientService = grpcClientService;
  }

  @GetMapping
  public ModelAndView getFeatureMap(@RequestParam(required = false) List<UUID> featureIds) {
    if (CollectionUtils.isEmpty(featureIds)) {
      featureIds = featureRepository.findAll().stream()
          .map(Feature::getId)
          .toList();
    }

    List<String> idsAsString = featureIds.stream()
        .map(UUID::toString)
        .toList();
    return new ModelAndView("gis-alpha-test/map/map")
        .addObject("featureIds", idsAsString);
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
