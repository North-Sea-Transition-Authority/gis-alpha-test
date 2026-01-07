package uk.co.fivium.gisalphatest.oracle;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OracleShapePolygonRepository extends CrudRepository<OracleShapePolygon, Long> {
}