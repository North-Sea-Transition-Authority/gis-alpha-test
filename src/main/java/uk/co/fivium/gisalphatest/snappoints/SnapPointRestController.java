package uk.co.fivium.gisalphatest.snappoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.co.fivium.gisalphatest.featuremap.SnapPointRequestParam;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;
import uk.co.fivium.gisalphatest.migration.Srs;

@RestController
public class SnapPointRestController {

  private final GrpcClientService grpcClientService;
  private final ObjectMapper objectMapper;

  public SnapPointRestController(GrpcClientService grpcClientService,
                                 ObjectMapper objectMapper) {
    this.grpcClientService = grpcClientService;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/api/snap-points")
  Map<String, Object> getSnapPoints(@RequestBody SnapPointRequestParam snapPointRequestParam) throws JsonProcessingException {
    var snapPoints = grpcClientService.getSnapPoints(
        Srs.fromWkid(snapPointRequestParam.srsWkid()),
        snapPointRequestParam.maxLat().doubleValue(),
        snapPointRequestParam.minLon().doubleValue(),
        snapPointRequestParam.minLat().doubleValue(),
        snapPointRequestParam.maxLon().doubleValue()
    );

    List<Map<String, Object>> pointsList = new ArrayList<>();
    for (var point : snapPoints) {
      var wgs84CoordsMap = objectMapper.readValue(point.wgs84Coordinates(), Map.class);
      var originalSrsCoordsMap = objectMapper.readValue(point.originalSrsCoordinates(), Map.class);

      Map<String, Object> pointMap = new HashMap<>();
      pointMap.put("id", point.id());
      pointMap.put("coordinates", List.of(wgs84CoordsMap.get("x"), wgs84CoordsMap.get("y")));
      pointMap.put("originalSrsCoordinates", List.of(originalSrsCoordsMap.get("x"), originalSrsCoordsMap.get("y")));
      pointsList.add(pointMap);
    }

    return Map.of("points", pointsList);
  }
}
