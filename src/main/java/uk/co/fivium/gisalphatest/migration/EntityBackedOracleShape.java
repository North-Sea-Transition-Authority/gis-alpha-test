package uk.co.fivium.gisalphatest.migration;

import java.util.List;
import java.util.Map;
import uk.co.fivium.gisalphatest.oracle.OracleBoundaryLine;
import uk.co.fivium.gisalphatest.oracle.OraclePolygonBoundary;
import uk.co.fivium.gisalphatest.oracle.OracleShape;
import uk.co.fivium.gisalphatest.oracle.OracleShapePolygon;

public record EntityBackedOracleShape(
    OracleShape shape,
    Map<OracleShapePolygon, List<OraclePolygonBoundary>> polygonToBoundary,
    Map<OraclePolygonBoundary, List<OracleBoundaryLine>> boundaryToLine
) {
}
