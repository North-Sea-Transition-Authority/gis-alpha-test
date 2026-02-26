package uk.co.fivium.gisalphatest.testmap;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/test-map")
class TestMapController {

  @GetMapping
  public ModelAndView getMap() {
    return new ModelAndView("gis-alpha-test/testMap/testMap");
  }
}
