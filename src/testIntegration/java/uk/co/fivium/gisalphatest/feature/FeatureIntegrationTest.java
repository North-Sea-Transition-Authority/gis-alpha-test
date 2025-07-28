package uk.co.fivium.gisalphatest.feature;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.co.fivium.gisalphatest.util.TestUtil;

@SpringBootTest
@ActiveProfiles({"integration-test"})
class FeatureIntegrationTest {

  @Autowired
  private FeatureService featureService;

  @Test
  void createFeatureWithTwoPolygons() {
    var polygonCollectionFeature = featureService.createFeature(FeatureType.POLYGON_COLLECTION, TestUtil.ED50_SR);

    var polygon1 = featureService.createPolygon(polygonCollectionFeature);

    var polygon1ExteriorLine =
        featureService.createLine(polygonCollectionFeature, polygon1, LineNavigationType.LOXODROME, true);

    featureService.createPoint(polygonCollectionFeature, polygon1ExteriorLine, -3.2, 58.1666666666667);
    featureService.createPoint(polygonCollectionFeature, polygon1ExteriorLine, -3.2, 58.2);
    featureService.createPoint(polygonCollectionFeature, polygon1ExteriorLine, -3.25, 58.2);
    featureService.createPoint(polygonCollectionFeature, polygon1ExteriorLine, -3.25, 58.1666666666667);
    featureService.createPoint(polygonCollectionFeature, polygon1ExteriorLine, -3.2, 58.1666666666667);

    var polygon2 = featureService.createPolygon(polygonCollectionFeature);

    var polygon2ExteriorRingLine =
        featureService.createLine(polygonCollectionFeature, polygon1, LineNavigationType.LOXODROME, true);

    featureService.createPoint(polygonCollectionFeature, polygon2ExteriorRingLine, -3.2, 58.1666666666667);
    featureService.createPoint(polygonCollectionFeature, polygon2ExteriorRingLine, -3.2, 58.2);
    featureService.createPoint(polygonCollectionFeature, polygon2ExteriorRingLine, -3.25, 58.2);
    featureService.createPoint(polygonCollectionFeature, polygon2ExteriorRingLine, -3.25, 58.1666666666667);
    featureService.createPoint(polygonCollectionFeature, polygon2ExteriorRingLine, -3.2, 58.1666666666667);

    var polygon2InteriorRingLine =
        featureService.createLine(polygonCollectionFeature, polygon2, LineNavigationType.LOXODROME, false);

    featureService.createPoint(polygonCollectionFeature, polygon2InteriorRingLine, -3.22, 58.175);
    featureService.createPoint(polygonCollectionFeature, polygon2InteriorRingLine, -3.22, 58.19);
    featureService.createPoint(polygonCollectionFeature, polygon2InteriorRingLine, -3.23, 58.19);
    featureService.createPoint(polygonCollectionFeature, polygon2InteriorRingLine, -3.23, 58.175);
    featureService.createPoint(polygonCollectionFeature, polygon2InteriorRingLine, -3.22, 58.175);
  }
}
