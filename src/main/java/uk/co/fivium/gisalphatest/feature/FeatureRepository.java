package uk.co.fivium.gisalphatest.feature;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface FeatureRepository extends ListCrudRepository<Feature, UUID> {

  Optional<Feature> findByShapeSidIdAndTestCase(Integer shapeSidId, String testCase);

  List<Feature> findAllByShapeSidId(Integer shapeSidId);
}
