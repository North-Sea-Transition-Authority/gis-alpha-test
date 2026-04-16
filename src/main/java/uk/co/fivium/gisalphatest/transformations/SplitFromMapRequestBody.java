package uk.co.fivium.gisalphatest.transformations;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SplitFromMapRequestBody(List<List<List<BigDecimal>>> originalSrsCoordinates,
                                      UUID commandJourneyId) {
}
