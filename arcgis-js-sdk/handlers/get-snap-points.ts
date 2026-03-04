import type {ArcGisServiceHandlers} from "../generated/arcgisjs/ArcGisService";
import * as projectOperator from "@arcgis/core/geometry/operators/projectOperator";
import Point from "@arcgis/core/geometry/Point.js";
import SpatialReference from "@arcgis/core/geometry/SpatialReference";
import {ed50ToWgs84Transformation, wgs84ToEd50Transformation} from "../utils/projection-utils";

export const getSnapPoints: ArcGisServiceHandlers["getSnapPoints"] = async (call, callback) => {
    if (!projectOperator.isLoaded()) {
        await projectOperator.load();
    }

    const {wgs84MinLon, wgs84MinLat, wgs84MaxLon, wgs84MaxLat, srsWkid} = call.request;
    const wgs84 = new SpatialReference({wkid: 4326}); //WGS84
    const originalSrs = new SpatialReference({wkid: srsWkid});

    const minStartingPointWgs84 = new Point({
        x: wgs84MinLon,
        y: wgs84MinLat,
        spatialReference: wgs84
    });
    const maxEndingPointWgs84 = new Point({
        x: wgs84MaxLon,
        y: wgs84MaxLat,
        spatialReference: wgs84
    });

    let projectOperatorOptionsWgs84ToOriginalSrs = {};
    let projectOperatorOptionsOriginalSrsToWgs84 = {};
    if (srsWkid === 4230) {
        //originalSrs == ED50
        projectOperatorOptionsWgs84ToOriginalSrs = {geographicTransformation: wgs84ToEd50Transformation};
        projectOperatorOptionsOriginalSrsToWgs84 = {geographicTransformation: ed50ToWgs84Transformation};
    }

    const minStartingPointOriginalSrs = projectOperator.execute(minStartingPointWgs84, originalSrs, projectOperatorOptionsWgs84ToOriginalSrs) as Point;
    const maxEndingPointOriginalSrs = projectOperator.execute(maxEndingPointWgs84, originalSrs, projectOperatorOptionsWgs84ToOriginalSrs) as Point;

    const arcSecondSpacing = 30 / 3600; // 30 arc seconds in decimal degrees
    const originalSrsStartLon = Math.floor(minStartingPointOriginalSrs.x / arcSecondSpacing) * arcSecondSpacing;
    const originalSrsStartLat = Math.floor(minStartingPointOriginalSrs.y / arcSecondSpacing) * arcSecondSpacing;

    const pointsArray = [];
    for (let originalSrsLon = originalSrsStartLon; originalSrsLon < maxEndingPointOriginalSrs.x; originalSrsLon += arcSecondSpacing) {
        for (let originalSrsLat = originalSrsStartLat; originalSrsLat < maxEndingPointOriginalSrs.y; originalSrsLat += arcSecondSpacing) {
            const currentPointOriginalSrs = new Point({
                x: originalSrsLon,
                y: originalSrsLat,
                spatialReference: originalSrs
            });
            const currentPointWgs84 = projectOperator.execute(currentPointOriginalSrs, wgs84, projectOperatorOptionsOriginalSrsToWgs84) as Point;

            pointsArray.push({
                id: `${originalSrsLon},${originalSrsLat}`,
                wgs84CoordinatesEsriJson: JSON.stringify(currentPointWgs84.toJSON()),
                originalSrsCoordinatesEsriJson: JSON.stringify(currentPointOriginalSrs.toJSON())
            });
        }
    }

    callback(null, {points: pointsArray});
}