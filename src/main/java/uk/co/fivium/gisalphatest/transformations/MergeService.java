package uk.co.fivium.gisalphatest.transformations;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.OperatorImportFromJson;
import com.esri.core.geometry.Point2D;
import com.esri.core.geometry.Polyline;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.Line;
import uk.co.fivium.gisalphatest.feature.LineRepository;
import uk.co.fivium.gisalphatest.feature.Polygon;
import uk.co.fivium.gisalphatest.feature.PolygonRepository;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Service
public class MergeService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MergeService.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;
  private final TransformationResultProcessingService transformationResultProcessingService;
  private final FeatureRepository featureRepository;
  private final PolygonRepository polygonRepository;
  private final LineRepository lineRepository;

  public MergeService(PolygonService polygonService,
                      GrpcClientService grpcClientService,
                      TransformationResultProcessingService transformationResultProcessingService,
                      FeatureRepository featureRepository,
                      PolygonRepository polygonRepository,
                      LineRepository lineRepository) {
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
    this.transformationResultProcessingService = transformationResultProcessingService;
    this.featureRepository = featureRepository;
    this.polygonRepository = polygonRepository;
    this.lineRepository = lineRepository;
  }


  @Transactional
  public Feature mergePolygons(Feature featureInput1, Feature featureInput2) {
    String esriJsonPolygonInput1 = polygonService.getPolygonsAsEsriJson(featureInput1, false).getFirst();
    String esriJsonPolygonInput2 = polygonService.getPolygonsAsEsriJson(featureInput2, false).getFirst();

    LOGGER.info("Input polygons:");
    LOGGER.info(esriJsonPolygonInput1);
    LOGGER.info(esriJsonPolygonInput2);

    String resultEsriPolygon = grpcClientService.mergePolygons(esriJsonPolygonInput1, esriJsonPolygonInput2);
    var newFeature = transformationResultProcessingService.processOutputPolygon(List.of(featureInput1, featureInput2), resultEsriPolygon);
    removeInnerVertices(resultEsriPolygon, newFeature);

    return newFeature;
  }

  /**
   * After a merge, the output polygon may contain collinear vertices that were boundaries
   * between input lines from different parents. If two adjacent lines at such a vertex share
   * the same attributes, merge them into a single line entity (removing the redundant vertex).
   * If they have different attributes, keep them separate so each retains
   * its parent's attributes.
   */
  private void removeInnerVertices(String originalPolygonEsriJson,
                                   Feature newFeature) {
    Set<Point2D> innerVertices = findInnerVertices(originalPolygonEsriJson);

    if (innerVertices.isEmpty()) {
      return;
    }

    List<List<Line>> newFeatureLines = getFeatureLinesByRingSorted(newFeature);

    List<Line> linesToDelete = new ArrayList<>();
    for (List<Line> ringLines : newFeatureLines) {
      // Merge consecutive lines that share the same attributes at inner vertices
      int i = 0;
      while (i < ringLines.size()) {

        Line current = ringLines.get(i);
        Line next;

        if (i ==  ringLines.size() - 1) {
          //Check last line to first line in the ring
          next = ringLines.getFirst();
        } else {
          next = ringLines.get(i + 1);
        }

        Point2D currentEndPoint = getPolylineEndPoint(current.getLineJson());

        if (innerVertices.contains(currentEndPoint) && Objects.equals(current.getAttributes(), next.getAttributes())) {
          String combinedLineEsriJson = grpcClientService.mergeAndGeneralizeLines(List.of(current.getLineJson(), next.getLineJson()));
          current.setLineJson(combinedLineEsriJson);
          linesToDelete.add(next);
          ringLines.remove(next);
        } else {
          i++;
        }
      }
    }

    if (!linesToDelete.isEmpty()) {
      lineRepository.deleteAll(linesToDelete);
      List<Line> remainingLines = newFeatureLines.stream()
          .flatMap(List::stream)
          .toList();
      transformationResultProcessingService.numberLines(remainingLines);
      transformationResultProcessingService.validateLinesAreValid(remainingLines, originalPolygonEsriJson);
      lineRepository.saveAll(remainingLines);
    }
  }

  private List<List<Line>> getFeatureLinesByRingSorted(Feature newFeature) {
    var featurePolygons = polygonRepository.findAllByFeature(newFeature);
    var allLines = lineRepository.findAllByPolygonIn(featurePolygons);

    //add random attributes to line for testing
//    allLines.forEach(line -> {
//      var attributes = new HashMap<String, Object>();
//      attributes.put("id", line.getId());
//      line.setAttributes(attributes);
//    });

    // Group by polygon to avoid mixing rings from different polygons
    Map<Polygon, Map<Integer, List<Line>>> polygonToRingLines = allLines.stream()
        .collect(Collectors.groupingBy(Line::getPolygon,
            Collectors.groupingBy(Line::getRingNumber)));

    List<List<Line>> result = new ArrayList<>();
    for (var ringToLines : polygonToRingLines.values()) {
      for (List<Line> ringLines : ringToLines.values()) {
        ringLines.sort(Comparator.comparing(Line::getRingConnectionOrder));
        result.add(ringLines);
      }
    }
    return result;
  }

  /**
   * Find vertices that exist in the original polygon but were removed by generalize.
   * These are collinear (inner) vertices that don't contribute to the polygon shape.
   */
  private Set<Point2D> findInnerVertices(String originalJson) {
    String generalizedPolygon = grpcClientService.generalizePolygon(originalJson);

    Set<Point2D> originalVertices = getPolygonVertices(originalJson);
    Set<Point2D> generalizedVertices = getPolygonVertices(generalizedPolygon);
    originalVertices.removeAll(generalizedVertices);
    return originalVertices;
  }

  private Set<Point2D> getPolygonVertices(String esriJsonPolygon) {
    MapGeometry mapGeometry = OperatorImportFromJson.local()
        .execute(Geometry.Type.Polygon, esriJsonPolygon);
    var esriPolygon = (com.esri.core.geometry.Polygon) mapGeometry.getGeometry();

    Set<Point2D> vertices = new HashSet<>();
    for (int i = 0; i < esriPolygon.getPointCount(); i++) {
      vertices.add(esriPolygon.getPoint(i).getXY());
    }
    return vertices;
  }

  private Point2D getPolylineEndPoint(String esriJsonPolyline) {
    MapGeometry mapGeometry = OperatorImportFromJson.local()
        .execute(Geometry.Type.Polyline, esriJsonPolyline);
    Polyline polyline = (Polyline) mapGeometry.getGeometry();
    int endIndex = polyline.getPathEnd(0) - 1;
    return polyline.getPoint(endIndex).getXY();
  }
}
