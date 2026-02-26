package uk.co.fivium.gisalphatest.transformations;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Profile("development")
@Controller
public class SplitTestController {

  private final TestTransformationService testTransformationService;

  public SplitTestController(TestTransformationService testTransformationService) {
    this.testTransformationService = testTransformationService;
  }

  //GISA-41 split a shape with a cut line
  @GetMapping("/split")
  public ModelAndView splitPolygon() {
    String testCase = "Simple split test";
    testTransformationService.testOracleShapeSplit(
        new OracleShapeCompositeKey(57005318, testCase),
        List.of(new OracleShapeCompositeKey(57450328, testCase), new OracleShapeCompositeKey(57450333, testCase)),
        testCase
    );

    return new ModelAndView("gis-alpha-test/layout/layout");
  }

  //GISA-30 split a diagonal loxodrome shape
  @GetMapping("/split2")
  public ModelAndView splitPolygon2() {
    String testCase = "GISA-30";
    testTransformationService.testOracleShapeSplit(
        new OracleShapeCompositeKey(57015719, testCase),
        List.of(new OracleShapeCompositeKey(57021440, testCase), new OracleShapeCompositeKey(57021445, testCase)),
        testCase
    );

    return new ModelAndView("gis-alpha-test/layout/layout");
  }

  //GISA-69 split an offshore coastline shape
  @GetMapping("/split3")
  public ModelAndView splitPolygon3() {
    String testCase = "GISA-69";
    testTransformationService.testOracleShapeSplit(
        new OracleShapeCompositeKey(56975489, testCase),
        List.of(new OracleShapeCompositeKey(56977341, testCase), new OracleShapeCompositeKey(56977346, testCase)),
        testCase
    );

    return new ModelAndView("gis-alpha-test/layout/layout");
  }

  //GISA-68 Split a geodesic treaty boundary shape
  @GetMapping("/split4")
  public ModelAndView splitPolygon4() {
    String testCase = "GISA-68";
    testTransformationService.testOracleShapeSplit(
        new OracleShapeCompositeKey(57015751, testCase),
        List.of(new OracleShapeCompositeKey(57021455, testCase), new OracleShapeCompositeKey(57021450, testCase)),
        testCase
    );

    return new ModelAndView("gis-alpha-test/layout/layout");
  }

  //GISA-70 split an onshore shape
  @GetMapping("/split5")
  public ModelAndView splitPolygon5() {
    String testCase = "GISA-70";
    testTransformationService.testOracleShapeSplit(
        new OracleShapeCompositeKey(57011879, testCase),
        List.of(
            new OracleShapeCompositeKey(57015532, testCase),
            new OracleShapeCompositeKey(57015517, testCase),
            new OracleShapeCompositeKey(57015522, testCase)
        ),
        testCase
    );

    return new ModelAndView("gis-alpha-test/layout/layout");
  }

  //GISA-71 split a shape with a hole
  @GetMapping("/split6")
  public ModelAndView splitPolygon6() {
    String testCase = "GISA-71";
    testTransformationService.testOracleShapeSplit(
        new OracleShapeCompositeKey(56983740, testCase),
        List.of(
            new OracleShapeCompositeKey(56985006, testCase),
            new OracleShapeCompositeKey(56985011, testCase)
        ),
        testCase
    );

    return new ModelAndView("gis-alpha-test/layout/layout");
  }

  //GISA-72 split by creating new hole in a shape
  @GetMapping("/split7")
  public ModelAndView splitPolygon7() {
    String testCase = "GISA-72";
    testTransformationService.testOracleShapeSplit(
        new OracleShapeCompositeKey(57475726, testCase),
        List.of(
            new OracleShapeCompositeKey(57476564, testCase),
            new OracleShapeCompositeKey(57476569, testCase)
        ),
        testCase
    );

    return new ModelAndView("gis-alpha-test/layout/layout");
  }
}
