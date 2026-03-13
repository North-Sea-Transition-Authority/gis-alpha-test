package uk.co.fivium.gisalphatest.transformations.command;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransformationCommandRepository extends ListCrudRepository<TransformationCommand, UUID> {

  Optional<TransformationCommand> findFirstByCommandJourneyAndStatusOrderByCommandOrderDesc(
      CommandJourney journey, CommandStatus status);

  Optional<TransformationCommand> findFirstByCommandJourneyAndStatusOrderByCommandOrderAsc(
      CommandJourney journey, CommandStatus status);

  List<TransformationCommand> findByCommandJourneyAndStatus(CommandJourney journey, CommandStatus status);

  @Query("SELECT MAX(tc.commandOrder) FROM TransformationCommand tc WHERE tc.commandJourney = :journey")
  Optional<Integer> findMaxCommandOrderByJourney(CommandJourney journey);
}
