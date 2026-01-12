package uk.co.fivium.gisalphatest.migration;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisalphatest.feature.PolygonService;
import uk.co.fivium.gisalphatest.oracle.OracleShapeCompositeKey;

@Controller
@RequestMapping("/migrate")
public class MigrationTestController {

  private final MigrationService migrationService;
  private final PolygonService polygonService;

  MigrationTestController(
      MigrationService migrationService,
      PolygonService polygonService) {
    this.migrationService = migrationService;
    this.polygonService = polygonService;
  }

  @GetMapping
  public ModelAndView migrate() {
    //Migrate
    migrationService.migrate(
        List.of(
            new OracleShapeCompositeKey(23922223, "GISA-27"),
            new OracleShapeCompositeKey(23922738, "GISA-27")
        )
    );

    //Validate by building all the polygons based on the esri json lines
    polygonService.getPolygonsAsEsriJson(23922223, "GISA-27").forEach(System.out::println);
    polygonService.getPolygonsAsEsriJson(23922738, "GISA-27").forEach(System.out::println);;

    return new ModelAndView("gis-alpha-test/layout/layout");
  }
}
