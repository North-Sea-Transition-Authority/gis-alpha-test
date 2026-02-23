import * as geographicTransformationUtils
    from "@arcgis/core/geometry/operators/support/geographicTransformationUtils.js";
import * as projectOperator from "@arcgis/core/geometry/operators/projectOperator.js";
import SpatialReference from "@arcgis/core/geometry/SpatialReference.js";
import GeographicTransformationStep from "@arcgis/core/geometry/operators/support/GeographicTransformationStep";
import Point from "@arcgis/core/geometry/Point.js";
import GeographicTransformation from "@arcgis/core/geometry/operators/support/GeographicTransformation";

async function tester() {
    if (!geographicTransformationUtils.isLoaded()) {
        await geographicTransformationUtils.load();
    }

    if (!projectOperator.isLoaded()) {
        await projectOperator.load();
    }


    const ed50SR = new SpatialReference({ wkid: 4230 });
    const wgs84SR = new SpatialReference({ wkid: 4326 });

    // ED50 test point: 53°00'02.887"N 01°00'05.101"E
    // Expected WGS84 result: 53°00'00.000"N 01°00'00.000"E
    const geometry = new Point({
        x: 1.001417,  // 01°00'05.101"E converted to decimal degrees
        y: 53.000802, // 53°00'02.887"N
        spatialReference: ed50SR
    });

    const geoTransfStep = new GeographicTransformationStep({ wkid: 1311 }); //ED_1950_To_WGS_1984_18
    const geoTransformation = new GeographicTransformation({
        steps: [geoTransfStep]
    })

    const result = projectOperator.execute(geometry, wgs84SR, {geographicTransformation: geoTransformation}) as Point;
    console.log('Projected point:', result.toJSON());
    console.log('Expected: x=1.0, y=53.0');

}

tester();