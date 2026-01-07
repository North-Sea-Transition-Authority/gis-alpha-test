package uk.co.fivium.gisalphatest.oracle;

import java.io.Serial;
import java.io.Serializable;

public class OracleShapeCompositeKey implements Serializable {

  @Serial
  private static final long serialVersionUID = 7238002817265532511L;

  private Integer shapeSidId;
  private String testCase;

  public Integer getShapeSidId() {
    return shapeSidId;
  }

  public void setShapeSidId(Integer shapeSidId) {
    this.shapeSidId = shapeSidId;
  }

  public String getTestCase() {
    return testCase;
  }

  public void setTestCase(String testCase) {
    this.testCase = testCase;
  }
}
