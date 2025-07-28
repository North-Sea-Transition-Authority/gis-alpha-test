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
class Line {

  @Id
  @UuidGenerator
  private UUID id;

  @JoinColumn(name = "feature_id")
  @ManyToOne
  private Feature feature;

  @JoinColumn(name = "polygon_id")
  @ManyToOne
  private Polygon polygon;

  @Enumerated(EnumType.STRING)
  private LineNavigationType navigationType;

  private boolean exteriorRing;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> attributes;

  UUID getId() {
    return id;
  }

  Feature getFeature() {
    return feature;
  }

  void setFeature(Feature feature) {
    this.feature = feature;
  }

  Polygon getPolygon() {
    return polygon;
  }

  void setPolygon(Polygon polygon) {
    this.polygon = polygon;
  }

  LineNavigationType getNavigationType() {
    return navigationType;
  }

  void setNavigationType(LineNavigationType navigationType) {
    this.navigationType = navigationType;
  }

  boolean isExteriorRing() {
    return exteriorRing;
  }

  void setExteriorRing(boolean external) {
    this.exteriorRing = external;
  }

  Map<String, Object> getAttributes() {
    return attributes;
  }

  void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }
}
