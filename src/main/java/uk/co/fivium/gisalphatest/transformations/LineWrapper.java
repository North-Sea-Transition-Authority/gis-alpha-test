package uk.co.fivium.gisalphatest.transformations;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.OperatorImportFromJson;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import uk.co.fivium.gisalphatest.feature.Line;

public record LineWrapper(Line line,
                          Point start,
                          Point end) {

  static LineWrapper fromEntity(Line line) {
    MapGeometry mapGeometry = OperatorImportFromJson.local().execute(Geometry.Type.Polyline, line.getLineJson());
    Polyline polyline = (Polyline) mapGeometry.getGeometry();

    //Line should only have 1 path
    int startIndex = polyline.getPathStart(0);
    int endIndex = polyline.getPathEnd(0) - 1 ; //returns the index after the last element
    Point startPoint = polyline.getPoint(startIndex);
    Point endPoint = polyline.getPoint(endIndex);

    return new LineWrapper(line, startPoint, endPoint);
  }

  public static LineWrapper fromNodeResponse(Line line,
                                             Point startPoint,
                                             Point endpoint) {
    return new LineWrapper(line, startPoint, endpoint);
  }
}
