package uk.co.fivium.gisalphatest.feature;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shapes")
public record ShapesConfigProperties(Map<String, ShapeConfig> shapes) {

  public record ShapeConfig(
      String featureName,
      String testCase,
      List<LineConfig> lines
  ) {}

  public record LineConfig(
      String lineJson,
      LineNavigationType type,
      Integer ringNumber
  ) {}
}
