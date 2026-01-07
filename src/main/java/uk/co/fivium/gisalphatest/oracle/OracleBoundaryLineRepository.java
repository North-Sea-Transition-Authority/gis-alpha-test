package uk.co.fivium.gisalphatest.oracle;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OracleBoundaryLineRepository extends CrudRepository<OracleBoundaryLine, Long> {
}
