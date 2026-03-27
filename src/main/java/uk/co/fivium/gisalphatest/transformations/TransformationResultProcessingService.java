package uk.co.fivium.gisalphatest.transformations;

import com.esri.core.geometry.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureAreaService;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.FeatureType;
import uk.co.fivium.gisalphatest.feature.Line;
import uk.co.fivium.gisalphatest.feature.LineNavigationType;
import uk.co.fivium.gisalphatest.feature.LineRepository;
import uk.co.fivium.gisalphatest.feature.Polygon;
import uk.co.fivium.gisalphatest.feature.PolygonRepository;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Service
public class TransformationResultProcessingService {

  public static final String AFTER_SPLIT = "_afterSplit";

  private final PolygonRepository polygonRepository;
  private final LineRepository lineRepository;
  private final FeatureAreaService featureAreaService;
  private final GrpcClientService grpcClientService;
  private final FeatureRepository featureRepository;

  public TransformationResultProcessingService(PolygonRepository polygonRepository,
                                               LineRepository lineRepository,
                                               FeatureAreaService featureAreaService,
                                               GrpcClientService grpcClientService,
                                               FeatureRepository featureRepository) {
    this.polygonRepository = polygonRepository;
    this.lineRepository = lineRepository;
    this.featureAreaService = featureAreaService;
    this.grpcClientService = grpcClientService;
    this.featureRepository = featureRepository;
  }

  @Transactional
  public Feature processOutputPolygon(List<Feature> targets,
                                      String outputPolygon) {
    var inputPolygons = targets.stream()
        .flatMap(target -> polygonRepository.findAllByFeature(target).stream())
        .toList();
    var inputPolygonLines = lineRepository.findAllByPolygonIn(inputPolygons);

    List<Line> newLineEntities = getLinesForOutputPolygon(outputPolygon, inputPolygonLines);
    numberLines(newLineEntities);
    validateLinesAreValid(newLineEntities, outputPolygon);
    var newFeature = copyParentEntityAttributes(targets, inputPolygons, newLineEntities);
    lineRepository.saveAll(newLineEntities);
    featureAreaService.calculateFeatureArea(newFeature);
    return newFeature;
  }

  private List<Line> getLinesForOutputPolygon(String outputPolygon,
                                              List<Line> inputPolygonLines) {
    List<String> explodedPolygonLines = grpcClientService.explodePolygon(outputPolygon);
    var findParentLineResponse = grpcClientService.findParentLine(inputPolygonLines, explodedPolygonLines);
    Map<UUID, Line> idToParentLine = inputPolygonLines.stream()
        .collect(Collectors.toMap(Line::getId, Function.identity()));
    boolean isOnshore = inputPolygonLines.stream()
        .anyMatch(line -> LineNavigationType.CARTESIAN.equals(line.getNavigationType()));

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
      newLineEntity.setNavigationType(isOnshore ? LineNavigationType.CARTESIAN : LineNavigationType.LOXODROME);
      newLineEntities.add(newLineEntity);
    });
    return newLineEntities;
  }

  public void numberLines(List<Line> unorderedLines) {
    LinkedList<LineWrapper> pool = new LinkedList<>();

    //Java version
    //unorderedLines.forEach(line -> pool.add(LineWrapper.fromEntity(line)));

    //node server version
    List<LineWrapper> allLineWrappers = grpcClientService.getLineStartAndEndpoints(unorderedLines);
    pool.addAll(allLineWrappers);

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
      rotateRingToStartAtNorthwestmostPoint(allLineWrappers, ringNumberCounter);
      ringNumberCounter++;
    }
  }

  private Optional<LineWrapper> findNextLine(LinkedList<LineWrapper> pool,
                                             Point targetStart) {
    return pool.stream()
        .filter(lineWrapper -> lineWrapper.start().getXY().equals(targetStart.getXY()))
        .findFirst();
  }

  void rotateRingToStartAtNorthwestmostPoint(List<LineWrapper> lineWrappers, int ringNumber) {
    List<LineWrapper> ringLines = lineWrappers.stream()
        .filter(lw -> lw.line().getRingNumber() != null && lw.line().getRingNumber() == ringNumber)
        .toList();
    if (ringLines.isEmpty()) {
      return;
    }

    //lines haven't been persisted yet
    Map<UUID, Line> tempIdToLine = new HashMap<>();
    Map<UUID, LineWrapper> tempIdToLineWrapper = new HashMap<>();
    ringLines.forEach(lineWrapper -> {
      var tempId = UUID.randomUUID();
      tempIdToLine.put(tempId, lineWrapper.line());
      tempIdToLineWrapper.put(tempId, lineWrapper);
    });

    LineWrapper startLine = tempIdToLineWrapper.get(grpcClientService.findNorthwestmostLine(tempIdToLine));

    int startLineIndex = startLine.line().getRingConnectionOrder();
    if (startLineIndex == 1) {
      // Already starts at the correct line
      return;
    }

    // Rotate the ring connection order so the NW-most line becomes #1
    // This is a circular rotation that "cuts" at startLineIndex and moves it to the front
    // Example: if startLineIndex=3 and there are 4 lines total: [1,2,3,4] -> [3,4,1,2]
    ringLines.forEach(lineWrapper -> {
      int oldIndex = lineWrapper.line().getRingConnectionOrder();
      int newOrder;

      if (oldIndex >= startLineIndex) {
        newOrder = oldIndex - startLineIndex + 1;
      } else {
        newOrder = ringLines.size() - startLineIndex + 1 + oldIndex;
      }

      lineWrapper.line().setRingConnectionOrder(newOrder);
    });
  }

  public void validateLinesAreValid(List<Line> newLineEntities, String outputPolygonEsriJson) {
    boolean linesAreValid = grpcClientService.validatePolygonReconstruction(newLineEntities, outputPolygonEsriJson);
    if (!linesAreValid) {
      throw new IllegalStateException("Cannot generate valid polygon from processed lines");
    }
  }

  private Feature copyParentEntityAttributes(List<Feature> targets,
                                             List<Polygon> inputPolygons,
                                             List<Line> newLineEntities) {
    var newFeature = new Feature();
    if (targets.size() == 1) {
      var target = targets.getFirst();
      newFeature.setType(target.getType());
      newFeature.setFeatureName(target.getFeatureName() + AFTER_SPLIT);
      newFeature.setSrs(target.getSrs());
      newFeature.setTestCase(target.getTestCase() + AFTER_SPLIT);
    } else {
      String afterMerge = "after merge";
      newFeature.setSrs(targets.getFirst().getSrs());
      newFeature.setFeatureName(afterMerge);
      newFeature.setTestCase(afterMerge);
      newFeature.setType(FeatureType.POLYGON);
    }
    featureRepository.save(newFeature);

    var newPolygon = new Polygon();
    newPolygon.setFeature(newFeature);
    if (inputPolygons.size() == 1) {
      //TODO GISA-73 handle splits with multiple input polygons
      newPolygon.setAttributes(inputPolygons.getFirst().getAttributes());
    } else {
      //used when merging 2 polygons, we dont cascade parent attributes. Might need to update when GISA-73 is done.
      newPolygon.setAttributes(new HashMap<>());
    }

    polygonRepository.save(newPolygon);
    newLineEntities.forEach(line -> line.setPolygon(newPolygon));

    return newFeature;
  }
}
