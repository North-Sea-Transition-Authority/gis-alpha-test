package uk.co.fivium.gisalphatest.util;

import static uk.co.fivium.gisalphatest.util.EsriGeometryApiUtil.appendRingToPolygon;
import static uk.co.fivium.gisalphatest.util.MathUtil.roundDecimalPlaces;
import static uk.co.fivium.gisalphatest.util.TestUtil.rotateCoordinateRing;

import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCLineString;
import com.esri.core.geometry.ogc.OGCPolygon;
import java.util.ArrayList;
import java.util.List;

public class EsriGeometryApiTestUtil {

  public static OGCPolygon rotatePolygon(OGCPolygon polygon, int distance) {
    var exteriorRing = polygon.exteriorRing();

    var coordinates = new ArrayList<Coordinate>();
    for (int i = 0; i < exteriorRing.numPoints(); i++) {
      var point = exteriorRing.pointN(i);
      coordinates.add(new Coordinate(point.X(), point.Y()));
    }

    rotateCoordinateRing(coordinates, distance);

    return newPolygonFromCoordinates(coordinates, polygon.getEsriSpatialReference());
  }

  public static OGCLineString newLineStringFromCoordinates(List<Coordinate> coordinates) {
    var lineString = new com.esri.core.geometry.Polyline();

    lineString.startPath(toEsri(coordinates.getFirst()));

    for (var point : coordinates.subList(1, coordinates.size()).reversed()) {
      lineString.lineTo(toEsri(point));
    }

    return (OGCLineString) OGCGeometry.createFromEsriGeometry(lineString, null);
  }

  public static OGCPolygon newPolygonFromCoordinates(List<Coordinate> coordinates, SpatialReference spatialReference) {
    var newPolygon = new com.esri.core.geometry.Polygon();

    var points = coordinates.stream()
        .map(coordinate -> new Point(coordinate.x(), coordinate.z()))
        .toList();

    appendRingToPolygon(newPolygon, points);

    return (OGCPolygon) OGCGeometry.createFromEsriGeometry(newPolygon, spatialReference);
  }

  public static List<Coordinate> getRoundedCoordinates(OGCLineString lineString, int decimalPlaces) {
    return getCoordinates(lineString)
        .stream()
        .map(coordinate ->
            new Coordinate(
                roundDecimalPlaces(coordinate.x(), decimalPlaces),
                roundDecimalPlaces(coordinate.z(), decimalPlaces)
            )
        )
        .toList();
  }

  public static List<Coordinate> getCoordinates(OGCLineString lineString) {
    var coordinates = new ArrayList<Coordinate>();
    for (var i = 0; i < lineString.numPoints(); i++) {
      var point = lineString.pointN(i);
      coordinates.add(new Coordinate(point.X(), point.Y()));
    }
    return coordinates;
  }

  public static com.esri.core.geometry.Point toEsri(Coordinate coordinate) {
    return new com.esri.core.geometry.Point(coordinate.x(), coordinate.z());
  }
}
