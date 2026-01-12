package uk.co.fivium.gisalphatest.feature;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import org.springframework.stereotype.Service;

@Service
public class FeatureService {

  private final FeatureRepository featureRepository;
  private final PolygonRepository polygonRepository;
  private final LineRepository lineRepository;

  FeatureService(
      FeatureRepository featureRepository,
      PolygonRepository polygonRepository,
      LineRepository lineRepository
  ) {
    this.featureRepository = featureRepository;
    this.polygonRepository = polygonRepository;
    this.lineRepository = lineRepository;
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
