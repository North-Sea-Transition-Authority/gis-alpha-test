package uk.co.fivium.gisalphatest.mvc;

import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.branding.CustomerConfigurationProperties;
import uk.co.fivium.gisalphatest.branding.ServiceConfigurationProperties;

@Service
public class ControllerAdviceService {

  private final CustomerConfigurationProperties customerConfigurationProperties;
  private final ServiceConfigurationProperties serviceConfigurationProperties;

  public ControllerAdviceService(
      CustomerConfigurationProperties customerConfigurationProperties,
      ServiceConfigurationProperties serviceConfigurationProperties
  ) {
    this.customerConfigurationProperties = customerConfigurationProperties;
    this.serviceConfigurationProperties = serviceConfigurationProperties;
  }

  public void addBrandingModelAttributes(Object model) {
    var attributeConsumer = getAttributeConsumer(model);
    attributeConsumer.accept("serviceBranding", serviceConfigurationProperties);
    attributeConsumer.accept("customerBranding", customerConfigurationProperties);
  }

  public void addCommonUrlModelAttributes(Object model) {
    getAttributeConsumer(model).accept("serviceHomeUrl", "/");
  }

  public void addFooterLinkModelAttributes(Object model) {
    getAttributeConsumer(model).accept("accessibilityStatementUrl", "");
    getAttributeConsumer(model).accept("privacyUrl",
        "#"); // TODO xyz - privacy policy link, e.g. https://www.nstauthority.co.uk/footer/privacy-statement/ for NSTA
    getAttributeConsumer(model).accept("cookiePolicyUrl", "");
    getAttributeConsumer(model).accept("contactPageUrl", "");
  }

  private BiConsumer<String, Object> getAttributeConsumer(Object object) {
    if (object instanceof ModelAndView modelAndView) {
      return modelAndView::addObject;
    }

    if (object instanceof Model model) {
      return model::addAttribute;
    }

    throw new IllegalArgumentException(
        "Expected Model or ModelAndView but got %s".formatted(object.getClass().getSimpleName())
    );
  }
}
