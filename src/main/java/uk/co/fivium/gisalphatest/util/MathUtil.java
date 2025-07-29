package uk.co.fivium.gisalphatest.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtil {

  public static double roundDecimalPlaces(double value, int places) {
    if (places < 0) {
      throw new IllegalArgumentException();
    }

    return BigDecimal.valueOf(value)
        .setScale(places, RoundingMode.HALF_UP)
        .doubleValue();
  }
}
