package uk.co.fivium.gisalphatest.transformations.command;

import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandJourneyRepository extends ListCrudRepository<CommandJourney, UUID> {
}
