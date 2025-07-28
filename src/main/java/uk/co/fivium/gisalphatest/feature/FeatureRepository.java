package uk.co.fivium.gisalphatest.feature;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface FeatureRepository extends CrudRepository<Feature, UUID> {
}
