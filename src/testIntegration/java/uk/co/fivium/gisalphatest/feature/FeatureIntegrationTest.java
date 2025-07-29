package uk.co.fivium.gisalphatest.feature;

import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.esri.core.geometry.ogc.OGCLineString;
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
  private ArcGisService arcGisService;

  @Test
  void createFeatureWithTwoPolygons() {
    var feature = featureService.createFeature(FeatureType.POLYGON_COLLECTION, TestUtil.ED50_SR);

    var polygon1 = featureService.createPolygon(feature);

    var polygon1ExteriorLine =
        featureService.createLine(feature, polygon1, LineNavigationType.LOXODROME, 0);

    featureService.createPoint(feature, polygon1ExteriorLine, -3.2, 58.1666666666667);
    featureService.createPoint(feature, polygon1ExteriorLine, -3.2, 58.2);
    featureService.createPoint(feature, polygon1ExteriorLine, -3.25, 58.2);
    featureService.createPoint(feature, polygon1ExteriorLine, -3.25, 58.1666666666667);
    featureService.createPoint(feature, polygon1ExteriorLine, -3.2, 58.1666666666667);

    var polygon2 = featureService.createPolygon(feature);

    var polygon2ExteriorRingLine =
        featureService.createLine(feature, polygon1, LineNavigationType.LOXODROME, 0);

    featureService.createPoint(feature, polygon2ExteriorRingLine, -3.2, 58.1666666666667);
    featureService.createPoint(feature, polygon2ExteriorRingLine, -3.2, 58.2);
    featureService.createPoint(feature, polygon2ExteriorRingLine, -3.25, 58.2);
    featureService.createPoint(feature, polygon2ExteriorRingLine, -3.25, 58.1666666666667);
    featureService.createPoint(feature, polygon2ExteriorRingLine, -3.2, 58.1666666666667);

    var polygon2InteriorRingLine =
        featureService.createLine(feature, polygon2, LineNavigationType.LOXODROME, 1);

    featureService.createPoint(feature, polygon2InteriorRingLine, -3.22, 58.175);
    featureService.createPoint(feature, polygon2InteriorRingLine, -3.22, 58.19);
    featureService.createPoint(feature, polygon2InteriorRingLine, -3.23, 58.19);
    featureService.createPoint(feature, polygon2InteriorRingLine, -3.23, 58.175);
    featureService.createPoint(feature, polygon2InteriorRingLine, -3.22, 58.175);
  }

  @Test
  void createTestShape12() throws Exception {
    var feature = featureService.createFeature(FeatureType.POLYGON, TestUtil.ED50_SR);

    var polygon = featureService.createPolygon(feature);

    var lineString1GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.76666666666667, 56.1666666666667], [2.68333333333333, 56.1666666666667] ] }";
    var lineString2GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.68333333333333, 56.1666666666667], [2.6, 56.05] ] }";
    var lineString3GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.6, 56.05], [2.66666666666667, 56.05], [2.8, 56.05] ] }";
    var lineString4GeoJson = "{ \"type\": \"LineString\", \"coordinates\": [ [2.8, 56.05], [2.76666666666667, 56.1666666666667] ] }";

    createLineFromGeoJson(polygon, LineNavigationType.LOXODROME, 0, lineString1GeoJson);
    createLineFromGeoJson(polygon, LineNavigationType.GEODESIC, 0, lineString2GeoJson);
    createLineFromGeoJson(polygon, LineNavigationType.GEODESIC, 0, lineString3GeoJson);
    createLineFromGeoJson(polygon, LineNavigationType.LOXODROME, 0, lineString4GeoJson);
  }

  private void createLineFromGeoJson(
      Polygon polygon,
      LineNavigationType navigationType,
      Integer ringNumber,
      String lineStringGeoJson
  ) throws Exception {
    var feature = polygon.getFeature();
    var line = featureService.createLine(feature, polygon, navigationType, ringNumber);

    var lineString = (OGCLineString) OGCGeometry.fromGeoJson(lineStringGeoJson);

    lineString.setSpatialReference(SpatialReference.create(feature.getSrs()));

    if (navigationType == LineNavigationType.GEODESIC) {
      lineString = arcGisService.densifyLine(lineString, true);
    }

    for (var i = 0; i < lineString.numPoints(); i++) {
      var point = lineString.pointN(i);
      featureService.createPoint(feature, line, point.X(), point.Y());
    }
  }
}
