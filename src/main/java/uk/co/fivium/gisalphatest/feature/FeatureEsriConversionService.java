package uk.co.fivium.gisalphatest.feature;

import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ogc.OGCGeometry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FeatureEsriConversionService {

  private final LineRepository lineRepository;

  FeatureEsriConversionService(
      LineRepository lineRepository
  ) {
    this.lineRepository = lineRepository;
  }
  /**
   * After updating the model in GISA-32 this no longer works, and we are not going to update it as we will exclusively use the
   * arc gis js sdk
   */
  public OGCGeometry toOgc(Polygon polygon) {
    var lines = lineRepository.findAllByPolygon(polygon);
    var esriPolygon = new com.esri.core.geometry.Polygon();

    var linesByRingNumber = lines.stream()
        .collect(Collectors.groupingBy(Line::getRingNumber));

    var sortedRingNumbers = linesByRingNumber.keySet().stream()
        .sorted()
        .toList();

    for (var ringNumber : sortedRingNumbers) {
      var sortedLinesInRing = linesByRingNumber.get(ringNumber).stream()
          .sorted(Comparator.comparing(Line::getRingConnectionOrder))
          .toList();

      var ringEsriPoints = new ArrayList<com.esri.core.geometry.Point>();

//      for (var line : sortedLinesInRing) {
//        var sortedLinePoints = line.getLineJson()
//            .stream()
//            .map(EsriGeometryApiUtil::toEsri)
//            .toList();
//
//        ringEsriPoints.addAll(sortedLinePoints);
//      }
//      appendRingToPolygon(esriPolygon, ringEsriPoints);
    }

    return OGCGeometry.createFromEsriGeometry(esriPolygon, SpatialReference.create(polygon.getFeature().getSrs()));
  }
}
