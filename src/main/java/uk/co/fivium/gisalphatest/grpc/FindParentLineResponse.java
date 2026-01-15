package uk.co.fivium.gisalphatest.grpc;

import java.util.List;
import java.util.Map;

public record FindParentLineResponse(Map<String, Integer> polylineToParentLineId,
                                     List<String> orphanLines) {
}
