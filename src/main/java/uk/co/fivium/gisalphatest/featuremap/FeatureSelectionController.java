package uk.co.fivium.gisalphatest.featuremap;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.FeatureService;
import uk.co.fivium.gisalphatest.mvc.ReverseRouter;

@Controller
@RequestMapping("/select-features")
public class FeatureSelectionController {

  private final FeatureService featureService;

  public FeatureSelectionController(FeatureService featureService) {
    this.featureService = featureService;
  }

  @GetMapping
  public ModelAndView renderSelectFeatures() {
    return getModelAndView();
  }

  @PostMapping
  public ModelAndView processSelectFeatures(@ModelAttribute SelectFeatureForm form) {
    if (CollectionUtils.isEmpty(form.getFeatureIds())) {
      return getModelAndView();
    }
    return ReverseRouter.redirect(on(MapController.class).getFeatureMap(form.getFeatureIds()));
  }

  private ModelAndView getModelAndView() {
    return new ModelAndView("gis-alpha-test/selectFeatures/selectFeatures")
        .addObject("form", new SelectFeatureForm())
        .addObject("features", featureService.getFeatureIdNameMap());
  }
}
