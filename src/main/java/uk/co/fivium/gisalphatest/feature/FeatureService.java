package uk.co.fivium.gisalphatest.feature;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureService {

  private final FeatureRepository featureRepository;
  private final PolygonRepository polygonRepository;
  private final LineRepository lineRepository;
  private final EntityManager entityManager;

  FeatureService(
      FeatureRepository featureRepository,
      PolygonRepository polygonRepository,
      LineRepository lineRepository,
      EntityManager entityManager
  ) {
    this.featureRepository = featureRepository;
    this.polygonRepository = polygonRepository;
    this.lineRepository = lineRepository;
    this.entityManager = entityManager;
  }

  public EntityBackedFeature getEntityBackedFeature(Integer shapeSidId, String testCase) {
    var feature = getFeature(shapeSidId, testCase);
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

  public Feature createFeature(FeatureType featureType, Integer srs) {
    var feature = new Feature();
    feature.setType(featureType);
    feature.setSrs(srs);
    featureRepository.save(feature);
    return feature;
  }

  public Polygon createPolygon(Feature feature) {
    var polygon = new Polygon();
    polygon.setFeature(feature);
    polygon.setAttributes(new HashMap<>());
    polygonRepository.save(polygon);
    return polygon;
  }

  public Line createLine(
      Feature feature,
      @Nullable Polygon polygon,
      LineNavigationType navigationType,
      int ringNumber,
      int ringConnectionOrder,
      String lineJson
  ) {
    var line = new Line();
    line.setPolygon(polygon);
    line.setNavigationType(navigationType);
    line.setRingNumber(ringNumber);
    line.setRingConnectionOrder(ringConnectionOrder);
    line.setAttributes(new HashMap<>());
    line.setLineJson(lineJson);
    lineRepository.save(line);
    return line;
  }
}
