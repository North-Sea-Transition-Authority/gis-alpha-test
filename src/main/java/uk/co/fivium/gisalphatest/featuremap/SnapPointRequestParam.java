package uk.co.fivium.gisalphatest.featuremap;

import java.math.BigDecimal;

public record SnapPointRequestParam(
    BigDecimal minLon,
    BigDecimal minLat,
    BigDecimal maxLon,
    BigDecimal maxLat,
    Integer srsWkid
) {
}
