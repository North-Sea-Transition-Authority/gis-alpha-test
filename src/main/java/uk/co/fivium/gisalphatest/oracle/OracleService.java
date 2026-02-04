package uk.co.fivium.gisalphatest.oracle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.migration.EntityBackedOracleShape;

@Service
public class OracleService {

  private final OracleBoundaryLineRepository oracleLineRepository;
  private final OraclePolygonBoundaryRepository oracleBoundaryRepository;
  private final OracleShapePolygonRepository oraclePolygonRepository;
  private final OracleShapeRepository oracleShapeRepository;

  OracleService(
      OracleBoundaryLineRepository oracleLineRepository,
      OraclePolygonBoundaryRepository oracleBoundaryRepository,
      OracleShapePolygonRepository oraclePolygonRepository,
      OracleShapeRepository oracleShapeRepository
  ) {
    this.oracleLineRepository = oracleLineRepository;
    this.oracleBoundaryRepository = oracleBoundaryRepository;
    this.oraclePolygonRepository = oraclePolygonRepository;
    this.oracleShapeRepository = oracleShapeRepository;
  }

  /**
   *
   * @return returns a list of records that maps lines to boundaries, and boundaries to polygons for a given shape.
   */
  public List<EntityBackedOracleShape> getEntityBackedOracleShapes(Collection<OracleShapeCompositeKey> ids) {
    List<EntityBackedOracleShape> entityBackedOracleShapes = new ArrayList<>();

    var oracleShapes = oracleShapeRepository.findAllById(ids)
        .stream()
        .sorted(Comparator.nullsFirst(Comparator.comparing(OracleShape::getShapeSidId)))
        .toList();

    for (var oracleShape : oracleShapes) {
      Map<OracleShapePolygon, List<OraclePolygonBoundary>> polygonToBoundary = new HashMap<>();
      Map<OraclePolygonBoundary, List<OracleBoundaryLine>> boundaryToLine = new HashMap<>();

      var oraclePolygons = getPolygonsByShapeSidId(oracleShape.getShapeSidId());
      for (var oraclePolygon : oraclePolygons) {
        var oracleBoundaries = getBoundariesByPolygonSidId(oraclePolygon.getPolygonSidId().longValue());

        polygonToBoundary.put(oraclePolygon, oracleBoundaries);

        for (var oracleBoundary : oracleBoundaries) {
          var lines = getLinesByBoundarySidId(oracleBoundary.getBoundarySidId())
              .stream()
              .sorted(Comparator.comparing(OracleBoundaryLine::getConnectionOrder))
              .toList();
          boundaryToLine.put(oracleBoundary, lines);
        }
      }
      entityBackedOracleShapes.add(
          new EntityBackedOracleShape(
              oracleShape,
              polygonToBoundary,
              boundaryToLine
          )
      );
    }
    return entityBackedOracleShapes;
  }

  public double getOracleShapeArea(OracleShapeCompositeKey shapeId) {
    return oracleShapeRepository.findById(shapeId)
        .map(OracleShape::getShareAreaM2).
        orElse((double) 0);
  }

  private List<OracleShapePolygon> getPolygonsByShapeSidId(Integer shapeSidId) {
    return oraclePolygonRepository.findAllByShapeSidId(shapeSidId);
  }

  private List<OraclePolygonBoundary> getBoundariesByPolygonSidId(Long polygonSidId) {
    return oracleBoundaryRepository.findAllByPolygonSidId(polygonSidId);
  }

  private List<OracleBoundaryLine> getLinesByBoundarySidId(Long boundarySidId) {
    return oracleLineRepository.findAllByBoundarySidId(boundarySidId);
  }
}
