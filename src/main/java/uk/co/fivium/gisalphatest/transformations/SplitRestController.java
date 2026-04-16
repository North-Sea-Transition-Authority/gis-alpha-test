package uk.co.fivium.gisalphatest.transformations;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;
import uk.co.fivium.gisalphatest.migration.Srs;
import uk.co.fivium.gisalphatest.transformations.command.CommandJourneyService;
import uk.co.fivium.gisalphatest.transformations.command.CommandResponse;
import uk.co.fivium.gisalphatest.transformations.command.TransformationCommandReceiver;

@RestController
@RequestMapping("/api/split")
public class SplitRestController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SplitRestController.class);

  private final GrpcClientService grpcClientService;
  private final TransformationCommandReceiver transformationCommandReceiver;
  private final CommandJourneyService commandJourneyService;

  public SplitRestController(GrpcClientService grpcClientService,
                             TransformationCommandReceiver transformationCommandReceiver,
                             CommandJourneyService commandJourneyService) {
    this.grpcClientService = grpcClientService;
    this.transformationCommandReceiver = transformationCommandReceiver;
    this.commandJourneyService = commandJourneyService;
  }

  @PostMapping("/execute")
  public CommandResponse splitFromMap(@RequestBody SplitFromMapRequestBody splitFromMapRequestBody) {
    LOGGER.info("Received request for '{}'", splitFromMapRequestBody);
    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(splitFromMapRequestBody.commandJourneyId());
    var features = commandJourneyService.getActiveFeatures(commandJourney);
    Srs srs = Srs.fromWkid(features.getFirst().getSrs());
    String cutterLine = grpcClientService.convertPointsToPolyline(splitFromMapRequestBody.originalSrsCoordinates(), srs);
    List<Feature> outputFeatures = transformationCommandReceiver.executeSplit(features, cutterLine);

    String journeyId = outputFeatures.isEmpty()
        ? null
        : outputFeatures.getFirst().getCreatedByCommand().getCommandJourney().getId().toString();

    return new CommandResponse(
        journeyId,
        outputFeatures.stream().map(feature -> feature.getId().toString()).toList()
    );
  }

  @PostMapping("/{journeyId}/undo")
  public CommandResponse undo(@PathVariable UUID journeyId) {
    var journey = commandJourneyService.getCommandJourneyOrThrow(journeyId);
    List<Feature> outputFeatures = transformationCommandReceiver.undo(journey);

    return new CommandResponse(
        journeyId.toString(),
        outputFeatures.stream().map(feature -> feature.getId().toString()).toList()
    );
  }

  @PostMapping("/{journeyId}/redo")
  public CommandResponse redo(@PathVariable UUID journeyId) {
    var journey = commandJourneyService.getCommandJourneyOrThrow(journeyId);
    List<Feature> outputFeatures = transformationCommandReceiver.redo(journey);

    return new CommandResponse(
        journeyId.toString(),
        outputFeatures.stream().map(feature -> feature.getId().toString()).toList()
    );
  }
}
