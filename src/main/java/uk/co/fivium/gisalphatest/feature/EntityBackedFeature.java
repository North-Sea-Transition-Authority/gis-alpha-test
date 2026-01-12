package uk.co.fivium.gisalphatest.feature;

import java.util.List;
import java.util.Map;

public record EntityBackedFeature(
    Feature feature,
    Map<Polygon, List<Line>> polygonToLines
) {
}
