import {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";

import * as containsOperator from "@arcgis/core/geometry/operators/containsOperator.js";
import Polygon from "@arcgis/core/geometry/Polygon";

export const checkParentContainsChild: ArcGisServiceHandlers["CheckParentContainsChild"] = (call, callback) => {
 const parent = Polygon.fromJSON(JSON.parse(call.request.parentPolygon));
 const child = Polygon.fromJSON(JSON.parse(call.request.childPolygon));

 const isChildContainedByParent = containsOperator.execute(parent, child);

 console.log(`isChildContainedByParent: ${isChildContainedByParent}`);

 callback(null, {isChildContainedByParent: isChildContainedByParent});
}