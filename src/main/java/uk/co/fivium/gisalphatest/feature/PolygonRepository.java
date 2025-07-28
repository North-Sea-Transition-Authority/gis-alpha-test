package uk.co.fivium.gisalphatest.feature;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface PolygonRepository extends CrudRepository<Polygon, UUID> {
}
