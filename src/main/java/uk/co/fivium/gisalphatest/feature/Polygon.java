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
class Polygon {

  @Id
  @UuidGenerator
  private UUID id;

  @JoinColumn(name = "feature_id")
  @ManyToOne
  private Feature feature;

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

  Map<String, Object> getAttributes() {
    return attributes;
  }

  void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }
}
