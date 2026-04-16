package uk.co.fivium.gisalphatest.transformations.command;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;

@Service
public class CommandJourneyService {

  private final CommandJourneyRepository commandJourneyRepository;
  private final FeatureRepository featureRepository;

  CommandJourneyService(CommandJourneyRepository commandJourneyRepository,
                        FeatureRepository featureRepository) {
    this.commandJourneyRepository = commandJourneyRepository;
    this.featureRepository = featureRepository;
  }

  @Transactional
  public CommandJourney createAndAssignCommandJourney(List<Feature> features) {
    CommandJourney journey = commandJourneyRepository.save(new CommandJourney());
    features.forEach(feature -> feature.setCommandJourney(journey));
    featureRepository.saveAll(features);
    return journey;
  }

  public CommandJourney getCommandJourneyOrThrow(UUID id) {
    return commandJourneyRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("CommandJourney %s not found".formatted(id)));
  }

  public List<Feature> getActiveFeatures(CommandJourney commandJourney) {
    return featureRepository.findAllByCommandJourney(commandJourney).stream()
        .filter(Feature::isActive)
        .toList();
  }
}
