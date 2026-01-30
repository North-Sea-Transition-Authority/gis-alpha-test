package uk.co.fivium.gisalphatest.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "lines")
public class Line {

  @Id
  @UuidGenerator
  private UUID id;

  private Integer oracleLineSsid;

  private Integer boundarySidId;

  @JoinColumn(name = "polygon_id")
  @ManyToOne
  private Polygon polygon;

  @Enumerated(EnumType.STRING)
  private LineNavigationType navigationType;

  private Integer ringNumber;

  private Integer ringConnectionOrder;

  private String lineJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> attributes;

  public UUID getId() {
    return id;
  }

  public Integer getOracleLineSsid() {
    return oracleLineSsid;
  }

  public void setOracleLineSsid(Integer oracleLineSsid) {
    this.oracleLineSsid = oracleLineSsid;
  }

  public Integer getBoundarySidId() {
    return boundarySidId;
  }

  public void setBoundarySidId(Integer boundarySidId) {
    this.boundarySidId = boundarySidId;
  }

  public Polygon getPolygon() {
    return polygon;
  }

  public void setPolygon(Polygon polygon) {
    this.polygon = polygon;
  }

  public LineNavigationType getNavigationType() {
    return navigationType;
  }

  public void setNavigationType(LineNavigationType navigationType) {
    this.navigationType = navigationType;
  }

  public Integer getRingNumber() {
    return ringNumber;
  }

  public void setRingNumber(Integer ringNumber) {
    this.ringNumber = ringNumber;
  }

  public Integer getRingConnectionOrder() {
    return ringConnectionOrder;
  }

  public void setRingConnectionOrder(Integer ringConnectionOrder) {
    this.ringConnectionOrder = ringConnectionOrder;
  }

  public boolean isExteriorRing() {
    return ringNumber == 0;
  }

  public String getLineJson() {
    return lineJson;
  }

  public void setLineJson(String lineJson) {
    this.lineJson = lineJson;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }
}
