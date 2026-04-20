package uk.co.fivium.gisalphatest.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.grpc.GrpcClientService;

@Service
public class PolygonService {

  private final FeatureService featureService;
  private final GrpcClientService grpcClientService;
  private final LineRepository lineRepository;

  public PolygonService(
      FeatureService featureService,
      GrpcClientService grpcClientService,
      LineRepository lineRepository) {
    this.featureService = featureService;

    this.grpcClientService = grpcClientService;
    this.lineRepository = lineRepository;
  }

  /**
   * Generates all the EsriJSON polygons for a given feature.
   *
   * @param shapeSidId the shapeSidId of the feature
   * @param testCase the testcase of the feature
   * @param densifyLoxodromeLines true if the loxodrome lines on the feature should be densified before the polygon is built.
   * @return a list of EsriJSON polygons for the given feature
   */
  public List<String> getPolygonsAsEsriJson(Integer shapeSidId, String testCase, boolean densifyLoxodromeLines) {
    var entityBackedFeature = featureService.getEntityBackedFeature(shapeSidId, testCase);
    return getPolygonsAsEsriJson(entityBackedFeature, densifyLoxodromeLines);
  }

  /**
   * Generates all the EsriJSON polygons for a given feature.
   *
   * @param feature the feature whose polygons will be built as EsriJSON.
   * @param densifyLoxodromeLines true if the loxodrome lines on the feature should be densified before the polygon is built (usually true if calculating the area).
   * @return a list of EsriJSON polygons for the given feature
   */
  public List<String> getPolygonsAsEsriJson(Feature feature, boolean densifyLoxodromeLines) {
    var entityBackedFeature = featureService.getEntityBackedFeature(feature);
    return getPolygonsAsEsriJson(entityBackedFeature, densifyLoxodromeLines);
  }

  /**
   *  Generates all the EsriJSON polygons for a given polygon.
   * @param polygon the polygon whose EsriJSON representation will be built.
   * @param srs the srs of the polygon
   * @param densifyLoxodromeLines true if the loxodrome lines on the feature should be densified before the polygon is built (usually true if calculating the area).
   * @return EsriJSON polygon of the given polygon
   */
  public String getPolygonAsEsriJson(Polygon polygon, Integer srs, boolean densifyLoxodromeLines) {
    var lineJsons = lineRepository.findAllByPolygon(polygon)
        .stream()
        .sorted(Comparator.comparing(Line::getRingConnectionOrder))
        .map(line -> {
          if (densifyLoxodromeLines && LineNavigationType.LOXODROME.equals(line.getNavigationType())) {
            return grpcClientService.densifyLoxodromePolyline(line.getLineJson());
          }
          return line.getLineJson();
        })
        .toList();
    return grpcClientService.buildPolygon(lineJsons, srs);
  }

  private List<String> getPolygonsAsEsriJson(EntityBackedFeature entityBackedFeature, boolean densifyLoxodromeLines) {
    List<String> polygonsAsEsriJson = new ArrayList<>();
    var srs = entityBackedFeature.feature().getSrs();

    for (var entry : entityBackedFeature.polygonToLines().entrySet()) {
      var lineJsons = entry.getValue()
          .stream()
          .sorted(Comparator.comparing(Line::getRingConnectionOrder))
          .map(line -> {
            if (densifyLoxodromeLines && LineNavigationType.LOXODROME.equals(line.getNavigationType())) {
              return grpcClientService.densifyLoxodromePolyline(line.getLineJson());
            }
            return line.getLineJson();
          })
          .toList();
      polygonsAsEsriJson.add(grpcClientService.buildPolygon(lineJsons, srs));

    }

    return polygonsAsEsriJson;
  }

  public List<String> getPolygonsAsEsriJsonProjected(List<String> esriJsonPolygons) {
    return grpcClientService.projectedPolygonsToWgs84(esriJsonPolygons);
  }
}
