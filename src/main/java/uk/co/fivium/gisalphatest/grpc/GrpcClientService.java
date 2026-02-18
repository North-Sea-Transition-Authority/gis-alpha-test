package uk.co.fivium.gisalphatest.grpc;

import arcgisjs.BatchConvertGeoJsonToEsriJsonRequest;
import arcgisjs.BatchConvertGeoJsonToEsriJsonResponse;
import arcgisjs.ConvertEsriJsonPolygonToGeoJsonRequestOuterClass;
import arcgisjs.GeneralizePolygonRequestOuterClass;
import arcgisjs.EsriJsonLineWithNavigationTypeAndIdOuterClass;
import arcgisjs.GeoJsonLineInputOuterClass;
import arcgisjs.GetStartAndEndPointsRequestOuterClass;
import arcgisjs.LineWithIdOuterClass;
import arcgisjs.MergeAndGeneralizeLinesRequestOuterClass;
import arcgisjs.MergePolygonsRequestOuterClass;
import arcgisjs.OrderedLineSegmentOuterClass;
import arcgisjs.ValidatePolygonReconstructionRequestOuterClass;
import arcgisjs.VerifyChildGeodesicLinesOverlapParentsRequestOuterClass;
import com.esri.core.geometry.Point;
import com.example.grpc.ArcGisServiceGrpc;
import com.example.grpc.BuildPolygonRequest;
import com.example.grpc.CalculateAreaResponse;
import com.example.grpc.CalculatePolygonAreaRequest;
import com.example.grpc.CheckParentContainsChildRequest;
import com.example.grpc.CheckParentContainsChildResponse;
import com.example.grpc.DensifyLoxodromeLineRequest;
import com.example.grpc.DensifyLoxodromeLineResponse;
import com.example.grpc.EsriJsonPolygon;
import com.example.grpc.EsriJsonResponse;
import com.example.grpc.ExplodePolygonRequest;
import com.example.grpc.ExplodePolygonResponse;
import com.example.grpc.FindParentLineRequest;
import com.example.grpc.GeoJsonRequest;
import com.example.grpc.LineParent;
import com.example.grpc.ReconstructedLine;
import com.example.grpc.SplitPolygonRequest;
import com.example.grpc.SplitPolygonResponse;
import com.example.grpc.UnionPolygonsRequest;
import com.example.grpc.UnionPolygonsResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisalphatest.feature.Line;
import uk.co.fivium.gisalphatest.feature.LineNavigationType;
import uk.co.fivium.gisalphatest.migration.OracleBoundaryLineWithRing;
import uk.co.fivium.gisalphatest.transformations.LineWrapper;

@Service
public class GrpcClientService {

  @GrpcClient("node-server")
  private ArcGisServiceGrpc.ArcGisServiceBlockingStub arcgisClient;

  /**
   * Converts a GeoJSON line to an EsriJSON polyline.
   * @param geoJson a GeoJSON of type "LineString" in string format
   * @param wkid the spatial reference Well Known ID of the geojson. (E.g. the number version of "ED 50")
   * @param isGeodesic true if the input GeoJSON represents a geodesic line.
   * @param parentLines list of all the lines from the parent shape, if there is no parent shape, this should be an empty list.
   * @return An EsriJSON polyline in string format
   */
  public String convertLineToEsriJson(
      String geoJson,
      Integer wkid,
      boolean isGeodesic,
      List<String> parentLines
  ) {
    GeoJsonRequest request = GeoJsonRequest.newBuilder()
        .setGeoJsonString(geoJson)
        .setWkid(wkid)
        .setIsGeodesic(isGeodesic)
        .addAllParentLines(parentLines)
        .build();

    EsriJsonResponse response = arcgisClient.convertGeoJsonLineToEsriJsonLine(request);
    return response.getEsriJsonString();
  }

  /**
   * Converts multiple GeoJSON lines to EsriJSON polylines in a single gRPC call.
   * If the line belongs to a child shape, the start and end nodes of some lines may be shifted to align with the parent shape
   * if the parent and child shape contain geodesic lines, as the parents geodesic line will be densified and may not then
   * line up with the child.
   * @param linesWithRing list of records containing the OracleBoundaryLine and its ring number
   * @param wkid the spatial reference Well Known ID
   * @param parentLines list of all the lines from the parent shape, if there is no parent shape, this should be an empty list.
   * @return A map of oracleLineSsid to EsriJSON polyline string
   */
  public Map<Integer, String> convertLinesToEsriJson(
      List<OracleBoundaryLineWithRing> linesWithRing,
      Integer wkid,
      List<String> parentLines
  ) {
    var requestBuilder = BatchConvertGeoJsonToEsriJsonRequest.BatchGeoJsonRequest.newBuilder()
        .setWkid(wkid)
        .addAllParentLineJsons(parentLines);

    for (var entry : linesWithRing) {
      var oracleLine = entry.oracleBoundaryLine();
      requestBuilder.addLinesWithType(GeoJsonLineInputOuterClass.GeoJsonLineInput.newBuilder()
          .setGeoJsonString(oracleLine.getLineGeojson())
          .setIsGeodesic(oracleLine.getLineNavigationType() == LineNavigationType.GEODESIC)
          .setOracleLineSsid(oracleLine.getLineSidId().intValue())
          .setConnectionOrder(oracleLine.getConnectionOrder())
          .build()
      );
    }

    BatchConvertGeoJsonToEsriJsonResponse.BatchEsriJsonResponse response =
        arcgisClient.batchConvertGeoJsonLinesToEsriJsonLines(requestBuilder.build());

    Map<Integer, String> result = new HashMap<>();
    for (var lineOutput : response.getLinesList()) {
      result.put(lineOutput.getOracleLineSsid(), lineOutput.getEsriJsonString());
    }
    return result;
  }

  public String convertCutLineToEsriJson(
      String geoJson,
      Integer wkid
  ) {
    return convertLineToEsriJson(geoJson, wkid, false, List.of());
  }

  /**
   * Takes a list of polylines and combines them into a polygon using the arcgis js sdk
   *
   * @param polylines An ordered list of Esri JSON polylines
   * @param srs The id of the coordinate system
   * @return EsriJSON of a polygon as a string.
   */
  public String buildPolygon(List<String> polylines, Integer srs) {
    var request = BuildPolygonRequest.newBuilder()
        .addAllEsriJsonLineStrings(polylines)
        .setSrs(srs)
        .build();

    EsriJsonResponse response = arcgisClient.buildPolygon(request);
    return response.getEsriJsonString();
  }

  /**
   * Densifies an EsriJSON loxodrome polyline
   * @param polyline an EsriJSON loxodrome polyline
   * @return a densified EsriJSON polyline
   */
  public String densifyLoxodromePolyline(String polyline) {
    var request = DensifyLoxodromeLineRequest.newBuilder()
        .setEsriJsonPolyline(polyline)
        .build();

    DensifyLoxodromeLineResponse response = arcgisClient.densifyLoxodromePolyline(request);
    return response.getEsriJsonPolyline();
  }

  /**
   * Takes a list of EsriJSON polygons and combines time into a single shape.
   * @param polygons a list of EsriJSON polygons
   * @return an EsriJSON polygon or "shape" that is made up of all the inputted polygons.
   */
  public String unionPolygons(List<String> polygons) {
    var request = UnionPolygonsRequest.newBuilder()
        .addAllEsriJsonPolygons(polygons)
        .build();
    UnionPolygonsResponse response = arcgisClient.unionPolygons(request);
    return response.getEsriJsonPolygon();
  }

  /**
   * Takes an EsriJSON polygon and returns the area
   * @param esriPolygon an EsriJSON polygon. This should have loxodrome lines densified.
   * @return the area in metres squared
   */
  public double calculatePolygonArea(
      String esriPolygon,
      boolean isOnshore
  ) {
    var request = CalculatePolygonAreaRequest.newBuilder()
        .setEsriJsonPolygon(esriPolygon)
        .setIsOnshore(isOnshore)
        .build();
    CalculateAreaResponse response = arcgisClient.calculatePolygonArea(request);
    return response.getArea();
  }

  public List<String> splitPolygon(String polygon, String cutter) {
    var request = SplitPolygonRequest.newBuilder()
        .setTarget(EsriJsonPolygon.newBuilder().setEsriJsonPolygon(polygon))
        .setEsriJsonCutter(cutter)
        .build();

    SplitPolygonResponse response = arcgisClient.splitPolygon(request);
    return response.getPolygonsList()
        .stream()
        .map(EsriJsonPolygon::getEsriJsonPolygon)
        .toList();
  }

  /**
   * Create a polyline for each coordinate pair in the polygon.
   * @param polygon Input polygon
   * @return A list of esriJson polylines for every coordinate pair of the input. These can be union together to reconstruct
   * the original input.
   */
  public List<String> explodePolygon(String polygon) {
    var request = ExplodePolygonRequest.newBuilder()
        .setTarget(EsriJsonPolygon.newBuilder().setEsriJsonPolygon(polygon))
        .build();
    ExplodePolygonResponse response = arcgisClient.explodePolygon(request);
    return response.getEsriJsonLinesList();
  }

  /**
   * Find which lines are contained by other lines
   * @param parentLines Line entities that will contain the contained lines
   * @param containedLines Child lines that are contained by the parentLines
   * @return A map of contained lines esri json to the id of the parent line that contains it.
   */
  public FindParentLineResponse findParentLine(List<Line> parentLines, List<String> containedLines) {
    var requestBuilder = FindParentLineRequest.newBuilder();

    for (Line line : parentLines) {
      requestBuilder.addParents(LineParent.newBuilder()
          .setId(line.getId().toString())
          .setEsriJsonPolyline(line.getLineJson())
          .build());
    }
    for (String polyline : containedLines) {
      requestBuilder.addChildren(polyline);
    }

    var response = arcgisClient.findParentLine(requestBuilder.build());

    Map<String, UUID> polylineToParentLineId = new HashMap<>();

    for (ReconstructedLine line : response.getLinesList()) {
      polylineToParentLineId.put(line.getEsriJsonPolyline(), UUID.fromString(line.getParentId()));
    }

    return new FindParentLineResponse(polylineToParentLineId, response.getOrphanedChildrenJsonList());
  }

  /**
   * Verifies that all child geodesic lines overlap their parent geodesic lines.
   * @param parentLines All parent lines with navigation types
   * @param childLines All child lines with navigation types
   * @return true if all child geodesic lines overlap their parent geodesic lines, else false.
   */
  public boolean verifyChildGeodesicLinesOverlapParents(
      List<Line> parentLines,
      List<Line> childLines
  ) {
    var requestBuilder = VerifyChildGeodesicLinesOverlapParentsRequestOuterClass.VerifyChildGeodesicLinesOverlapParentsRequest.newBuilder();

    for (Line line : parentLines) {
      requestBuilder.addParentLines(
          EsriJsonLineWithNavigationTypeAndIdOuterClass.EsriJsonLineWithNavigationTypeAndId.newBuilder()
          .setEsriJsonPolyline(line.getLineJson())
          .setIsGeodesic(LineNavigationType.GEODESIC.equals(line.getNavigationType()))
          .build()
      );
    }

    for (Line line : childLines) {
      requestBuilder.addChildLines(
          EsriJsonLineWithNavigationTypeAndIdOuterClass.EsriJsonLineWithNavigationTypeAndId.newBuilder()
          .setEsriJsonPolyline(line.getLineJson())
          .setIsGeodesic(LineNavigationType.GEODESIC.equals(line.getNavigationType()))
          .build()
      );
    }

    var response = arcgisClient.verifyChildGeodesicLinesOverlapParents(requestBuilder.build());
    return response.getIsValid();
  }

  public boolean checkParentContainsChild(String parentPolygon, String childPolygon) {
    var request = CheckParentContainsChildRequest.newBuilder()
        .setParentPolygon(parentPolygon)
        .setChildPolygon(childPolygon)
        .build();

    CheckParentContainsChildResponse response = arcgisClient.checkParentContainsChild(request);
    return response.getIsChildContainedByParent();
  }

  /**
   * Find the start and end coordinates for each line.
   * @param lines
   * @return A list with a record containig the line entity plus the start and end points.
   */
  public List<LineWrapper> getLineStartAndEndpoints(List<Line> lines) {
    //lines may not been persisted yet so they might not have an ID
    Map<UUID, Line> tempIdToLine = new HashMap<>();
    lines.forEach(line -> tempIdToLine.put(UUID.randomUUID(), line));

    var linesWithId = tempIdToLine.entrySet().stream()
        .map(entry -> LineWithIdOuterClass.LineWithId.newBuilder()
            .setId(entry.getKey().toString())
            .setPolyLineEsriJson(entry.getValue().getLineJson())
            .build())
        .toList();
    var request = GetStartAndEndPointsRequestOuterClass.GetStartAndEndPointsRequest.newBuilder()
        .addAllLines(linesWithId)
        .build();

    var response = arcgisClient.getStartAndEndPoints(request);

    return response.getLinesList().stream()
        .map(lineWithStartAndEndPoint -> {
          var line = tempIdToLine.get(UUID.fromString(lineWithStartAndEndPoint.getLineId()));
          Point startPoint = new Point(lineWithStartAndEndPoint.getStartPoint().getX(), lineWithStartAndEndPoint.getStartPoint().getY());
          Point endPoint = new Point(lineWithStartAndEndPoint.getEndPoint().getX(), lineWithStartAndEndPoint.getEndPoint().getY());
          return LineWrapper.fromNodeResponse(line, startPoint, endPoint);
        })
        .toList();
  }

  /**
   * Validate the line ordering is correct and can be used to construct a valid polygon.
   * Validate the lines can form a polygon that is spatially equal to teh original polygon.
   * @param lines Lines used to construct a processed polygon.
   * @param originalPolygonEsriJson Original polygon before processing.
   * @return True if line ordering is correct and processed lines can form a spatially equal polygon to the original.
   */
  public boolean validatePolygonReconstruction(List<Line> lines, String originalPolygonEsriJson) {
    var orderedLineSegments = lines.stream()
        .map(line -> OrderedLineSegmentOuterClass.OrderedLineSegment.newBuilder()
            .setEsriJsonPolyline(line.getLineJson())
            .setRingNumber(line.getRingNumber())
            .setConnectionOrder(line.getRingConnectionOrder())
            .build())
        .toList();
    var request = ValidatePolygonReconstructionRequestOuterClass.ValidatePolygonReconstructionRequest.newBuilder()
        .addAllLines(orderedLineSegments)
        .setOriginalPolygonEsriJson(originalPolygonEsriJson)
        .build();

    var response = arcgisClient.validatePolygonReconstruction(request);
    return response.getIsValid();
  }

  /**
   * Merge 2 polygons using the ArcGis JS unionOperator.
   * @param polygon1 EsriJson polygon to union
   * @param polygon2 EsriJson polygon to union
   * @return The merged polygon esriJson
   */
  public String mergePolygons(String polygon1, String polygon2) {
    var request = MergePolygonsRequestOuterClass.MergePolygonsRequest.newBuilder()
        .setInputPolygon1(polygon1)
        .setInputPolygon2(polygon2)
        .build();
    var response = arcgisClient.mergePolygons(request);
    return response.getResultPolygon();
  }

  /**
   * Remove redundant vertices on a polygon using the ArcGis generalizeOperator.
   * @param polygon Polygon to generalize as EsriJson.
   * @return Generalized polygon as EsriJson.
   */
  public String generalizePolygon(String polygon) {
    var request = GeneralizePolygonRequestOuterClass.GeneralizePolygonRequest.newBuilder()
        .setEsriPolygon(polygon)
        .build();
    var response = arcgisClient.generalizePolygon(request);
    return response.getEsriPolygon();
  }

  /**
   * Merge multiple polylines into a single line.
   * The resulting line is generalized to remove redundant vertices.
   */
  public String mergeAndGeneralizeLines(List<String> polylinesEsriJson) {
    var request = MergeAndGeneralizeLinesRequestOuterClass.MergeAndGeneralizeLinesRequest.newBuilder()
        .addAllEsriPolylines(polylinesEsriJson)
        .build();
    var response = arcgisClient.mergeAndGeneralizeLines(request);
    return response.getEsriPolyline();
  }

  /**
   * Convert an esriJson polygon into the geoJson format. It will project the geometry to srs WGS84 (World Geodetic System 1984)
   * as that is the SRS supported by GeoJson
   * @param esriJsonPolygon Polygon to convert
   * @return A string representing the original polygon after being projected to the new srs and stored in geoJson.
   */
  public String convertEsriJsonPolygonToGeoJson(String esriJsonPolygon) {
    var request = ConvertEsriJsonPolygonToGeoJsonRequestOuterClass.ConvertEsriJsonPolygonToGeoJsonRequest.newBuilder()
        .setEsriJsonPolygon(esriJsonPolygon)
        .build();

    var response = arcgisClient.convertEsriJsonPolygonToGeoJson(request);
    return response.getGeoJsonPolygon();
  }
}
