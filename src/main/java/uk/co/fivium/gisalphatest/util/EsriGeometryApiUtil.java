package uk.co.fivium.gisalphatest.util;

import java.util.List;
import uk.co.fivium.gisalphatest.feature.Point;

public class EsriGeometryApiUtil {

  public static void appendRingToPolygon(com.esri.core.geometry.Polygon esriPolygon, List<com.esri.core.geometry.Point> ringPoints) {
    esriPolygon.startPath(ringPoints.getFirst());

    com.esri.core.geometry.Point previousPoint = null;

    // Never add the end point, the line will be auto closed.
    // TODO: validate the end point is the same as first?
    for (var point : ringPoints.subList(1, ringPoints.size() - 1).reversed()) {
      if (previousPoint != null && previousPoint.getX() == point.getX() && previousPoint.getY() == point.getY()) {
        continue;
      }
      esriPolygon.lineTo(point);
      previousPoint = point;
    }
  }

  public static com.esri.core.geometry.Point toEsri(Point point) {
    return new com.esri.core.geometry.Point(point.getX().doubleValue(), point.getZ().doubleValue());
  }
}
