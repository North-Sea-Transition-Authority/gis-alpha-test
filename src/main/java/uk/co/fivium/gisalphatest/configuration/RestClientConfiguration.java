package uk.co.fivium.gisalphatest.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class RestClientConfiguration {

  @Bean
  RestClient restClient() {
    return RestClient.builder().build();
  }
}