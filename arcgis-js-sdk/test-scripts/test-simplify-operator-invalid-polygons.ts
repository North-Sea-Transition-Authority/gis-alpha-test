import Polygon from "@arcgis/core/geometry/Polygon.js";
import * as simplifyOperator from "@arcgis/core/geometry/operators/simplifyOperator.js";

//The aim is to test if the simplify operator changes the rings' starting point after execution.
//The test polygons have rings with the wrong winding order, so the operator should update the elements with the correct orientation.

// 1. Create Variant 1: Starts at [0,0], Winding is WRONG (CCW)
// Path: (0,0) -> (10,0) -> (0,10) -> (0,0)
const polyBadWinding1 = new Polygon({
    rings: [
        [
            [0, 0],
            [10, 0],
            [0, 10],
            [0, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

// 2. Create Variant 2: Starts at [10,0], Winding is WRONG (CCW)
// Path: (10,0) -> (0,10) -> (0,0) -> (10,0)
const polyBadWinding2 = new Polygon({
    rings: [
        [
            [10, 0],
            [0, 10],
            [0, 0],
            [10, 0]
        ]
    ],
    spatialReference: { wkid: 4326 }
});

console.log("--- BEFORE SIMPLIFY ---");
console.log("Poly 1 Simple?", simplifyOperator.isSimple(polyBadWinding1)); // Should be FALSE
console.log("Poly 2 Simple?", simplifyOperator.isSimple(polyBadWinding2)); // Should be FALSE

// 3. Execute Simplify (Fixes Winding)
const simple1 = simplifyOperator.execute(polyBadWinding1) as Polygon;
const simple2 = simplifyOperator.execute(polyBadWinding2) as Polygon;

console.log("\n--- AFTER SIMPLIFY ---");
console.log("Poly 1 Simple?", simplifyOperator.isSimple(simple1)); // Should be TRUE
console.log("Poly 2 Simple?", simplifyOperator.isSimple(simple2)); // Should be TRUE

// 4. Analyze the Resulting Paths
const ring1 = simple1.rings[0];
const ring2 = simple2.rings[0];

// Print the full rings to see the order
console.log("\n--- RESULTING RINGS (Format: [x,y]) ---");
console.log("Result 1:", JSON.stringify(ring1));
// Expected Change: Reversed to [ [0,0], [0,10], [10,0], [0,0] ]

console.log("Result 2:", JSON.stringify(ring2));
// Expected Change: Reversed to [ [10,0], [0,0], [0,10], [10,0] ]


// 5. Compare Start Points
const start1 = ring1[0];
const start2 = ring2[0];

console.log("\n--- START POINT CHECK ---");
console.log(`Start Point 1: [${start1}]`);
console.log(`Start Point 2: [${start2}]`);

if (start1[0] !== start2[0] || start1[1] !== start2[1]) {
    console.log("CONCLUSION: The operator fixed the winding but PRESERVED the different start points.");
} else {
    console.log("CONCLUSION: The operator NORMALIZED the start points.");
}