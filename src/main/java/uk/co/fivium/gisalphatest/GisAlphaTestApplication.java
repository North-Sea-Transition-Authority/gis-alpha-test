package uk.co.fivium.gisalphatest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
class GisAlphaTestApplication {

  public static void main(String[] args) {
    SpringApplication.run(GisAlphaTestApplication.class, args);
  }
}
