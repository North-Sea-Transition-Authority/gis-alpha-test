package uk.co.fivium.gisalphatest.featuremap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.FeatureType;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Controller
@RequestMapping("/feature-map")
class FeatureMapController {

  private static final Logger logger = LoggerFactory.getLogger(FeatureMapController.class);

  private final FeatureRepository featureRepository;
  private final PolygonService polygonService;
  private final ObjectMapper objectMapper;
  private final GrpcClientService grpcClientService;

  FeatureMapController(
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
  public ModelAndView getFeatureMap() {
    return new ModelAndView("gis-alpha-test/featuremap/featureMap");
  }

  @GetMapping("/esrijson")
  @ResponseBody
  public Map<String, Object> getEsriJson() throws JsonProcessingException {
    var features = featureRepository.findAll();

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
