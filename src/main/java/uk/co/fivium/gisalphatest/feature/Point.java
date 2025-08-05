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
public class Point {

  @Id
  @UuidGenerator
  private UUID id;

  @JoinColumn(name = "feature_id")
  @ManyToOne
  private Feature feature;

  @JoinColumn(name = "line_id")
  @ManyToOne
  private Line line;

  private Integer lineConnectionOrder;

  private double x;

  private double z;

  public UUID getId() {
    return id;
  }

  public Feature getFeature() {
    return feature;
  }

  public void setFeature(Feature feature) {
    this.feature = feature;
  }

  public Line getLine() {
    return line;
  }

  public void setLine(Line line) {
    this.line = line;
  }

  public int getLineConnectionOrder() {
    return lineConnectionOrder;
  }

  public void setLineConnectionOrder(Integer lineConnectionOrder) {
    this.lineConnectionOrder = lineConnectionOrder;
  }

  public double getX() {
    return x;
  }

  public void setX(double x) {
    this.x = x;
  }

  public double getZ() {
    return z;
  }

  public void setZ(double z) {
    this.z = z;
  }
}
