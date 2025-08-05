package uk.co.fivium.gisalphatest.feature;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface PointRepository extends CrudRepository<Point, UUID> {

  Collection<Point> findAllByLineIn(Collection<Line> lines);
}
