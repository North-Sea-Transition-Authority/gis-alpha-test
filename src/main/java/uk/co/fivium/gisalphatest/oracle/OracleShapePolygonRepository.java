package uk.co.fivium.gisalphatest.oracle;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OracleShapePolygonRepository extends ListCrudRepository<OracleShapePolygon, Long> {

  public List<OracleShapePolygon> findAllByShapeSidId(Integer shapeSidId);
}