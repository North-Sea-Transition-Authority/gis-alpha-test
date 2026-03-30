package uk.co.fivium.gisalphatest.feature;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisalphatest.migration.Srs;
import uk.co.fivium.gisalphatest.oracle.ShapeType;
import uk.co.fivium.gisalphatest.util.StreamUtil;

@Service
public class FeatureService {

  private final FeatureRepository featureRepository;
  private final PolygonRepository polygonRepository;
  private final LineRepository lineRepository;
  private final EntityManager entityManager;
  private final ShapesConfigProperties shapesConfigProperties;

  FeatureService(
      FeatureRepository featureRepository,
      PolygonRepository polygonRepository,
      LineRepository lineRepository,
      EntityManager entityManager,
      ShapesConfigProperties shapesConfigProperties
  ) {
    this.featureRepository = featureRepository;
    this.polygonRepository = polygonRepository;
    this.lineRepository = lineRepository;
    this.entityManager = entityManager;
    this.shapesConfigProperties = shapesConfigProperties;
  }

  public EntityBackedFeature getEntityBackedFeature(Integer shapeSidId, String testCase) {
    var feature = getFeature(shapeSidId, testCase);
    return getEntityBackedFeature(feature);
  }

  public EntityBackedFeature getEntityBackedFeature(Feature feature) {
    var polygons = polygonRepository.findAllByFeature(feature);

    Map<Polygon, List<Line>> polygonToLines = new HashMap<>();

    for (var polygon : polygons) {
      polygonToLines.put(polygon, lineRepository.findAllByPolygon(polygon));
    }

    return
        new EntityBackedFeature(
            feature,
            polygonToLines
        );
  }

  public Feature getFeature(Integer shapeSidId, String testCase) {
    return featureRepository.findByShapeSidIdAndTestCase(shapeSidId, testCase)
        .orElseThrow(() -> new IllegalStateException("Feature not found for shapeSidId %s test case %s"
            .formatted(shapeSidId, testCase)));
  }

  @Transactional
  public void deleteAll() {
    lineRepository.deleteAll();
    polygonRepository.deleteAll();
    var features = featureRepository.findAll();
    featureRepository.deleteAll(features.stream().filter(f -> f.getParentFeatureId() != null).toList());
    featureRepository.deleteAll(features.stream().filter(f -> f.getParentFeatureId() == null).toList());
    entityManager.flush();
  }

  @Transactional
  public void deleteAll(Collection<Feature> features) {
    lineRepository.deleteAllByPolygon_FeatureIn(features);
    polygonRepository.deleteAllByFeatureIn(features);
    featureRepository.deleteAll(features.stream().filter(f -> f.getParentFeatureId() != null).toList());
    featureRepository.deleteAll(features.stream().filter(f -> f.getParentFeatureId() == null).toList());
  }

  @Transactional
  public void resetTablesForUserTesting() {
    deleteAll();

    if (shapesConfigProperties.shapes() == null || shapesConfigProperties.shapes().isEmpty()) {
      return;
    }

    int shapeSid = 1;
    for (var entry : shapesConfigProperties.shapes().entrySet()) {
      var shapeConfig = entry.getValue();
      var feature = createFeatureEntity(shapeSid, shapeConfig.featureName(), shapeConfig.testCase());
      var polygon = createPolygonEntity(shapeSid, feature);

      int lineIndex = 1;
      for (var lineConfig : shapeConfig.lines()) {
        createLineEntity(shapeSid, lineIndex, polygon, lineConfig.lineJson(), lineConfig.type(), lineConfig.ringNumber());
        lineIndex++;
      }
      shapeSid++;
    }
  }

  private Feature createFeatureEntity(int id, String name, String testCase) {
    Feature feature = new Feature();
    feature.setShapeSidId(id);
    feature.setFeatureName(name);
    feature.setType(ShapeType.BLOCK);
    feature.setSrs(Srs.ED50.getWkid());
    feature.setTestCase(testCase);
    return featureRepository.save(feature);
  }

  private Polygon createPolygonEntity(int id, Feature feature) {
    Polygon polygon = new Polygon();
    polygon.setOraclePolygonSsid(id * 100);
    polygon.setFeature(feature);
    polygon.setAttributes(Map.of());
    return polygonRepository.save(polygon);
  }

  private void createLineEntity(
      int id,
      int lineIndex,
      Polygon polygon,
      String lineJson,
      LineNavigationType lineNavigationType,
      Integer ringNumber
  ) {
    Line line = new Line();
    line.setOracleLineSsid(id * 1000 + lineIndex);
    line.setBoundarySidId(id * 10000 + lineIndex);
    line.setPolygon(polygon);
    line.setNavigationType(lineNavigationType);
    line.setRingNumber(ringNumber);
    line.setRingConnectionOrder(lineIndex);
    line.setLineJson(lineJson);
    line.setAttributes(Map.of());
    lineRepository.save(line);
  }

  public Map<String, String> getFeatureIdNameMap() {
    return featureRepository.findAllByActive(true).stream()
        .sorted(Comparator.comparing(Feature::getFeatureName))
        .collect(StreamUtil.toLinkedHashMap(
                feature -> feature.getId().toString(),
                feature -> "%s %s".formatted(feature.getFeatureName(), feature.getType())
            )
        );
  }
}
