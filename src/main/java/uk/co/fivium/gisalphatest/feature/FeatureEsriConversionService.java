package uk.co.fivium.gisalphatest.feature;

import static uk.co.fivium.gisalphatest.util.EsriGeometryApiUtil.appendRingToPolygon;

import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCPolygon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.util.EsriGeometryApiUtil;

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

      var ringEsriPoints = new ArrayList<com.esri.core.geometry.Point>();

      for (var line : sortedLinesInRing) {
        var sortedLinePoints = pointsByLineId.get(line.getId())
            .stream()
            .sorted(Comparator.comparing(Point::getLineConnectionOrder))
            .map(EsriGeometryApiUtil::toEsri)
            .toList();

        ringEsriPoints.addAll(sortedLinePoints);
      }

      appendRingToPolygon(esriPolygon, ringEsriPoints);
    }

    return (OGCPolygon) OGCGeometry.createFromEsriGeometry(esriPolygon, SpatialReference.create(polygon.getFeature().getSrs()));
  }
}
