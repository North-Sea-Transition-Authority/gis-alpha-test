package uk.co.fivium.gisalphatest.feature;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import uk.co.fivium.gisalphatest.oracle.ShapeType;
import uk.co.fivium.gisalphatest.transformations.command.CommandJourney;
import uk.co.fivium.gisalphatest.transformations.command.TransformationCommand;

@Entity
@Table(name = "features")
public class Feature {

  @Id
  @UuidGenerator
  private UUID id;

  private Integer shapeSidId;

  @Enumerated(EnumType.STRING)
  private ShapeType type;

  private Integer srs;

  private String featureName;

  private BigDecimal featureArea;

  private BigDecimal areaDifference;

  private UUID parentFeatureId;

  private String testCase;

  private Boolean active = true;

  @ManyToOne
  @JoinColumn(name = "created_by_command_id")
  private TransformationCommand createdByCommand;

  @ManyToOne
  @JoinColumn(name = "command_journey_id")
  private CommandJourney commandJourney;

  public UUID getId() {
    return id;
  }

  public Integer getShapeSidId() {
    return shapeSidId;
  }

  public void setShapeSidId(Integer shapeSidId) {
    this.shapeSidId = shapeSidId;
  }

  public ShapeType getType() {
    return type;
  }

  public void setType(ShapeType type) {
    this.type = type;
  }

  public Integer getSrs() {
    return srs;
  }

  public void setSrs(Integer srs) {
    this.srs = srs;
  }

  public String getFeatureName() {
    return featureName;
  }

  public void setFeatureName(String featureName) {
    this.featureName = featureName;
  }

  public BigDecimal getFeatureArea() {
    return featureArea;
  }

  public void setFeatureArea(BigDecimal featureArea) {
    this.featureArea = featureArea;
  }

  public BigDecimal getAreaDifference() {
    return areaDifference;
  }

  public void setAreaDifference(BigDecimal areaDifference) {
    this.areaDifference = areaDifference;
  }

  public UUID getParentFeatureId() {
    return parentFeatureId;
  }

  public void setParentFeatureId(UUID parentFeatureId) {
    this.parentFeatureId = parentFeatureId;
  }

  public String getTestCase() {
    return testCase;
  }

  public void setTestCase(String testCase) {
    this.testCase = testCase;
  }

  public Boolean isActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public TransformationCommand getCreatedByCommand() {
    return createdByCommand;
  }

  public void setCreatedByCommand(TransformationCommand createdByCommand) {
    this.createdByCommand = createdByCommand;
  }

  public CommandJourney getCommandJourney() {
    return commandJourney;
  }

  public void setCommandJourney(CommandJourney commandJourney) {
    this.commandJourney = commandJourney;
  }
}
