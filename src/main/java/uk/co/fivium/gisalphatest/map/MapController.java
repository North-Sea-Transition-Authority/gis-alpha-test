package uk.co.fivium.gisalphatest.map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/map")
class MapController {

  @GetMapping
  public ModelAndView getMap() {
    return new ModelAndView("gis-alpha-test/map/map");
  }
}
