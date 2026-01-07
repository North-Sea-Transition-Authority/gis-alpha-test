package uk.co.fivium.gisalphatest.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "GIS_ALPHA_CUT_LINES")
public class OracleCutLine {

  @Id
  @Column(name = "ID")
  private Long id;

  @Column(name = "CUT_LINE_GEOJSON")
  private String cutLineGeojson;

  @Column(name = "TEST_CASE")
  private String testCase;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCutLineGeojson() {
    return cutLineGeojson;
  }

  public void setCutLineGeojson(String cutLineGeojson) {
    this.cutLineGeojson = cutLineGeojson;
  }

  public String getTestCase() {
    return testCase;
  }

  public void setTestCase(String testCase) {
    this.testCase = testCase;
  }

}