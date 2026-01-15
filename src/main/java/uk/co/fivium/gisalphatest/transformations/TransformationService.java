package uk.co.fivium.gisalphatest.transformations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.Line;
import uk.co.fivium.gisalphatest.feature.LineNavigationType;
import uk.co.fivium.gisalphatest.feature.LineRepository;
import uk.co.fivium.gisalphatest.feature.PolygonRepository;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Service
public class TransformationService {

  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;
  private final PolygonRepository polygonRepository;
  private final LineRepository lineRepository;

  public TransformationService(PolygonService polygonService,
                               GrpcClientService grpcClientService,
                               PolygonRepository polygonRepository,
                               LineRepository lineRepository) {
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
    this.polygonRepository = polygonRepository;
    this.lineRepository = lineRepository;
  }

  List<String> splitPolygon(Feature target,
                            String cutterLineEsriJson) {
    var esriJsonPolygon = polygonService.getPolygonsAsEsriJson(target.getShapeSidId(), target.getTestCase()).getFirst();

    System.out.println("target polygon:");
    System.out.println(esriJsonPolygon);

    List<String> resultPolygons = grpcClientService.splitPolygon(esriJsonPolygon, cutterLineEsriJson);

    //test output polygon esrijson
    //19 lines that in reality are 18 (there is an extra node in a line)
    //String testerOutput = "{\"spatialReference\":{\"wkid\":4230},\"rings\":[[[2.46666666666667,53.05],[2.46666666666667,53.06],[2.46666666666667,53.0666666666667],[2.45,53.0666666666667],[2.45,53.075],[2.475,53.075],[2.475,53.1125],[2.48333333333333,53.1125],[2.48333333333333,53.1027777777778],[2.5,53.1027777777778],[2.5,53.0916666666667],[2.51666666666667,53.0916666666667],[2.51666666666667,53.0861111111111],[2.52222222222222,53.0861111111111],[2.52222222222222,53.0805555555556],[2.51666666666667,53.0805555555556],[2.51666666666667,53.0666666666667],[2.5,53.0666666666667],[2.5,53.05],[2.46666666666667,53.05]]]}";

    resultPolygons.forEach(polygon -> constructLines(target, polygon));

    return resultPolygons;
  }

  private void constructLines(Feature target,
                              String resultPolygon) {
    var polygons = polygonRepository.findAllByFeature(target);
    var lines = lineRepository.findAllByPolygonIn(polygons);
    List<String> explodedPolygonLines = grpcClientService.explodePolygon(resultPolygon);

    var findParentLineResponse = grpcClientService.findParentLine(lines, explodedPolygonLines);

    Map<Integer, Line> idToParentLine = lines.stream().collect(Collectors.toMap(Line::getId, Function.identity()));

    List<Line> newLineEntities = findParentLineResponse.polylineToParentLineId().entrySet().stream()
        .map(polylineToParentId -> {
          Line parentLine = idToParentLine.get(polylineToParentId.getValue());
          var newLineEntity = new Line();
          newLineEntity.setLineJson(polylineToParentId.getKey());
          newLineEntity.setAttributes(parentLine.getAttributes());
          newLineEntity.setNavigationType(parentLine.getNavigationType());
          return newLineEntity;
        })
        .collect(Collectors.toCollection(ArrayList::new));

    findParentLineResponse.orphanLines().forEach(polyline -> {
      var newLineEntity = new Line();
      newLineEntity.setLineJson(polyline);
      newLineEntity.setAttributes(new HashMap<>());
      newLineEntity.setNavigationType(LineNavigationType.LOXODROME);
      newLineEntities.add(newLineEntity);
    });
  }
}
