package uk.co.fivium.gisalphatest.migration;

import java.util.List;
import java.util.Map;

public class BrokenBlockUtil {

  /**
   * This is a map of ref block names to license block names, where the license block is only partially in the ref block.
   * This is needed as a license block, which is only partially in a ref block, is hard to identify through the database.
   */
  public static final Map<String, List<String>> refBlockNameToLicenseBlocks = Map.of(
      "16/29", List.of("16/29c"),
      "16/30", List.of("16/29c")
  );

  public static List<String> getBrokenLicenseBlockNames(String refBlockName) {
    return refBlockNameToLicenseBlocks.getOrDefault(refBlockName, List.of());
  }
}
