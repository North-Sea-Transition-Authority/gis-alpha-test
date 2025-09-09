package uk.co.fivium.gisalphatest.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestUtil {

  // ED50
  public static final int ED50_SR = 4230;
  // BNG
  public static final int BNG_SR = 27700;

  public static final double ORACLE_AREA_CALCULATION_ED50_POLYGON_AREA_KM2 = 201.514163007667;
  public static final double ORACLE_AREA_CALCULATION_BNG_POLYGON_AREA_KM2 = 21.1926213500511;

  public static List<Coordinate> rotateCoordinateRing(List<Coordinate> coordinates, int distance) {
    var newCoordinates = new ArrayList<>(coordinates);
    newCoordinates.removeLast(); // Remove the duplicated end point that closes the ring
    Collections.rotate(newCoordinates, distance);
    newCoordinates.add(newCoordinates.getFirst()); // Add the end point back
    return newCoordinates;
  }
}
