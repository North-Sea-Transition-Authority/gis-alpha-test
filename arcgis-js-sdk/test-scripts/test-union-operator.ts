import Polygon from "@arcgis/core/geometry/Polygon.js";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";
import * as generalizeOperator from "@arcgis/core/geometry/operators/generalizeOperator.js";

// SPIKE: Union of two polygons under different boundary-sharing scenarios.
// Goal: Understand what the unionOperator produces for each case and whether
// the merge is more complex than anticipated.

// ============================================================================
// Helper: log the rings of a result polygon
// ============================================================================
function logResult(label: string, result: Polygon | null | undefined) {
    console.log(`\n=== ${label} ===`);
    if (!result) {
        console.log("Result is null/undefined — union produced no geometry.");
        return;
    }
    console.log(`Number of rings: ${result.rings.length}`);
    result.rings.forEach((ring, i) => {
        console.log(`  Ring ${i} (${ring.length} points): ${JSON.stringify(ring)}`);
    });
    console.log(`Extent: ${JSON.stringify(result.extent.toJSON())}`);
}

// ============================================================================
// CASE 1 — Shared boundary with NO extra points on the boundary
// Two adjacent squares sharing edge x=10 from y=0 to y=10.
//
//  polyA          polyB
//  (0,10)---(10,10)---(20,10)
//    |         |         |
//    |         |         |
//  (0,0)----(10,0)----(20,0)
// ============================================================================
console.log("\n############################################################");
console.log("# CASE 1: Shared boundary — no extra points on boundary   #");
console.log("############################################################");

const case1_polyA = new Polygon({
    rings: [
        [
            [0, 0],
            [0, 10],
            [10, 10],
            [10, 0],
            [0, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

const case1_polyB = new Polygon({
    rings: [
        [
            [10, 0],
            [10, 10],
            [20, 10],
            [20, 0],
            [10, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

console.log("Input A rings:", JSON.stringify(case1_polyA.rings));
console.log("Input B rings:", JSON.stringify(case1_polyB.rings));

const case1_result = unionOperator.execute(case1_polyA, case1_polyB) as Polygon;
logResult("CASE 1 RESULT", case1_result);

// ============================================================================
// CASE 2 — Shared boundary WITH extra points on the boundary
// Same adjacent squares, but both polygons have a midpoint (10,5) on the
// shared edge, so the shared boundary contains an interior vertex.
//
//  polyA               polyB
//  (0,10)---(10,10)---(20,10)
//    |       (10,5)       |
//    |         |          |
//  (0,0)----(10,0)----(20,0)
// ============================================================================
console.log("\n############################################################");
console.log("# CASE 2: Shared boundary — with points on boundary       #");
console.log("############################################################");

const case2_polyA = new Polygon({
    rings: [
        [
            [0, 0],
            [0, 10],
            [10, 10],
            [10, 5],
            [10, 0],
            [0, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

const case2_polyB = new Polygon({
    rings: [
        [
            [10, 0],
            [10, 5],
            [10, 10],
            [20, 10],
            [20, 0],
            [10, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

console.log("Input A rings:", JSON.stringify(case2_polyA.rings));
console.log("Input B rings:", JSON.stringify(case2_polyB.rings));

const case2_result = unionOperator.execute(case2_polyA, case2_polyB) as Polygon;
logResult("CASE 2 RESULT", case2_result);

// ============================================================================
// CASE 2.5 — Shared boundary, but only polyA has an extra point on it
// polyA has a midpoint (5,10) on its top edge. polyB does NOT have that point.
// Shows whether the union handles mismatched vertex density on the shared boundary.
//
//  polyA               polyB
//  (0,10)--(5,10)-(10,10)---(20,10)
//    |                |         |
//    |                |         |
//  (0,0)------------(10,0)---(20,0)
// ============================================================================
console.log("\n############################################################");
console.log("# CASE 2.5: Shared boundary — only polyA has extra point  #");
console.log("############################################################");

const case2_5_polyA = new Polygon({
    rings: [
        [
            [0, 0],
            [0, 10],
            [5, 10],
            [10, 10],
            [10, 0],
            [0, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

const case2_5_polyB = new Polygon({
    rings: [
        [
            [10, 0],
            [10, 10],
            [20, 10],
            [20, 0],
            [10, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

console.log("Input A rings:", JSON.stringify(case2_5_polyA.rings));
console.log("Input B rings:", JSON.stringify(case2_5_polyB.rings));

const case2_5_result = unionOperator.execute(case2_5_polyA, case2_5_polyB) as Polygon;
logResult("CASE 2.5 RESULT", case2_5_result);

// ============================================================================
// CASE 2.5 AFTER GENERALIZE — Run generalizeOperator on the union result
// The vertex (5,10) is collinear between (0,10) and (10,10), so its deviation
// from the line is 0. A small maxDeviation should strip it without affecting
// the actual corner vertices of the rectangle.
// ============================================================================
console.log("\n############################################################");
console.log("# CASE 2.5 AFTER GENERALIZE                               #");
console.log("############################################################");

//max deviation set to 0.0000001. Which is around 1cm for the unit degrees in our SRS.
const case2_5_generalized = generalizeOperator.execute(case2_5_result, 0.0000001) as Polygon;
logResult("CASE 2.5 AFTER GENERALIZE RESULT", case2_5_generalized);

// ============================================================================
// CASE 3 — Share a single corner point, no shared line
// Two squares touching only at (10,10).
//
//              polyB
//          (10,20)---(20,20)
//            |          |
//            |          |
//  (0,10)---(10,10)---(20,10)
//    |         |
//    |  polyA  |
//  (0,0)----(10,0)
// ============================================================================
console.log("\n############################################################");
console.log("# CASE 3: Share a single corner point, no shared line     #");
console.log("############################################################");

const case3_polyA = new Polygon({
    rings: [
        [
            [0, 0],
            [0, 10],
            [10, 10],
            [10, 0],
            [0, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

const case3_polyB = new Polygon({
    rings: [
        [
            [10, 10],
            [10, 20],
            [20, 20],
            [20, 10],
            [10, 10]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

console.log("Input A rings:", JSON.stringify(case3_polyA.rings));
console.log("Input B rings:", JSON.stringify(case3_polyB.rings));

const case3_result = unionOperator.execute(case3_polyA, case3_polyB) as Polygon;
logResult("CASE 3 RESULT", case3_result);

// ============================================================================
// CASE 4 — Disjoint polygons (no shared points or boundaries)
// Two squares with a gap between them.
//
//  polyA                    polyB
//  (0,10)---(10,10)    (20,10)---(30,10)
//    |         |          |         |
//    |         |          |         |
//  (0,0)----(10,0)     (20,0)----(30,0)
// ============================================================================
console.log("\n############################################################");
console.log("# CASE 4: Disjoint — no shared points or boundaries       #");
console.log("############################################################");

const case4_polyA = new Polygon({
    rings: [
        [
            [0, 0],
            [0, 10],
            [10, 10],
            [10, 0],
            [0, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

const case4_polyB = new Polygon({
    rings: [
        [
            [20, 0],
            [20, 10],
            [30, 10],
            [30, 0],
            [20, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

console.log("Input A rings:", JSON.stringify(case4_polyA.rings));
console.log("Input B rings:", JSON.stringify(case4_polyB.rings));

const case4_result = unionOperator.execute(case4_polyA, case4_polyB) as Polygon;
logResult("CASE 4 RESULT", case4_result);

// ============================================================================
// CASE 5 — Polygon with a hole (donut) unioned with an adjacent polygon
// polyA is a square (0,0)→(20,20) with a square hole (5,5)→(15,15).
// polyB is a plain square (20,0)→(30,20) sharing the right edge of polyA.
//
//  (0,20)---------(20,20)---(30,20)
//    |               |         |
//    |  (5,15)-(15,15)  polyB  |
//    |    | hole |     |       |
//    |  (5,5)--(15,5)  |       |
//    |               |         |
//  (0,0)----------(20,0)----(30,0)
// ============================================================================
console.log("\n############################################################");
console.log("# CASE 5: Polygon with hole + adjacent polygon            #");
console.log("############################################################");

const case5_polyA = new Polygon({
    rings: [
        // Outer ring (clockwise)
        [
            [0, 0],
            [0, 20],
            [20, 20],
            [20, 0],
            [0, 0]
        ],
        // Inner ring / hole (counter-clockwise)
        [
            [5, 5],
            [15, 5],
            [15, 15],
            [5, 15],
            [5, 5]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

const case5_polyB = new Polygon({
    rings: [
        [
            [20, 0],
            [20, 20],
            [30, 20],
            [30, 0],
            [20, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

console.log("Input A rings:", JSON.stringify(case5_polyA.rings));
console.log("Input B rings:", JSON.stringify(case5_polyB.rings));

const case5_result = unionOperator.execute(case5_polyA, case5_polyB) as Polygon;
logResult("CASE 5 RESULT", case5_result);

// ============================================================================
// CASE 6 — Two solid polygons that form a donut when merged
// polyA is the bottom U-shape, polyB is the top U-shape.
// Neither has a hole, but together they form a square frame with a hole.
//
//  (0,20)----------------------(20,20)
//    |                            |
//    |  (5,15)----------(15,15)   |
//  polyB |                   | polyB
//    |  (5,10)          (15,10)   |
//    |---|                  |-----|
//    |  (5,10)          (15,10)   |
//  polyA |                  | polyA
//    |  (5,5)-----------(15,5)    |
//    |                            |
//  (0,0)-----------------------(20,0)
// ============================================================================
console.log("\n############################################################");
console.log("# CASE 6: Two solid polygons that form a donut when merged#");
console.log("############################################################");

// Bottom U-shape: outer frame from y=0 to y=10, with a notch cut out (5,5)→(15,10)
const case6_polyA = new Polygon({
    rings: [
        [
            [0, 0],
            [0, 10],
            [5, 10],
            [5, 5],
            [15, 5],
            [15, 10],
            [20, 10],
            [20, 0],
            [0, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

// Top U-shape: outer frame from y=10 to y=20, with a notch cut out (5,10)→(15,15)
const case6_polyB = new Polygon({
    rings: [
        [
            [0, 10],
            [0, 20],
            [20, 20],
            [20, 10],
            [15, 10],
            [15, 15],
            [5, 15],
            [5, 10],
            [0, 10]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

console.log("Input A rings:", JSON.stringify(case6_polyA.rings));
console.log("Input B rings:", JSON.stringify(case6_polyB.rings));

const case6_result = unionOperator.execute(case6_polyA, case6_polyB) as Polygon;
logResult("CASE 6 RESULT", case6_result);

// ============================================================================
// CASE 7 — C-shape + rectangle forming a hole
// polyA is a C-shape opening to the right. polyB is a rectangle that caps the
// open side, enclosing the inner area of the C and creating a hole.
//
//  (0,20)-----------(20,20)---(25,20)
//    |                 |         |
//    |  (5,15)---(20,15)  polyB  |
//    |    |  (hole)  |    |      |
//    |  (5,5)----(20,5)   |      |
//    |                 |         |
//  (0,0)------------(20,0)----(25,0)
//
//       polyA (C-shape)    polyB (rect)
//
// Expected: outer ring (0,0)→(25,20), hole at (5,5)→(20,15)
// ============================================================================
console.log("\n############################################################");
console.log("# CASE 7: C-shape + rectangle forming a hole              #");
console.log("############################################################");

// C-shape opening to the right
const case7_polyA = new Polygon({
    rings: [
        [
            [0, 0],
            [0, 20],
            [20, 20],
            [20, 15],
            [5, 15],
            [5, 5],
            [20, 5],
            [20, 0],
            [0, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

// Rectangle that caps the right side of the C
const case7_polyB = new Polygon({
    rings: [
        [
            [20, 0],
            [20, 20],
            [25, 20],
            [25, 0],
            [20, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

console.log("Input A rings:", JSON.stringify(case7_polyA.rings));
console.log("Input B rings:", JSON.stringify(case7_polyB.rings));

const case7_result = unionOperator.execute(case7_polyA, case7_polyB) as Polygon;
logResult("CASE 7 RESULT", case7_result);

// ============================================================================
// CASE 8 — Two polygons that partially overlap
// polyA and polyB are squares that overlap in a 5x10 region.
//
//  (0,10)-------(10,10)
//    |       (5,10)-------(15,10)
//    |  polyA  | overlap |       |
//    |       (5,0)---------(15,0)
//  (0,0)--------(10,0)
//
// polyA: (0,0)→(10,10)
// polyB: (5,0)→(15,10)
// Overlap region: (5,0)→(10,10)
// Expected: single ring covering (0,0)→(15,10)
// ============================================================================
console.log("\n############################################################");
console.log("# CASE 8: Two polygons that partially overlap             #");
console.log("############################################################");

const case8_polyA = new Polygon({
    rings: [
        [
            [0, 0],
            [0, 10],
            [10, 10],
            [10, 0],
            [0, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

const case8_polyB = new Polygon({
    rings: [
        [
            [5, 0],
            [5, 10],
            [15, 10],
            [15, 0],
            [5, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

console.log("Input A rings:", JSON.stringify(case8_polyA.rings));
console.log("Input B rings:", JSON.stringify(case8_polyB.rings));

const case8_result = unionOperator.execute(case8_polyA, case8_polyB) as Polygon;
logResult("CASE 8 RESULT", case8_result);

// ============================================================================
// SUMMARY
// ============================================================================
console.log("\n\n############################################################");
console.log("# SUMMARY                                                  #");
console.log("############################################################");

const cases = [
    { name: "Case 1 (shared boundary, no extra pts)", result: case1_result },
    { name: "Case 2 (shared boundary, with extra pts)", result: case2_result },
    { name: "Case 2.5 (shared boundary, only polyA has extra pt)", result: case2_5_result },
    { name: "Case 2.5 after generalize", result: case2_5_generalized },
    { name: "Case 3 (single shared corner point)", result: case3_result },
    { name: "Case 4 (disjoint)", result: case4_result },
    { name: "Case 5 (polygon with hole + adjacent)", result: case5_result },
    { name: "Case 6 (two solids forming a donut)", result: case6_result },
    { name: "Case 7 (C-shape + rect forming a hole)", result: case7_result },
    { name: "Case 8 (partial overlap)", result: case8_result },
];

for (const c of cases) {
    if (c.result) {
        console.log(`${c.name}: ${c.result.rings.length} ring(s), total vertices = ${c.result.rings.reduce((sum, r) => sum + r.length, 0)}`);
    } else {
        console.log(`${c.name}: null/undefined`);
    }
}
