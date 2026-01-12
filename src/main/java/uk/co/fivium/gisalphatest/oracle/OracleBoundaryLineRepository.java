package uk.co.fivium.gisalphatest.oracle;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OracleBoundaryLineRepository extends ListCrudRepository<OracleBoundaryLine, Long> {

  List<OracleBoundaryLine> findAllByBoundarySidId(Long boundarySidId);
}
