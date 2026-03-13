
package uk.co.fivium.gisalphatest.migration;

import static uk.co.fivium.gisalphatest.feature.LineUtils.getLinesFromFeature;
import static uk.co.fivium.gisalphatest.migration.Srs.fromOracleName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureAreaService;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.FeatureService;
import uk.co.fivium.gisalphatest.feature.FeatureType;
import uk.co.fivium.gisalphatest.feature.Line;
import uk.co.fivium.gisalphatest.feature.LineNavigationType;
import uk.co.fivium.gisalphatest.feature.LineRepository;
import uk.co.fivium.gisalphatest.feature.Polygon;
import uk.co.fivium.gisalphatest.feature.PolygonRepository;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;
import uk.co.fivium.gisalphatest.oracle.OraclePolygonBoundary;
import uk.co.fivium.gisalphatest.oracle.OracleService;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;
import uk.co.fivium.gisalphatest.transformations.command.CommandJourneyRepository;
import uk.co.fivium.gisalphatest.transformations.command.TransformationCommandRepository;

@Service
@Profile("development")
public class MigrationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationService.class);

  private static final boolean useGrpc = true;

  private final LineRepository lineRepository;
  private final PolygonRepository polygonRepository;
  private final FeatureRepository featureRepository;

  private final OracleService oracleService;
  private final FeatureService featureService;
  private final PolygonService polygonService;

  private final MigrationRestApiService migrationRestApiService;
  private final GrpcClientService grpcClientService;
  private final FeatureAreaService featureAreaService;
  private final TransformationCommandRepository transformationCommandRepository;
  private final CommandJourneyRepository commandJourneyRepository;

  MigrationService(
      LineRepository lineRepository,
      PolygonRepository polygonRepository,
      FeatureRepository featureRepository,
      OracleService oracleService,
      FeatureService featureService,
      PolygonService polygonService,
      MigrationRestApiService migrationRestApiService,
      GrpcClientService grpcClientService,
      FeatureAreaService featureAreaService,
      TransformationCommandRepository transformationCommandRepository,
      CommandJourneyRepository commandJourneyRepository) {
    this.lineRepository = lineRepository;
    this.polygonRepository = polygonRepository;
    this.featureRepository = featureRepository;
    this.oracleService = oracleService;
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.migrationRestApiService = migrationRestApiService;
    this.grpcClientService = grpcClientService;
    this.featureAreaService = featureAreaService;
    this.transformationCommandRepository = transformationCommandRepository;
    this.commandJourneyRepository = commandJourneyRepository;
  }

  @Transactional
  public void migrate(Collection<OracleShapeCompositeKey> ids) {

    //Added this to make testing on local dev easier
    resetDataBase();

    var entityBackedOracleShapes = oracleService.getEntityBackedOracleShapes(ids);

    for (var entityBackedShape : entityBackedOracleShapes) {
      System.out.printf("migrating %s %s%n", entityBackedShape.shape().getShapeSidId(), entityBackedShape.shape().getShapeName());
      var newFeature = migrateFeature(entityBackedShape);

      Map<Polygon, List<Line>> polygonToLine = new HashMap<>();

      for (var polygonAndBoundary : entityBackedShape.polygonToBoundary().entrySet()) {
        var oraclePolygon = polygonAndBoundary.getKey();
        var oracleBoundaries = polygonAndBoundary.getValue();

        var newPolygon = migratePolygon(
            oraclePolygon.getPolygonSidId(),
            newFeature,
            Map.of() //TODO: Set attributes
        );

        var parentLines = new ArrayList<String>();
        if (newFeature.getParentFeatureId() != null) {
          var parent = featureRepository.findById(newFeature.getParentFeatureId()).orElseThrow();
          parentLines.addAll(featureService.getEntityBackedFeature(parent).polygonToLines().values().stream().flatMap(List::stream).map(Line::getLineJson).toList());
        }

        var newLines = migrateLines(
            newFeature,
            newPolygon,
            oracleBoundaries,
            entityBackedShape,
            parentLines
        );

        polygonToLine.put(newPolygon, newLines);

      }

      featureRepository.save(newFeature);
      for (var entry : polygonToLine.entrySet()) {
        var polygon = entry.getKey();
        polygonRepository.save(polygon);

        var lines = entry.getValue();
        var nonDuplicateLines = removeLineDuplicates(lines);
        lineRepository.saveAll(nonDuplicateLines);
      }
    }
  }

  private void resetDataBase() {
    featureService.deleteAll();
    transformationCommandRepository.deleteAll();
    commandJourneyRepository.deleteAll();
  }

  //TODO GISA-89 duplicate lines being persisted.
  //This is a temporal fix to update the line entity to use UUIDs without saving duplicates
  private List<Line> removeLineDuplicates(List<Line> lines) {
    Set<Integer> seenIds = new HashSet<>();
    List<Line> nonDuplicateLines = new ArrayList<>();
    for (var line : lines) {
      if (seenIds.contains(line.getOracleLineSsid())) {
        continue;
      } else {
        seenIds.add(line.getOracleLineSsid());
        nonDuplicateLines.add(line);
      }
    }
    return nonDuplicateLines;
  }

  public void verifyAllChildFeaturesAreInsideParentFeatures() {
    var childFeatures = featureRepository.findAllByParentFeatureIdIsNotNull();

    for (var child : childFeatures) {
      var parent = featureRepository.findById(child.getParentFeatureId())
          .orElseThrow(() -> new IllegalStateException("Parent feature not found for child %s".formatted(child.getId())));
      var childJson = grpcClientService.unionPolygons(polygonService.getPolygonsAsEsriJson(child, false));
      var parentJson = grpcClientService.unionPolygons(polygonService.getPolygonsAsEsriJson(parent, false));

      if (!grpcClientService.checkParentContainsChild(parentJson, childJson)) {
        throw new IllegalStateException("Child %s not contained by parent %s".formatted(child.getFeatureName(), parent.getFeatureName()));
      }

    }
    System.out.println("All children features are contained by parent features");

    verifyChildGeodesicLinesOverlapParents();
  }

  private void verifyChildGeodesicLinesOverlapParents() {
    var childFeatures = featureRepository.findAllByParentFeatureIdIsNotNull();
    for (var child : childFeatures) {
      var childLines = getLinesFromFeature(featureService.getEntityBackedFeature(child));

      if (childLines.stream().noneMatch(line -> LineNavigationType.GEODESIC.equals(line.getNavigationType()))) {
        continue;
      }

      var parent = featureRepository.findById(child.getParentFeatureId())
          .orElseThrow(() -> new IllegalStateException("Parent feature not found for child %s".formatted(child.getId())));
      var parentLines = getLinesFromFeature(featureService.getEntityBackedFeature(parent));

      var overlaps = grpcClientService.verifyChildGeodesicLinesOverlapParents(parentLines, childLines);

      System.out.printf("Child shape's '%s' geodesic lines overlap parent shape's '%s' geodesic lines: %s%n"
          .formatted(child.getFeatureName(), parent.getFeatureName(), overlaps));
    }
  }

  public void verifySubareasTopologicallyEqualToBlock(Integer shapeSid, String testCase) {

    var parentFeature = featureRepository.findByShapeSidIdAndTestCase(shapeSid, testCase).orElseThrow();
    var childFeatures = featureRepository.findAllByParentFeatureId(parentFeature.getId());

    var parentPolygon = grpcClientService.buildPolygon(getLinesFromFeature(
        featureService.getEntityBackedFeature(parentFeature)).stream().map(Line::getLineJson).toList(),
        parentFeature.getSrs()
    );

    var childPolygons = new ArrayList<String>();

    for (var child : childFeatures) {
      var childLines = getLinesFromFeature(featureService.getEntityBackedFeature(child)).stream().map(Line::getLineJson).toList();
      childPolygons.add(grpcClientService.buildPolygon(childLines, child.getSrs()));
    }

    var childPolygonUnion = grpcClientService.unionPolygons(childPolygons);

    var isEqual = grpcClientService.checkPolygonsAreTopologicallyEqual(parentPolygon, childPolygonUnion);

    System.out.printf("Sub area union of parent shape %s are topologically equal: %s%n", parentFeature.getFeatureName(), isEqual);
  }

  @Transactional
  public void migrateFeatureAreas() {
    var features = featureRepository.findAll();

    for(var feature: features) {
      if (feature.getShapeSidId() == null) {
        //only get areas for oracle migrated features
        continue;
      }

      featureAreaService.calculateFeatureArea(feature);
      featureAreaService.calculateAreaDifference(feature, oracleService.getOracleShapeArea(new OracleShapeCompositeKey(feature.getShapeSidId(), feature.getTestCase())));
      if (feature.getAreaDifference().abs().compareTo(BigDecimal.valueOf(20)) > 0) {
        LOGGER.warn("Shape id {} has new area greater than 20 metres squared different to oracle. Difference: {} ",
            feature.getId(),
            feature.getAreaDifference()
        );
      }
      System.out.printf("Shape %s area %s difference %s%n", feature.getFeatureName(), feature.getFeatureArea(), feature.getAreaDifference());
    }

    featureRepository.saveAll(features);
  }

  private Feature migrateFeature(EntityBackedOracleShape entityBackedShape) {
    var newFeature = new Feature();
    newFeature.setShapeSidId(entityBackedShape.shape().getShapeSidId());
    newFeature.setFeatureName(entityBackedShape.shape().getShapeName());
    newFeature.setTestCase(entityBackedShape.shape().getTestCase());
    newFeature.setSrs(fromOracleName(entityBackedShape.shape().getShapeSrs()).getWkid());

    newFeature.setType(FeatureType.POLYGON);

    var parentShapeId = entityBackedShape.shape().getParentShapeId();
    if (parentShapeId != null) {
      var parentId = featureRepository.findAllByShapeSidId(parentShapeId)
          .stream()
          .filter(feature -> feature.getParentFeatureId() == null)
          .map(Feature::getId)
          .findFirst()
          .orElse(null);
      newFeature.setParentFeatureId(parentId);
    }

    newFeature.setFeatureArea(null);
    return newFeature;
  }

  private Polygon migratePolygon(
      Integer polygonSidId,
      Feature feature,
      Map<String, Object> attributes
  ) {
    var polygon = new Polygon();
    polygon.setOraclePolygonSsid(polygonSidId);
    polygon.setAttributes(attributes);
    polygon.setFeature(feature);
    return polygon;
  }

  private List<Line> migrateLines(
      Feature feature,
      Polygon polygon,
      List<OraclePolygonBoundary> oracleBoundaries,
      EntityBackedOracleShape entityBackedShape,
      List<String> parentLines
  ) {
// Collect all lines for this polygon to batch the gRPC call
    List<OracleBoundaryLineWithRing> linesWithRing = new ArrayList<>();
    for (var oracleBoundary : oracleBoundaries) {
      for (var oracleLine : entityBackedShape.boundaryToLine().get(oracleBoundary)) {
        linesWithRing.add(new OracleBoundaryLineWithRing(oracleLine, oracleBoundaries.indexOf(oracleBoundary)));
      }
    }

    // Single batch gRPC call for all lines in this polygon
    Map<Integer, String> esriJsonByLineSsid = grpcClientService.convertLinesToEsriJson(
        linesWithRing,
        feature.getSrs(),
        parentLines
    );

    // Create Line entities from the batch results
    List<Line> newLines = new ArrayList<>();
    for (var entry : linesWithRing) {
      var oracleLine = entry.oracleBoundaryLine();
      var oracleLineSsid = oracleLine.getLineSidId().intValue();
      var line = new Line();
      line.setOracleLineSsid(oracleLineSsid);
      line.setAttributes(Map.of());
      line.setPolygon(polygon);
      line.setNavigationType(oracleLine.getLineNavigationType());
      line.setBoundarySidId(oracleLine.getBoundarySidId().intValue());
      line.setLineJson(esriJsonByLineSsid.get(oracleLineSsid));
      line.setRingNumber(entry.ringNumber());
      line.setRingConnectionOrder(oracleLine.getConnectionOrder().intValue());
      newLines.add(line);
    }
    return newLines;
  }
}
