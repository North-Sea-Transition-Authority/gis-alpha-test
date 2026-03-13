package uk.co.fivium.gisalphatest.transformations.command;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "command_journeys")
public class CommandJourney {

  @Id
  @UuidGenerator
  private UUID id;

  public UUID getId() {
    return id;
  }
}
