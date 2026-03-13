package uk.co.fivium.gisalphatest.transformations.command;

import java.util.List;

public record CommandResponse(String commandJourneyId, List<String> outputFeatureIds) {
}
