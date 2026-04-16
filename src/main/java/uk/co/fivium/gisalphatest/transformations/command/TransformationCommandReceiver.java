package uk.co.fivium.gisalphatest.transformations.command;

import java.util.ArrayList;
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
  private final FeatureService featureService;

  public TransformationCommandReceiver(SplitService splitService,
                                       FeatureRepository featureRepository,
                                       TransformationCommandRepository commandRepository,
                                       FeatureService featureService) {
    this.splitService = splitService;
    this.featureRepository = featureRepository;
    this.commandRepository = commandRepository;
    this.featureService = featureService;
  }

  /**
   * Executes a split transformation on a list of features using a cutter line.
   * Will link resulting split features to a {@link TransformationCommand} and deactivate only input features
   * that were actually split.
   * Calling this will hard delete any commands that have been undone on the same journey and their outputs.
   * @param inputFeatures The candidate features to split.
   * @param cutterLine The cutter line used for splitting.
   * @return The features that were created by the split.
   */
  @Transactional
  public List<Feature> executeSplit(List<Feature> inputFeatures,
                                    String cutterLine) {
    validateAllFeaturesAreInSameJourneyOrThrow(inputFeatures);

    List<Feature> affectedInputFeatures = new ArrayList<>();
    List<Feature> outputFeatures = new ArrayList<>();

    // Track split results per input so command history and active flags only apply to changed features.
    for (Feature inputFeature : inputFeatures) {
      List<Feature> splitResult = splitService.splitPolygon(inputFeature, cutterLine);
      if (!splitResult.isEmpty()) {
        affectedInputFeatures.add(inputFeature);
        outputFeatures.addAll(splitResult);
      }
    }

    if (outputFeatures.isEmpty()) {
      //exit early if no split took place
      return Collections.emptyList();
    }

    CommandJourney journey = inputFeatures.getFirst().getCommandJourney();

    clearUndoStack(journey);
    TransformationCommand command = createNewTransformationCommand(affectedInputFeatures, journey);

    // Mark only affected input features as inactive.
    affectedInputFeatures.forEach(feature -> feature.setActive(false));
    featureRepository.saveAll(affectedInputFeatures);

    // Link output features to the command.
    outputFeatures.forEach(feature -> {
      feature.setCreatedByCommand(command);
      feature.setCommandJourney(journey);
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

  private void validateAllFeaturesAreInSameJourneyOrThrow(List<Feature> features) {
    Set<UUID> distinctJourneyIds = features.stream()
        .map(Feature::getCommandJourney)
        .filter(Objects::nonNull)
        .map(CommandJourney::getId)
        .collect(Collectors.toSet());

    if (distinctJourneyIds.size() > 1) {
      throw new IllegalArgumentException("Cannot split features from different journeys in one command");
    }
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
