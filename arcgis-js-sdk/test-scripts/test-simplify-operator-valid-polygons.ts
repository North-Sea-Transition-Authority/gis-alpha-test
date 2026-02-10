import Polygon from "@arcgis/core/geometry/Polygon.js";
import * as simplifyOperator from "@arcgis/core/geometry/operators/simplifyOperator.js";


//The aim is to test if the simplify operator changes the rings' starting point after execution. To see if it selects a specific starting point.
//These are variants of the same valid polygon with different starting points.

// 1. Define Variant 1: Triangle starting at Bottom-Left [0,0]
const poly1 = new Polygon({
    rings: [
        [
            [0, 0],
            [0, 10],
            [10, 0],
            [0, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

// 2. Define Variant 2: Same Triangle starting at Top-Left [0,10]
const poly2 = new Polygon({
    rings: [
        [
            [0, 10],
            [10, 0],
            [0, 0],
            [0, 10]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

// 3. Execute Simplify
// The operator returns GeometryUnion | null | undefined, so we cast or check it.
const simple1 = simplifyOperator.execute(poly1) as Polygon;
const simple2 = simplifyOperator.execute(poly2) as Polygon;

if (simple1 && simple2) {
    // 4. Extract the starting points (first vertex of the first ring)
    const start1 = simple1.rings[0][0];
    const start2 = simple2.rings[0][0];

    console.log(`Variant 1 Start: [${start1[0]}, ${start1[1]}]`);
    console.log(`Variant 2 Start: [${start2[0]}, ${start2[1]}]`);

    // 5. Verify if they were normalized to the same point
    const isSameStart = (start1[0] === start2[0]) && (start1[1] === start2[1]);

    console.log("------------------------------------------------");
    if (isSameStart) {
        console.log("RESULT: The operator NORMALIZED the start point.");
    } else {
        console.log("RESULT: The operator PRESERVED the original start point.");
    }
} else {
    console.error("Simplification failed (returned null).");
}