package uk.co.fivium.gisalphatest.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "polygons")
public class Polygon {

  @Id
  @UuidGenerator
  private UUID id;

  private Integer oraclePolygonSsid;

  @JoinColumn(name = "feature_id")
  @ManyToOne
  private Feature feature;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> attributes;

  public UUID getId() {
    return id;
  }

  public Integer getOraclePolygonSsid() {
    return oraclePolygonSsid;
  }

  public void setOraclePolygonSsid(Integer id) {
    this.oraclePolygonSsid = id;
  }

  public Feature getFeature() {
    return feature;
  }

  public void setFeature(Feature feature) {
    this.feature = feature;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }
}
