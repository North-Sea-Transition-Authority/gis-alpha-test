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
}
