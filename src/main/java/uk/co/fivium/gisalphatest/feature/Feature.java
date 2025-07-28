package uk.co.fivium.gisalphatest.feature;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "features")
class Feature {

  @Id
  @UuidGenerator
  private UUID id;

  @Enumerated(EnumType.STRING)
  private FeatureType type;

  private Integer srs;

  UUID getId() {
    return id;
  }

  FeatureType getType() {
    return type;
  }

  void setType(FeatureType type) {
    this.type = type;
  }

  Integer getSrs() {
    return srs;
  }

  void setSrs(Integer srs) {
    this.srs = srs;
  }
}
