package uk.co.fivium.gisalphatest.transformations;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Profile("development")
@Controller
public class MergeTestController {

  private final TestTransformationService testTransformationService;

  public MergeTestController(TestTransformationService testTransformationService) {
    this.testTransformationService = testTransformationService;
  }

  @GetMapping("/merge")
  public ModelAndView merge() {
    var testCase = "GISA-42";
    testTransformationService.testOracleMerge(
        List.of(new OracleShapeCompositeKey(57367181, testCase), new OracleShapeCompositeKey(57367213, testCase)),
        new OracleShapeCompositeKey(57367683, testCase)
    );
    return new ModelAndView("gis-alpha-test/layout/layout");
  }

  // GISA-31 merge a geodesic treaty boundary shape
  @GetMapping("/merge2")
  public ModelAndView merge2() {
    var testCase = "GISA-31";
    testTransformationService.testOracleMerge(
        List.of(
            new OracleShapeCompositeKey(57070124, testCase),
            new OracleShapeCompositeKey(57070189, testCase)
        ),
        new OracleShapeCompositeKey(57070711, testCase)
    );
    return new ModelAndView("gis-alpha-test/layout/layout");
  }

  // GISA-74 merge an offshore coastline shape
  @GetMapping("/merge3")
  public ModelAndView merge3() {
    var testCase = "GISA-74";
    testTransformationService.testOracleMerge(
        List.of(
            new OracleShapeCompositeKey(57368699, testCase),
            new OracleShapeCompositeKey(57367793, testCase)
        ),
        new OracleShapeCompositeKey(57442868, testCase)
    );
    return new ModelAndView("gis-alpha-test/layout/layout");
  }

  // GISA-75 merge an offshore coastline shape
  @GetMapping("/merge4")
  public ModelAndView merge4() {
    var testCase = "GISA-75";
    testTransformationService.testOracleMerge(
        List.of(
            new OracleShapeCompositeKey(57071199, testCase),
            new OracleShapeCompositeKey(57071101, testCase)
        ),
        new OracleShapeCompositeKey(57071424, testCase)
    );
    return new ModelAndView("gis-alpha-test/layout/layout");
  }

  // GISA-76 merge a shape with a hole
  @GetMapping("/merge5")
  public ModelAndView merge5() {
    var testCase = "GISA-76";
    testTransformationService.testOracleMerge(
        List.of(
            new OracleShapeCompositeKey(57367245, testCase),
            new OracleShapeCompositeKey(57367277, testCase)
        ),
        new OracleShapeCompositeKey(57367748, testCase)
    );
    return new ModelAndView("gis-alpha-test/layout/layout");
  }
}
