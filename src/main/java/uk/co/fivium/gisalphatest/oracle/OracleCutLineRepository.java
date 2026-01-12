package uk.co.fivium.gisalphatest.oracle;

import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OracleCutLineRepository extends ListCrudRepository<OracleCutLine, Long> {
  Optional<OracleCutLine> findByTestCase(String testCase);
}
