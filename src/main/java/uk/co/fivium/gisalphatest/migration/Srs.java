package uk.co.fivium.gisalphatest.migration;

public enum Srs {
  ED50(4230, "ED 50"),
  BNG(27700, "OSGB NATIONAL GRID")
  ;
  private final Integer value;
  private final String oracleName;

  Srs(Integer value, String oracleName) {
    this.value = value;
    this.oracleName = oracleName;
  }

  public Integer getValue() {
    return value;
  }

  public String getOracleName() {
    return oracleName;
  }

  public static Srs fromOracleName(String oracleName) {
    for (Srs srs : Srs.values()) {
      if (srs.getOracleName().equals(oracleName)) {
        return srs;
      }
    }
    return null;
  }
}
