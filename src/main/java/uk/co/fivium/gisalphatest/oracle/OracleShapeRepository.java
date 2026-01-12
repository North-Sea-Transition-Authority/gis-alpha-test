package uk.co.fivium.gisalphatest.oracle;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OracleShapeRepository extends ListCrudRepository<OracleShape, OracleShapeCompositeKey> {

}
