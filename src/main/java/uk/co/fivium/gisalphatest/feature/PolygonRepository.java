package uk.co.fivium.gisalphatest.feature;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

public interface PolygonRepository extends ListCrudRepository<Polygon, UUID> {

  List<Polygon> findAllByFeature(Feature feature);

  //TODO GISA-86 update postgres entities to use UUIDs as IDs rather than oracle Integers
  @Query("SELECT MAX(id) FROM Polygon ")
  Integer findMaxId();
}
