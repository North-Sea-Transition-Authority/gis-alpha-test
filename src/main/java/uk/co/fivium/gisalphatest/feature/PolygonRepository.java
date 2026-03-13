package uk.co.fivium.gisalphatest.feature;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface PolygonRepository extends ListCrudRepository<Polygon, UUID> {

  List<Polygon> findAllByFeature(Feature feature);

  void deleteAllByFeatureIn(Collection<Feature> features);
}
