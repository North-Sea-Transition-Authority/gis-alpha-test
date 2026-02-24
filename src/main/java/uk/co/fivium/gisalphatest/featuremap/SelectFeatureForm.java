package uk.co.fivium.gisalphatest.featuremap;

import java.util.List;
import java.util.UUID;

public class SelectFeatureForm {
  private List<UUID> featureIds;

  public List<UUID> getFeatureIds() {
    return featureIds;
  }

  public void setFeatureIds(List<UUID> featureIds) {
    this.featureIds = featureIds;
  }
}
