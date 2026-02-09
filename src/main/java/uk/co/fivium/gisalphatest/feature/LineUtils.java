package uk.co.fivium.gisalphatest.feature;

import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;

public class LineUtils {

  // 1 second in degrees (arc second) == 1° (degree) / 60'(minutes) / 60” (seconds)
  public static final Double ONE_ARC_SECOND = 1 / 3600.0;

  public static String pointToEastWestLineJson(double longitude, double latitude) {
    var startPoint = new Point(longitude - ONE_ARC_SECOND, latitude);
    var middlePoint = new Point(longitude, latitude);
    var endPoint = new Point(longitude + ONE_ARC_SECOND, latitude);

    var polyLine = new Polyline();
    polyLine.startPath(startPoint);
    polyLine.lineTo(middlePoint);
    polyLine.lineTo(endPoint);

    return polyLine.toString();
  }

  public static String pointToNorthSouthLineJson(double longitude, double latitude) {
    var startPoint = new Point(longitude, latitude - ONE_ARC_SECOND);
    var middlePoint = new Point(longitude, latitude);
    var endPoint = new Point(longitude, latitude + ONE_ARC_SECOND);

    var polyLine = new Polyline();
    polyLine.startPath(startPoint);
    polyLine.lineTo(middlePoint);
    polyLine.lineTo(endPoint);

    return polyLine.toString();
  }
}
