package uk.co.fivium.gisalphatest.feature;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.HashMap;
import org.springframework.stereotype.Service;

@Service
public class FeatureService {

  private final FeatureRepository featureRepository;
  private final PolygonRepository polygonRepository;
  private final LineRepository lineRepository;
  private final PointRepository pointRepository;

  FeatureService(
      FeatureRepository featureRepository,
      PolygonRepository polygonRepository,
      LineRepository lineRepository,
      PointRepository pointRepository
  ) {
    this.featureRepository = featureRepository;
    this.polygonRepository = polygonRepository;
    this.lineRepository = lineRepository;
    this.pointRepository = pointRepository;
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
      int ringConnectionOrder
  ) {
    var line = new Line();
    line.setFeature(feature);
    line.setPolygon(polygon);
    line.setNavigationType(navigationType);
    line.setRingNumber(ringNumber);
    line.setRingConnectionOrder(ringConnectionOrder);
    line.setAttributes(new HashMap<>());
    lineRepository.save(line);
    return line;
  }

  public Point createPoint(
      Feature feature,
      @Nullable Line line,
      int lineConnectionOrder,
      double x,
      double z
  ) {
    var point = new Point();
    point.setFeature(feature);
    point.setLine(line);
    point.setLineConnectionOrder(lineConnectionOrder);
    point.setX(BigDecimal.valueOf(x));
    point.setZ(BigDecimal.valueOf(z));
    pointRepository.save(point);
    return point;
  }
}
