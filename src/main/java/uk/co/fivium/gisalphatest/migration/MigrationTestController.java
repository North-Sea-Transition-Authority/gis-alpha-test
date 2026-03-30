package uk.co.fivium.gisalphatest.migration;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.featuremap.FeatureSelectionController;
import uk.co.fivium.gisalphatest.mvc.ReverseRouter;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Profile("development")
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
            new OracleShapeCompositeKey(56973868, "GISA-36 and GISA-38"),
            new OracleShapeCompositeKey(56973846, "GISA-36"),
            new OracleShapeCompositeKey(56973884, "GISA-38"),

            new OracleShapeCompositeKey(27912658, "GISA-65"),
            new OracleShapeCompositeKey(27912705, "GISA-65"),

            new OracleShapeCompositeKey(26282337, "GISA-146"),

            new OracleShapeCompositeKey(51662549, "GISA-115"),
            new OracleShapeCompositeKey(51663540, "GISA-115"),
            new OracleShapeCompositeKey(51662110, "GISA-115"),
            new OracleShapeCompositeKey(51662171, "GISA-115"),
            new OracleShapeCompositeKey(51661936, "GISA-115"),
            new OracleShapeCompositeKey(51662326, "GISA-115"),
            new OracleShapeCompositeKey(51662420, "GISA-115")
        ),
        true
    );

    // Migrate reference blocks after licenses
    migrationService.migrate(
        List.of(
            new OracleShapeCompositeKey(56222528, "GISA-144"),
            new OracleShapeCompositeKey(56241285, "GISA-144"),
            new OracleShapeCompositeKey(56226541, "GISA-146"),
            new OracleShapeCompositeKey(56226560, "GISA-146")
        ),
        false
    );

    migrationService.verifyAllLicensesAreInsideReferenceBlocks();
    migrationService.verifyAllChildFeaturesAreInsideParentFeatures();

//    Validate by building all the polygons based on the esri json lines
    polygonService.getPolygonsAsEsriJson(23922223, "GISA-27", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(23922738, "GISA-27", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(29960964, "GISA-63", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(56750115, "GISA-64", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(26239556, "GISA-49-simple", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(31965677, "GISA-49-simple", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(27240908, "GISA-49-coastline", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(56973797, "GISA-49-coastline", false).forEach(System.out::println);

    polygonService.getPolygonsAsEsriJson(5610939, "GISA-36 and GISA-38", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(56973868, "GISA-36 and GISA-38", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(56973846, "GISA-36", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(56973884, "GISA-38", false).forEach(System.out::println);


    polygonService.getPolygonsAsEsriJson(56222528, "GISA-144", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(56241285, "GISA-144", false).forEach(System.out::println);

    polygonService.getPolygonsAsEsriJson(26282337, "GISA-146", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(56226541, "GISA-146", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(56226560, "GISA-146", false).forEach(System.out::println);

    polygonService.getPolygonsAsEsriJson(51662549, "GISA-115", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(51663540, "GISA-115", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(51662110, "GISA-115", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(51662171, "GISA-115", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(51661936, "GISA-115", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(51662326, "GISA-115", false).forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(51662420, "GISA-115", false).forEach(System.out::println);

    migrationService.verifySubareasTopologicallyEqualToBlock(5610939, "GISA-36 and GISA-38");

    return ReverseRouter.redirect(on(FeatureSelectionController.class).renderSelectFeatures());
  }

  @GetMapping("/area")
  public ModelAndView migrateArea() {
    migrationService.migrateFeatureAreas();
    return new ModelAndView("gis-alpha-test/layout/layout");
  }
}
