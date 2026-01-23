package uk.co.fivium.gisalphatest.feature;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

public interface LineRepository extends ListCrudRepository<Line, UUID> {

  List<Line> findAllByPolygon(Polygon polygon);

  List<Line> findAllByPolygonIn(List<Polygon> polygons);

  //TODO GISA-86 update postgres entities to use UUIDs as IDs rather than oracle Integers
  @Query("SELECT MAX(id) FROM Line")
  Integer findMaxId();
}
