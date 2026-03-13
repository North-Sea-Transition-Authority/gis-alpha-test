package uk.co.fivium.gisalphatest.transformations.command;

import jakarta.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureRepository;
import uk.co.fivium.gisalphatest.feature.FeatureService;
import uk.co.fivium.gisalphatest.transformations.SplitService;
import uk.co.fivium.gisalphatest.transformations.TransformationType;

@Service
public class TransformationCommandReceiver {

  private final SplitService splitService;
  private final FeatureRepository featureRepository;
  private final TransformationCommandRepository commandRepository;
  private final CommandJourneyRepository journeyRepository;
  private final FeatureService featureService;

  public TransformationCommandReceiver(SplitService splitService,
                                       FeatureRepository featureRepository,
                                       TransformationCommandRepository commandRepository,
                                       CommandJourneyRepository journeyRepository,
                                       FeatureService featureService) {
    this.splitService = splitService;
    this.featureRepository = featureRepository;
    this.commandRepository = commandRepository;
    this.journeyRepository = journeyRepository;
    this.featureService = featureService;
  }

  /**
   * Executes a split transformation on a list of features using a cutter line.
   * Will link the resulting features to a {@link TransformationCommand} and deactivate the input features.
   * Calling this will hard delete any commands that have been undone on the same journey and their outputs.
   * @param inputFeatures The list of features to be split. They will be deactivated after a successful split.
   * @param cutterLine The cutter line used for splitting.
   * @return The list of resulting features after splitting. They will be linked to a command and be set to active. Empty list if no split took place.
   */
  @Transactional
  public List<Feature> executeSplit(List<Feature> inputFeatures,
                                    String cutterLine) {
    Optional<UUID> journeyIdOptional = validateAllFeaturesAreInSameJourneyOrThrow(inputFeatures);

    // Perform the splits
    List<Feature> outputFeatures = inputFeatures.stream()
        .flatMap(feature -> splitService.splitPolygon(feature, cutterLine).stream())
        .toList();

    if (outputFeatures.isEmpty()) {
      //exit early if no split took place
      return Collections.emptyList();
    }

    CommandJourney journey = journeyIdOptional.isEmpty()
        ? journeyRepository.save(new CommandJourney())
        : journeyRepository.findById(journeyIdOptional.get()).orElseThrow(EntityNotFoundException::new);

    clearUndoStack(journey);
    TransformationCommand command = createNewTransformationCommand(inputFeatures, journey);

    // Mark input features as inactive
    inputFeatures.forEach(feature -> feature.setActive(false));
    featureRepository.saveAll(inputFeatures);

    // Link output features to the command
    outputFeatures.forEach(feature -> {
      feature.setCreatedByCommand(command);
      feature.setActive(true);
    });
    featureRepository.saveAll(outputFeatures);

    return outputFeatures;
  }

  @Transactional
  public List<Feature> undo(CommandJourney journey) {
    Optional<TransformationCommand> currentActiveCommandOpt = commandRepository
        .findFirstByCommandJourneyAndStatusOrderByCommandOrderDesc(journey, CommandStatus.ACTIVE);

    if (currentActiveCommandOpt.isEmpty()) {
      return Collections.emptyList();
    }

    var currentActiveCommand = currentActiveCommandOpt.get();

    // Deactivate output features
    List<Feature> outputFeatures = featureRepository.findAllByCreatedByCommand(currentActiveCommand);
    outputFeatures.forEach(feature -> feature.setActive(false));
    featureRepository.saveAll(outputFeatures);

    // Reactivate input features
    List<Feature> inputFeatures = featureRepository.findAllById(currentActiveCommand.getInputIds());
    inputFeatures.forEach(feature -> feature.setActive(true));
    featureRepository.saveAll(inputFeatures);

    currentActiveCommand.setStatus(CommandStatus.UNDONE);
    commandRepository.save(currentActiveCommand);

    return inputFeatures;
  }

  @Transactional
  public List<Feature> redo(CommandJourney journey) {
    Optional<TransformationCommand> nextUndoneOpt = commandRepository
        .findFirstByCommandJourneyAndStatusOrderByCommandOrderAsc(journey, CommandStatus.UNDONE);

    if (nextUndoneOpt.isEmpty()) {
      return Collections.emptyList();
    }

    var nextUndoneCommand = nextUndoneOpt.get();

    // Deactivate input features
    List<Feature> inputFeatures = featureRepository.findAllById(nextUndoneCommand.getInputIds());
    inputFeatures.forEach(feature -> feature.setActive(false));
    featureRepository.saveAll(inputFeatures);

    // Reactivate output features
    List<Feature> outputFeatures = featureRepository.findAllByCreatedByCommand(nextUndoneCommand);
    outputFeatures.forEach(feature -> feature.setActive(true));
    featureRepository.saveAll(outputFeatures);

    nextUndoneCommand.setStatus(CommandStatus.ACTIVE);
    commandRepository.save(nextUndoneCommand);

    return outputFeatures;
  }

  private Optional<UUID> validateAllFeaturesAreInSameJourneyOrThrow(List<Feature> features) {
    Set<UUID> distinctJourneyIds = features.stream()
        .map(Feature::getCreatedByCommand)
        .filter(Objects::nonNull)
        .map(command -> command.getCommandJourney().getId())
        .collect(Collectors.toSet());

    if (distinctJourneyIds.size() > 1) {
      throw new IllegalArgumentException("Cannot split features from different journeys in one command");
    }
    return distinctJourneyIds.stream().findFirst();
  }

  /**
   * Hard-delete all commands with status UNDONE and their output features
   */
  private void clearUndoStack(CommandJourney journey) {
    List<TransformationCommand> undoneCommands = commandRepository.findByCommandJourneyAndStatus(journey, CommandStatus.UNDONE);
    for (TransformationCommand undone : undoneCommands) {
      featureService.deleteAll(featureRepository.findAllByCreatedByCommand(undone));
      commandRepository.delete(undone);
    }
  }

  private TransformationCommand createNewTransformationCommand(List<Feature> features,
                                                               CommandJourney journey) {
    int nextOrder = commandRepository.findMaxCommandOrderByJourney(journey)
        .map(max -> max + 1)
        .orElse(1);
    TransformationCommand command = new TransformationCommand();
    command.setCommandJourney(journey);
    command.setInputIds(features.stream().map(Feature::getId).collect(Collectors.toSet()));
    command.setStatus(CommandStatus.ACTIVE);
    command.setType(TransformationType.SPLIT);
    command.setCommandOrder(nextOrder);
    commandRepository.save(command);
    return command;
  }
}
