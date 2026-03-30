package uk.co.fivium.gisalphatest.feature;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import uk.co.fivium.gisalphatest.oracle.ShapeType;
import uk.co.fivium.gisalphatest.transformations.command.TransformationCommand;

public interface FeatureRepository extends ListCrudRepository<Feature, UUID> {

  List<Feature> findAllByActive(boolean active);

  Optional<Feature> findByShapeSidIdAndTestCase(Integer shapeSidId, String testCase);

  List<Feature> findAllByShapeSidId(Integer shapeSidId);

  List<Feature> findAllByShapeSidIdIn(List<Integer> shapeSidIds);

  List<Feature> findAllByParentFeatureIdIsNotNull();

  List<Feature> findAllByParentFeatureId(UUID parentFeatureId);

  List<Feature> findAllByCreatedByCommand(TransformationCommand command);

  List<Feature> findAllByType(ShapeType type);
}
