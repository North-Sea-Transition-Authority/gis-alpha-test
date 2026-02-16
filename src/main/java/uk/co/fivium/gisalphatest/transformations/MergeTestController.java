package uk.co.fivium.gisalphatest.transformations;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Controller
public class MergeTestController {

  private final MergeService mergeService;

  public MergeTestController(MergeService mergeService) {
    this.mergeService = mergeService;
  }

  @GetMapping("/merge")
  public ModelAndView merge() {
    var testCase = "GISA-42";
    mergeService.testOracleMerge(
        List.of(new OracleShapeCompositeKey(57367181, testCase), new OracleShapeCompositeKey(57367213, testCase)),
        new OracleShapeCompositeKey(57367683, testCase)
    );
    return new ModelAndView("gis-alpha-test/layout/layout");
  }
}
