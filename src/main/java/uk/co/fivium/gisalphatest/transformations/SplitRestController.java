package uk.co.fivium.gisalphatest.transformations;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

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
  public ResponseEntity<String> splitFromMap(@RequestBody SplitFromMapRequestBody splitFromMapRequestBody) {
    LOGGER.info("Received request for '{}'", splitFromMapRequestBody);
    var cutterLine = grpcClientService.convertPointsToEd50Polyline(splitFromMapRequestBody.ed50lineCoordinates());
    List<Feature> features = featureRepository.findAllById(splitFromMapRequestBody.featureIds());
    for (Feature feature : features) {
      splitService.splitPolygon(feature, cutterLine);
    }
    return ResponseEntity.ok().build();
  }
}
