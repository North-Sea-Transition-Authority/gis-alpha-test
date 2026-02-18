package uk.co.fivium.gisalphatest.migration;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Controller
@RequestMapping
public class MigrationTestController {

  private final MigrationService migrationService;
  private final PolygonService polygonService;

  MigrationTestController(
      MigrationService migrationService,
      PolygonService polygonService) {
    this.migrationService = migrationService;
    this.polygonService = polygonService;
  }

  @GetMapping("/migrate")
  public ModelAndView migrate() {
    //Migrate
    migrationService.migrate(
        List.of(
            new OracleShapeCompositeKey(23922223, "GISA-27"),
            new OracleShapeCompositeKey(23922738, "GISA-27"),
            new OracleShapeCompositeKey(29960964, "GISA-63"),
            new OracleShapeCompositeKey(56750115, "GISA-64"),
            new OracleShapeCompositeKey(26239556, "GISA-49-simple"),
            new OracleShapeCompositeKey(31965677, "GISA-49-simple"),
            new OracleShapeCompositeKey(27240908, "GISA-49-coastline"),
            new OracleShapeCompositeKey(56973797, "GISA-49-coastline"),

            new OracleShapeCompositeKey(5610939, "GISA-36 and GISA-38"),
//            new OracleShapeCompositeKey(56973868, "GISA-36 and GISA-38")
            new OracleShapeCompositeKey(56973846, "GISA-36")
        )
    );

    migrationService.verifyAllChildFeaturesAreInsideParentFeatures();

    //Validate by building all the polygons based on the esri json lines
    polygonService.getPolygonsAsEsriJson(23922223, "GISA-27", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(23922738, "GISA-27", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(29960964, "GISA-63", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(56750115, "GISA-64", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(26239556, "GISA-49-simple", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(31965677, "GISA-49-simple", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(27240908, "GISA-49-coastline", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(56973797, "GISA-49-coastline", false).forEach(System.out::println);

    polygonService.getPolygonsAsEsriJson(5610939, "GISA-36 and GISA-38", false).forEach(System.out::println);
//    polygonService.getPolygonsAsEsriJson(56973868, "GISA-36 and GISA-38", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(56973846, "GISA-36", false).forEach(System.out::println);

    return new ModelAndView("gis-alpha-test/layout/layout");
  }

  @GetMapping("/area")
  public ModelAndView migrateArea() {
    migrationService.migrateFeatureAreas();
    return new ModelAndView("gis-alpha-test/layout/layout");
  }
}
