package uk.co.fivium.gisalphatest.migration;

public enum Srs {
  ED50(4230, "ED 50"),
  BNG(27700, "OSGB NATIONAL GRID")
  ;
  private final Integer wkid;
  private final String oracleName;

  Srs(Integer wkid, String oracleName) {
    this.wkid = wkid;
    this.oracleName = oracleName;
  }

  public Integer getWkid() {
    return wkid;
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

  public static Srs fromWkid(Integer wkid) {
    for (Srs srs : Srs.values()) {
      if (srs.getWkid().equals(wkid)) {
        return srs;
      }
    }
    return null;
  }
}
