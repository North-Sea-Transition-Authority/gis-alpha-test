package uk.co.fivium.gisalphatest.oracle;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OraclePolygonBoundaryRepository extends ListCrudRepository<OraclePolygonBoundary, Long> {

  List<OraclePolygonBoundary> findAllByPolygonSidId(Long polygonSidId);
}
