package uk.co.fivium.gisalphatest.feature;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface LineRepository extends ListCrudRepository<Line, UUID> {

  List<Line> findAllByPolygon(Polygon polygon);

  List<Line> findAllByPolygonIn(List<Polygon> polygons);

  void deleteAllByPolygon_FeatureIn(Collection<Feature> features);

  List<Line> findAllByPolygon_FeatureIn(List<Feature> features);
}
