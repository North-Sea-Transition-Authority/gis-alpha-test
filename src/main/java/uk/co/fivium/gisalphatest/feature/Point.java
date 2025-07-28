package uk.co.fivium.gisalphatest.feature;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "points")
class Point {

  @Id
  @UuidGenerator
  private UUID id;

  @JoinColumn(name = "feature_id")
  @ManyToOne
  private Feature feature;

  @JoinColumn(name = "line_id")
  @ManyToOne
  private Line line;

  private double x;

  private double z;

  UUID getId() {
    return id;
  }

  Feature getFeature() {
    return feature;
  }

  void setFeature(Feature feature) {
    this.feature = feature;
  }

  Line getLine() {
    return line;
  }

  void setLine(Line line) {
    this.line = line;
  }

  double getX() {
    return x;
  }

  void setX(double x) {
    this.x = x;
  }

  double getZ() {
    return z;
  }

  void setZ(double y) {
    this.z = y;
  }
}
