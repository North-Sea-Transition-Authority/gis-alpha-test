package uk.co.fivium.gisalphatest.feature;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface LineRepository extends ListCrudRepository<Line, UUID> {

  List<Line> findAllByPolygon(Polygon polygon);
}
