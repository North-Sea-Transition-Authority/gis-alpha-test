package uk.co.fivium.gisalphatest.transformations;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Service
public class TransformationService {

  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;

  public TransformationService(PolygonService polygonService,
                               GrpcClientService grpcClientService) {
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
  }

  List<String> splitPolygon(Feature target, String cutterLineEsriJson) {
    var esriJsonPolygon = polygonService.getPolygonsAsEsriJson(target.getShapeSidId(), target.getTestCase()).getFirst();

    System.out.println("target polygon:");
    System.out.println(esriJsonPolygon);

    return grpcClientService.splitPolygon(esriJsonPolygon, cutterLineEsriJson);
  }
}
