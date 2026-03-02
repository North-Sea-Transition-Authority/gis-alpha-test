package uk.co.fivium.gisalphatest.transformations;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Service
public class SplitService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SplitService.class);

  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;
  private final FeatureRepository featureRepository;
  private final TransformationResultProcessingService transformationResultProcessingService;

  public SplitService(PolygonService polygonService,
                      GrpcClientService grpcClientService,
                      FeatureRepository featureRepository,
                      TransformationResultProcessingService transformationResultProcessingService) {
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
    this.featureRepository = featureRepository;
    this.transformationResultProcessingService = transformationResultProcessingService;
  }

  @Transactional
  public List<Feature> splitPolygon(Feature target, String cutterLineEsriJson) {
    var esriJsonPolygon = polygonService.getPolygonsAsEsriJson(target, false).getFirst();

    LOGGER.info("target polygon:");
    LOGGER.info(esriJsonPolygon);

    List<String> resultPolygons = grpcClientService.splitPolygon(esriJsonPolygon, cutterLineEsriJson);

    //test output polygon esrijson
    //19 lines that in reality are 18 (there is an extra node in a line)
    //String testerOutput = "{\"spatialReference\":{\"wkid\":4230},\"rings\":[[[2.46666666666667,53.05],[2.46666666666667,53.06],[2.46666666666667,53.0666666666667],[2.45,53.0666666666667],[2.45,53.075],[2.475,53.075],[2.475,53.1125],[2.48333333333333,53.1125],[2.48333333333333,53.1027777777778],[2.5,53.1027777777778],[2.5,53.0916666666667],[2.51666666666667,53.0916666666667],[2.51666666666667,53.0861111111111],[2.52222222222222,53.0861111111111],[2.52222222222222,53.0805555555556],[2.51666666666667,53.0805555555556],[2.51666666666667,53.0666666666667],[2.5,53.0666666666667],[2.5,53.05],[2.46666666666667,53.05]]]}";

    List<Feature> resultFeatures = new ArrayList<>();
    resultPolygons.forEach(polygon -> {
      var newFeature = transformationResultProcessingService.processOutputPolygon(List.of(target), polygon);
      resultFeatures.add(newFeature);
    });

    return resultFeatures;
  }
}
