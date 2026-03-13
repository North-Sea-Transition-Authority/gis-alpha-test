package uk.co.fivium.gisalphatest.transformations.command;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import uk.co.fivium.gisalphatest.transformations.TransformationType;

@Entity
@Table(name = "transformation_commands")
public class TransformationCommand {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "command_journey_id")
  private CommandJourney commandJourney;

  @JdbcTypeCode(SqlTypes.JSON)
  private Set<UUID> inputIds;

  @Enumerated(EnumType.STRING)
  private CommandStatus status;

  @Enumerated(EnumType.STRING)
  private TransformationType type;

  private Integer commandOrder;

  public UUID getId() {
    return id;
  }

  public CommandJourney getCommandJourney() {
    return commandJourney;
  }

  public void setCommandJourney(CommandJourney commandJourney) {
    this.commandJourney = commandJourney;
  }

  public Set<UUID> getInputIds() {
    return inputIds;
  }

  public void setInputIds(Set<UUID> inputIds) {
    this.inputIds = inputIds;
  }

  public CommandStatus getStatus() {
    return status;
  }

  public void setStatus(CommandStatus status) {
    this.status = status;
  }

  public TransformationType getType() {
    return type;
  }

  public void setType(TransformationType type) {
    this.type = type;
  }

  public Integer getCommandOrder() {
    return commandOrder;
  }

  public void setCommandOrder(Integer commandOrder) {
    this.commandOrder = commandOrder;
  }
}
