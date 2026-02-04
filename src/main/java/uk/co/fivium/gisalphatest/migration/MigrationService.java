
package uk.co.fivium.gisalphatest.migration;

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
import uk.co.fivium.gisalphatest.oracle.OracleBoundaryLine;
import uk.co.fivium.gisalphatest.oracle.OracleService;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Service
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

  MigrationService(
      LineRepository lineRepository,
      PolygonRepository polygonRepository,
      FeatureRepository featureRepository,
      OracleService oracleService,
      FeatureService featureService,
      PolygonService polygonService,
      MigrationRestApiService migrationRestApiService,
      GrpcClientService grpcClientService,
      FeatureAreaService featureAreaService) {
    this.lineRepository = lineRepository;
    this.polygonRepository = polygonRepository;
    this.featureRepository = featureRepository;
    this.oracleService = oracleService;
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.migrationRestApiService = migrationRestApiService;
    this.grpcClientService = grpcClientService;
    this.featureAreaService = featureAreaService;
  }

  @Transactional
  public void migrate(Collection<OracleShapeCompositeKey> ids) {

    //Added this to make testing on local dev easier
    featureService.deleteAll();

    var entityBackedOracleShapes = oracleService.getEntityBackedOracleShapes(ids);

    for (var entityBackedShape : entityBackedOracleShapes) {
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

        List<Line> newLines = new ArrayList<>();

        for (var oracleBoundary : oracleBoundaries) {
          for (var oracleLine : entityBackedShape.boundaryToLine().get(oracleBoundary)) {

            var ringNumber = oracleBoundaries.indexOf(oracleBoundary);
            var ringConnectionOrder = oracleLine.getConnectionOrder().intValue();


            var newLine = migrateLine(
                newPolygon,
                oracleLine,
                ringNumber,
                ringConnectionOrder,
                oracleLine.getLineNavigationType(),
                newFeature.getSrs()
            );

            newLines.add(newLine);
          }
        }

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

    for(var child: childFeatures) {
      var parent = featureRepository.findById(child.getParentFeatureId())
          .orElseThrow(() -> new IllegalStateException("Parent feature not found for child %s".formatted(child.getId())));
      var childJson = grpcClientService.unionPolygons(polygonService.getPolygonsAsEsriJson(child, false));
      var parentJson = grpcClientService.unionPolygons(polygonService.getPolygonsAsEsriJson(parent, false));

      if (!grpcClientService.checkParentContainsChild(parentJson, childJson)) {
        throw new IllegalStateException("Child %s not contained by parent %s".formatted(child.getId(), parent.getId()));
      }

    }
    System.out.println("All children features are contained by parent features");
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
      featureAreaService.calculateAreaDifference(feature, new OracleShapeCompositeKey(feature.getShapeSidId(), feature.getTestCase()));
      if (feature.getAreaDifference().abs().compareTo(BigDecimal.valueOf(20)) > 0) {
        LOGGER.warn("Shape id {} has new area greater than 20 metres squared different to oracle. Difference: {} ",
            feature.getId(),
            feature.getAreaDifference()
        );
      }
    }

    featureRepository.saveAll(features);
  }

  private Feature migrateFeature(EntityBackedOracleShape entityBackedShape) {
    var newFeature = new Feature();
    newFeature.setShapeSidId(entityBackedShape.shape().getShapeSidId());
    newFeature.setFeatureName(entityBackedShape.shape().getShapeName());
    newFeature.setTestCase(entityBackedShape.shape().getTestCase());
    newFeature.setSrs(fromOracleName(entityBackedShape.shape().getShapeSrs()).getValue());

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

  private Line migrateLine(
      Polygon polygon,
      OracleBoundaryLine oracleBoundaryLine,
      Integer ringNumber,
      Integer connectionOrder,
      LineNavigationType lineNavigationType,
      Integer wkid
  ) {

    var line = new Line();
    line.setOracleLineSsid(oracleBoundaryLine.getLineSidId().intValue());
    line.setAttributes(Map.of()); //TODO: Set attributes
    line.setPolygon(polygon);
    line.setNavigationType(lineNavigationType);
    line.setBoundarySidId(oracleBoundaryLine.getBoundarySidId().intValue());

    if (useGrpc) {
      // convert geoJson to esriJson using ArcGis JS SDK via gRPC

      String esriJson = switch (lineNavigationType) {
        case LOXODROME -> grpcClientService.convertLineToEsriJson(oracleBoundaryLine.getLineGeojson(), wkid, false);
        case GEODESIC -> grpcClientService.convertLineToEsriJson(oracleBoundaryLine.getLineGeojson(), wkid, true);
        case CARTESIAN -> null;
      };

      line.setLineJson(esriJson);
    } else {
      // convert geoJson to esriJson using ArcGis JS SDK via rest api

      String esriJson = switch (lineNavigationType) {
        case LOXODROME -> migrationRestApiService.geoJsonLineToEsriJsonLine(oracleBoundaryLine.getLineGeojson());
        case GEODESIC -> null;
        case CARTESIAN -> null;
      };

      line.setLineJson(esriJson);
    }

    line.setRingNumber(ringNumber);
    line.setRingConnectionOrder(connectionOrder);
    return line;
  }
}