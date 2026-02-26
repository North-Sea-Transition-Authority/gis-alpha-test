package uk.co.fivium.gisalphatest.feature;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.configuration.ResetFeatureConfigurationProperties;
import uk.co.fivium.gisalphatest.featuremap.FeatureSelectionController;
import uk.co.fivium.gisalphatest.mvc.ReverseRouter;

@Controller
@RequestMapping("/reset")
public class ResetFeaturesController {

  private final FeatureService featureService;

  private final String userTestingResetToken;

  ResetFeaturesController(
      FeatureService featureService,
      ResetFeatureConfigurationProperties resetFeatureConfigurationProperties
  ) {
    this.featureService = featureService;
    this.userTestingResetToken = resetFeatureConfigurationProperties.resetToken();
  }

  @GetMapping
  public ModelAndView showResetFeaturesForm(@ModelAttribute("form") ResetFeaturesForm form) {
    return new ModelAndView("gis-alpha-test/reset");
  }

  @PostMapping
  public ModelAndView resetFeatures(@ModelAttribute("form") ResetFeaturesForm form,
                                    BindingResult result) {
    if (StringUtils.isBlank(userTestingResetToken) || !userTestingResetToken.equals(form.getResetToken())) {
      result.rejectValue("resetToken", "error.invalid", "Invalid reset token");
      return new ModelAndView("gis-alpha-test/reset");
    }
    featureService.resetTablesForUserTesting();

    return ReverseRouter.redirect(on(FeatureSelectionController.class).renderSelectFeatures());
  }
}
