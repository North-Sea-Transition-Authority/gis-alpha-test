package uk.co.fivium.gisalphatest.feature;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface LineRepository extends CrudRepository<Line, UUID> {

  Collection<Line> findAllByPolygon(Polygon polygon);
}
