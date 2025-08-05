package uk.co.fivium.gisalphatest.feature;

import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCPolygon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FeatureEsriConversionService {

  private final LineRepository lineRepository;
  private final PointRepository pointRepository;

  FeatureEsriConversionService(LineRepository lineRepository, PointRepository pointRepository) {
    this.lineRepository = lineRepository;
    this.pointRepository = pointRepository;
  }

  public OGCPolygon toOgc(Polygon polygon) {
    var lines = lineRepository.findAllByPolygon(polygon);
    var points = pointRepository.findAllByLineIn(lines);

    var esriPolygon = new com.esri.core.geometry.Polygon();

    var linesByRingNumber = lines.stream()
        .collect(Collectors.groupingBy(Line::getRingNumber));

    var sortedRingNumbers = linesByRingNumber.keySet().stream()
        .sorted()
        .toList();

    var pointsByLineId = points.stream()
        .collect(Collectors.groupingBy(point -> point.getLine().getId()));

    for (var ringNumber : sortedRingNumbers) {
      var sortedLinesInRing = linesByRingNumber.get(ringNumber).stream()
          .sorted(Comparator.comparing(Line::getRingConnectionOrder))
          .toList();

      var ringPoints = new ArrayList<Point>();

      for (var line : sortedLinesInRing) {
        var sortedLinePoints = pointsByLineId.get(line.getId())
            .stream()
            .sorted(Comparator.comparing(Point::getLineConnectionOrder))
            .toList();

        ringPoints.addAll(sortedLinePoints);
      }

      esriPolygon.startPath(toEsri(ringPoints.getFirst()));

      Point previousPoint = null;

      // Never add the end point, the line will be auto closed.
      // TODO: validate the end point is the same as first?
      for (var point : ringPoints.subList(1, ringPoints.size() - 1).reversed()) {
        if (previousPoint != null && previousPoint.getX() == point.getX() && previousPoint.getZ() == point.getZ()) {
          continue;
        }
        esriPolygon.lineTo(toEsri(point));
        previousPoint = point;
      }
    }

    return (OGCPolygon) OGCGeometry.createFromEsriGeometry(esriPolygon, SpatialReference.create(polygon.getFeature().getSrs()));
  }

  private com.esri.core.geometry.Point toEsri(Point point) {
    return new com.esri.core.geometry.Point(point.getX(), point.getZ());
  }
}
