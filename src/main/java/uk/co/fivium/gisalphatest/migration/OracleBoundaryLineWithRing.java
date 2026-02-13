package uk.co.fivium.gisalphatest.migration;

import uk.co.fivium.gisalphatest.oracle.OracleBoundaryLine;

public record OracleBoundaryLineWithRing(OracleBoundaryLine oracleBoundaryLine, int ringNumber) {
}
