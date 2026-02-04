package uk.co.fivium.gisalphatest.transformations;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Controller
public class SplitTestController {

  private final SplitService splitService;

  public SplitTestController(SplitService splitService) {
    this.splitService = splitService;
  }

  //GISA-41 split a shape with a cut line
  @GetMapping("/split")
  public ModelAndView splitPolygon() {
    String testCase = "Simple split test";
    splitService.testOracleShapeSplit(
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
    splitService.testOracleShapeSplit(
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
    splitService.testOracleShapeSplit(
        new OracleShapeCompositeKey(56975489, testCase),
        List.of(new OracleShapeCompositeKey(56977341, testCase), new OracleShapeCompositeKey(56977346, testCase)),
        testCase
    );

    return new ModelAndView("gis-alpha-test/layout/layout");
  }
}
