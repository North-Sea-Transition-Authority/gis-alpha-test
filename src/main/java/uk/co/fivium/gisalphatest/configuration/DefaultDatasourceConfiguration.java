package uk.co.fivium.gisalphatest.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import uk.co.fivium.gisalphatest.GisAlphaTestApplication;

@Configuration
@EnableJpaRepositories(
    basePackageClasses = GisAlphaTestApplication.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "uk\\.co\\.fivium\\.gisalphatest\\.oracle\\..*"
    )
)
class DefaultDatasourceConfiguration {
}
