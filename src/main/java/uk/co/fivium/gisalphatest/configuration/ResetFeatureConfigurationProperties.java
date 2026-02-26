package uk.co.fivium.gisalphatest.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("user-testing")
@Validated
public record ResetFeatureConfigurationProperties (
    @NotBlank String resetToken
) {
}
