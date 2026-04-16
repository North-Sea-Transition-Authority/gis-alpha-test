package uk.co.fivium.gisalphatest.featuremap;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.Feature;
import uk.co.fivium.gisalphatest.feature.FeatureService;
import uk.co.fivium.gisalphatest.mvc.ReverseRouter;
import uk.co.fivium.gisalphatest.transformations.command.CommandJourney;
import uk.co.fivium.gisalphatest.transformations.command.CommandJourneyService;

@Controller
@RequestMapping("/select-features")
public class FeatureSelectionController {

  private final FeatureService featureService;
  private final CommandJourneyService commandJourneyService;

  public FeatureSelectionController(FeatureService featureService,
                                    CommandJourneyService commandJourneyService) {
    this.featureService = featureService;
    this.commandJourneyService = commandJourneyService;
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

    List<Feature> features = featureService.getFeatures(form.getFeatureIds());
    Set<CommandJourney> journeys = features.stream()
        .map(Feature::getCommandJourney)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    CommandJourney commandJourney = switch (journeys.size()) {
      case 0 ->
         commandJourneyService.createAndAssignCommandJourney(features);
      case 1 ->
          journeys.stream().findFirst().get();
      default -> throw new IllegalArgumentException("Selected features belong to multiple journeys");
    };


    return ReverseRouter.redirect(on(MapController.class).getFeatureMap(commandJourney.getId()));
  }

  private ModelAndView getModelAndView() {
    return new ModelAndView("gis-alpha-test/selectFeatures/selectFeatures")
        .addObject("form", new SelectFeatureForm())
        .addObject("features", featureService.getFeatureIdNameMap());
  }
}
