package uk.co.fivium.gisalphatest.migration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MigrationRestApiService {

  private final RestClient restClient;

  MigrationRestApiService(RestClient restClient) {
    this.restClient = restClient;
  }

  public String geoJsonLineToEsriJsonLine(String geoJson) {
    var response = restClient.post()
        .uri(URI.create("http://localhost:8082/api/geo-json-to-esri-json/line"))
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(geoJson)
        .retrieve()
        .body(EsriLineResponse.class);
    return response.esriJson();
  }

  record EsriLineResponse(
      @JsonProperty("esriJson") String esriJson
  ) {
  }
}
