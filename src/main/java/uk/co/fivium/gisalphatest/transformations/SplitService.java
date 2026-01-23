package uk.co.fivium.gisalphatest.transformations;

import com.esri.core.geometry.Point;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.Line;
import uk.co.fivium.gisalphatest.feature.LineNavigationType;
import uk.co.fivium.gisalphatest.feature.LineRepository;
import uk.co.fivium.gisalphatest.feature.Polygon;
import uk.co.fivium.gisalphatest.feature.PolygonRepository;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Service
public class SplitService {

  public static final String AFTER_SPLIT = "_afterSplit";
  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;
  private final PolygonRepository polygonRepository;
  private final LineRepository lineRepository;
  private final FeatureRepository featureRepository;

  public SplitService(PolygonService polygonService,
                      GrpcClientService grpcClientService,
                      PolygonRepository polygonRepository,
                      LineRepository lineRepository,
                      FeatureRepository featureRepository) {
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
    this.polygonRepository = polygonRepository;
    this.lineRepository = lineRepository;
    this.featureRepository = featureRepository;
  }

  @Transactional
  public List<String> splitPolygon(Feature target, String cutterLineEsriJson) {
    var esriJsonPolygon = polygonService.getPolygonsAsEsriJson(target.getShapeSidId(), target.getTestCase(), false).getFirst();

    System.out.println("target polygon:");
    System.out.println(esriJsonPolygon);

    List<String> resultPolygons = grpcClientService.splitPolygon(esriJsonPolygon, cutterLineEsriJson);

    //test output polygon esrijson
    //19 lines that in reality are 18 (there is an extra node in a line)
    //String testerOutput = "{\"spatialReference\":{\"wkid\":4230},\"rings\":[[[2.46666666666667,53.05],[2.46666666666667,53.06],[2.46666666666667,53.0666666666667],[2.45,53.0666666666667],[2.45,53.075],[2.475,53.075],[2.475,53.1125],[2.48333333333333,53.1125],[2.48333333333333,53.1027777777778],[2.5,53.1027777777778],[2.5,53.0916666666667],[2.51666666666667,53.0916666666667],[2.51666666666667,53.0861111111111],[2.52222222222222,53.0861111111111],[2.52222222222222,53.0805555555556],[2.51666666666667,53.0805555555556],[2.51666666666667,53.0666666666667],[2.5,53.0666666666667],[2.5,53.05],[2.46666666666667,53.05]]]}";

    resultPolygons.forEach(polygon -> processOutputPolygon(target, polygon));

    return resultPolygons;
  }

  private void processOutputPolygon(Feature target,
                                    String outputPolygon) {
    var inputPolygons = polygonRepository.findAllByFeature(target);
    var inputPolygonLines = lineRepository.findAllByPolygonIn(inputPolygons);

    List<Line> newLineEntities = getLinesForOutputPolygon(outputPolygon, inputPolygonLines);
    numberLines(newLineEntities);
    validateLinesAreValid(newLineEntities, outputPolygon);
    copyParentEntityAttributes(target, inputPolygons, newLineEntities);
    addLineIds(newLineEntities);
    lineRepository.saveAll(newLineEntities);

  }

  private List<Line> getLinesForOutputPolygon(String outputPolygon,
                                              List<Line> inputPolygonLines) {
    List<String> explodedPolygonLines = grpcClientService.explodePolygon(outputPolygon);
    var findParentLineResponse = grpcClientService.findParentLine(inputPolygonLines, explodedPolygonLines);
    Map<Integer, Line> idToParentLine = inputPolygonLines.stream()
        .collect(Collectors.toMap(Line::getId, Function.identity()));

    List<Line> newLineEntities = findParentLineResponse.polylineToParentLineId()
        .entrySet()
        .stream()
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
    return newLineEntities;
  }

  private void numberLines(List<Line> unorderedLines) {
    LinkedList<LineWrapper> pool = new LinkedList<>();

    //Java version
    //unorderedLines.forEach(line -> pool.add(LineWrapper.fromEntity(line)));

    //node server version
    pool.addAll(grpcClientService.getLineStartAndEndpoints(unorderedLines));

    int ringNumberCounter = 0;
    while (!pool.isEmpty()) {
      int ringConnectionOrderCounter = 1;
      LineWrapper current = pool.removeFirst();

      current.line().setRingNumber(ringNumberCounter);
      current.line().setRingConnectionOrder(ringConnectionOrderCounter);

      var isRingClosed = false;
      while(!isRingClosed && !pool.isEmpty()) {
        Point targetStart = current.end();
        Optional<LineWrapper> nextLine = findNextLine(pool, targetStart);

        if (nextLine.isPresent()) {
          current = nextLine.get();
          pool.remove(current);
          ringConnectionOrderCounter++;
          current.line().setRingNumber(ringNumberCounter);
          current.line().setRingConnectionOrder(ringConnectionOrderCounter);
        } else {
          isRingClosed = true;
        }
      }
      ringNumberCounter++;
    }
  }

  private Optional<LineWrapper> findNextLine(LinkedList<LineWrapper> pool,
                                             Point targetStart) {
    return pool.stream()
        .filter(lineWrapper -> lineWrapper.start().getXY().equals(targetStart.getXY()))
        .findFirst();
  }

  private void validateLinesAreValid(List<Line> newLineEntities, String outputPolygonEsriJson) {
    boolean linesAreValid = grpcClientService.validatePolygonReconstruction(newLineEntities, outputPolygonEsriJson);
    if (!linesAreValid) {
      throw new IllegalStateException("Cannot generate valid polygon from processed lines");
    }
  }

  private void copyParentEntityAttributes(Feature target,
                                          List<Polygon> inputPolygons,
                                          List<Line> newLineEntities) {
    var newFeature = new Feature();
    newFeature.setType(target.getType());
    newFeature.setFeatureName(target.getFeatureName() + AFTER_SPLIT);
    newFeature.setSrs(target.getSrs());
    newFeature.setTestCase(target.getTestCase() + AFTER_SPLIT);
    featureRepository.save(newFeature);

    var newPolygon = new Polygon();
    newPolygon.setFeature(newFeature);
    if (inputPolygons.size() == 1) {
      //TODO GISA-73 handle splits with multiple input polygons
      newPolygon.setAttributes(inputPolygons.getFirst().getAttributes());
    }

    //TODO GISA-86 update postgres entities to use UUIDs as IDs rather than oracle Integers
    var polygonId = polygonRepository.findMaxId();
    newPolygon.setId(++polygonId);
    polygonRepository.save(newPolygon);

    newLineEntities.forEach(line -> line.setPolygon(newPolygon));
  }

  //TODO GISA-86 update postgres entities to use UUIDs as IDs rather than oracle Integers
  private void addLineIds(List<Line> newLineEntities) {
    var currentId = lineRepository.findMaxId();
    for (Line line : newLineEntities) {
      line.setId(++currentId);
    }
  }
}
