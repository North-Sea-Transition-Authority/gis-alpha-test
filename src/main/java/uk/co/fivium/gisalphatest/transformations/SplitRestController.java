package uk.co.fivium.gisalphatest.transformations;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;
import uk.co.fivium.gisalphatest.migration.Srs;

@RestController
@RequestMapping("/api/split")
public class SplitRestController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SplitRestController.class);
  private final GrpcClientService grpcClientService;
  private final SplitService splitService;
  private final FeatureRepository featureRepository;

  public SplitRestController(GrpcClientService grpcClientService,
                             SplitService splitService,
                             FeatureRepository featureRepository) {
    this.grpcClientService = grpcClientService;
    this.splitService = splitService;
    this.featureRepository = featureRepository;
  }

  @PostMapping
  public List<String> splitFromMap(@RequestBody SplitFromMapRequestBody splitFromMapRequestBody) {
    LOGGER.info("Received request for '{}'", splitFromMapRequestBody);
    List<Feature> features = featureRepository.findAllById(splitFromMapRequestBody.featureIds());
    Srs srs = Srs.fromWkid(features.getFirst().getSrs());
    String cutterLine = grpcClientService.convertPointsToPolyline(splitFromMapRequestBody.originalSrsCoordinates(), srs);
    List<String> outputIds = new ArrayList<>();
    for (Feature feature : features) {
      List<Feature> results = splitService.splitPolygon(feature, cutterLine);
      results.forEach(result -> outputIds.add(result.getId().toString()));
    }
    return outputIds;
  }
}
