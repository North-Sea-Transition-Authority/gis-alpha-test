package uk.co.fivium.gisalphatest.feature;

import static org.assertj.core.api.Assertions.assertThat;

import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCLineString;
import com.esri.core.geometry.ogc.OGCPolygon;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.co.fivium.gisalphatest.arcgis.ArcGisService;
import uk.co.fivium.gisalphatest.util.TestUtil;

@SpringBootTest
@ActiveProfiles({"integration-test"})
class FeatureIntegrationTest {

  @Autowired
  private FeatureService featureService;

  @Autowired
  private FeatureEsriConversionService featureEsriConversionService;

  @Autowired
  private ArcGisService arcGisService;

  @Test
  void createFeatureWithTwoPolygons() {
    var feature = featureService.createFeature(FeatureType.POLYGON_COLLECTION, TestUtil.ED50_SR);

    var polygon1 = featureService.createPolygon(feature);

    var polygon1ExteriorLine =
        featureService.createLine(feature, polygon1, LineNavigationType.LOXODROME, 0, 0);

    featureService.createPoint(feature, polygon1ExteriorLine, 0, -3.2, 58.1666666666667);
    featureService.createPoint(feature, polygon1ExteriorLine, 1, -3.2, 58.2);
    featureService.createPoint(feature, polygon1ExteriorLine, 2, -3.25, 58.2);
    featureService.createPoint(feature, polygon1ExteriorLine, 3, -3.25, 58.1666666666667);
    featureService.createPoint(feature, polygon1ExteriorLine, 4, -3.2, 58.1666666666667);

    var polygon2 = featureService.createPolygon(feature);

    var polygon2ExteriorRingLine =
        featureService.createLine(feature, polygon1, LineNavigationType.LOXODROME, 0, 0);

    featureService.createPoint(feature, polygon2ExteriorRingLine, 0, -3.2, 58.1666666666667);
    featureService.createPoint(feature, polygon2ExteriorRingLine, 1, -3.2, 58.2);
    featureService.createPoint(feature, polygon2ExteriorRingLine, 2, -3.25, 58.2);
    featureService.createPoint(feature, polygon2ExteriorRingLine, 3, -3.25, 58.1666666666667);
    featureService.createPoint(feature, polygon2ExteriorRingLine, 4, -3.2, 58.1666666666667);

    var polygon2InteriorRingLine =
        featureService.createLine(feature, polygon2, LineNavigationType.LOXODROME, 1, 0);

    featureService.createPoint(feature, polygon2InteriorRingLine, 0, -3.22, 58.175);
    featureService.createPoint(feature, polygon2InteriorRingLine, 1, -3.22, 58.19);
    featureService.createPoint(feature, polygon2InteriorRingLine, 2, -3.23, 58.19);
    featureService.createPoint(feature, polygon2InteriorRingLine, 3, -3.23, 58.175);
    featureService.createPoint(feature, polygon2InteriorRingLine, 4, -3.22, 58.175);
  }

  @Test
  void createTestShape12() throws Exception {
    var feature = featureService.createFeature(FeatureType.POLYGON, TestUtil.ED50_SR);

    var polygon = featureService.createPolygon(feature);

    var lineString1GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.76666666666667, 56.1666666666667], [2.68333333333333, 56.1666666666667] ] }";
    var lineString2GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.68333333333333, 56.1666666666667], [2.6, 56.05] ] }";
    var lineString3GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.6, 56.05], [2.66666666666667, 56.05], [2.8, 56.05] ] }";
    var lineString4GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.8, 56.05], [2.76666666666667, 56.1666666666667] ] }";

    createLineFromGeoJson(polygon, LineNavigationType.LOXODROME, 0, 0, lineString1GeoJson);
    createLineFromGeoJson(polygon, LineNavigationType.GEODESIC, 0, 1, lineString2GeoJson);
    createLineFromGeoJson(polygon, LineNavigationType.GEODESIC, 0, 2, lineString3GeoJson);
    createLineFromGeoJson(polygon, LineNavigationType.LOXODROME, 0, 3, lineString4GeoJson);
  }

  @Test
  void cutTestShape12WithShape13CutLines() throws Exception {
    var feature = featureService.createFeature(FeatureType.POLYGON, TestUtil.ED50_SR);

    var polygon = featureService.createPolygon(feature);

    var shape12LineString1GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.76666666666667, 56.1666666666667], [2.68333333333333, 56.1666666666667] ] }";
    var shape12LineString2GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.68333333333333, 56.1666666666667], [2.6, 56.05] ] }";
    var shape12LineString3GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.6, 56.05], [2.66666666666667, 56.05], [2.8, 56.05] ] }";
    var shape12LineString4GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.8, 56.05], [2.76666666666667, 56.1666666666667] ] }";

    createLineFromGeoJson(polygon, LineNavigationType.LOXODROME, 0, 0, shape12LineString1GeoJson);
    createLineFromGeoJson(polygon, LineNavigationType.GEODESIC, 0, 1, shape12LineString2GeoJson);
    createLineFromGeoJson(polygon, LineNavigationType.GEODESIC, 0, 2, shape12LineString3GeoJson);
    createLineFromGeoJson(polygon, LineNavigationType.LOXODROME, 0, 3, shape12LineString4GeoJson);

    var shape13CutterLine1 = (OGCLineString) OGCGeometry.fromGeoJson("{ \"type\": \"LineString\", \"coordinates\": [ [2.625, 56.0916666666667], [2.8, 56.0916666666667] ] }");

    var ogcPolygon = featureEsriConversionService.toOgc(polygon);

    var cutPolygons = arcGisService.cutPolygon(ogcPolygon, shape13CutterLine1);
    assertThat(cutPolygons.size()).isEqualTo(2);
  }

  private List<OGCPolygon> getCutTestShape12WithShape13CutLinesExpectOutputPolygons() throws Exception {
    var feature = featureService.createFeature(FeatureType.POLYGON_COLLECTION, TestUtil.ED50_SR);

    var polygon1 = featureService.createPolygon(feature);

    var polygon1LineString1GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.78809527777778, 56.0916666666667], [2.76666666666667, 56.1666666666667] ] }";
    var polygon1LineString2GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.76666666666667, 56.1666666666667], [2.68333333333333, 56.1666666666667] ] }";
    var polygon1LineString3GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.68333333333333, 56.1666666666667], [2.62969944444444, 56.0916666666667] ] }";
    var polygon1LineString4GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.62969944444444, 56.0916666666667], [2.71666666666667, 56.0916666666667] ] }";
    var polygon1LineString5GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.71666666666667, 56.0916666666667], [2.78809527777778, 56.0916666666667] ] }";

    createLineFromGeoJson(polygon1, LineNavigationType.LOXODROME, 0, 0, polygon1LineString1GeoJson);
    createLineFromGeoJson(polygon1, LineNavigationType.LOXODROME, 0, 1, polygon1LineString2GeoJson);
    createLineFromGeoJson(polygon1, LineNavigationType.GEODESIC, 0, 2, polygon1LineString3GeoJson);
    createLineFromGeoJson(polygon1, LineNavigationType.LOXODROME, 0, 3, polygon1LineString4GeoJson);
    createLineFromGeoJson(polygon1, LineNavigationType.LOXODROME, 0, 4, polygon1LineString5GeoJson);

    return List.of(featureEsriConversionService.toOgc(polygon1));
  }

  private void createLineFromGeoJson(
      Polygon polygon,
      LineNavigationType navigationType,
      int ringNumber,
      int ringConnectionOrder,
      String lineStringGeoJson
  ) throws Exception {
    var feature = polygon.getFeature();
    var line = featureService.createLine(feature, polygon, navigationType, ringNumber, ringConnectionOrder);

    var lineString = (OGCLineString) OGCGeometry.fromGeoJson(lineStringGeoJson);

    lineString.setSpatialReference(SpatialReference.create(feature.getSrs()));

    if (navigationType == LineNavigationType.GEODESIC) {
      lineString = arcGisService.densifyLine(lineString, true);
    }

    for (var i = 0; i < lineString.numPoints(); i++) {
      var point = lineString.pointN(i);
      featureService.createPoint(feature, line, i, point.X(), point.Y());
    }
  }
}
