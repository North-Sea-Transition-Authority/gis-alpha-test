import {describe, expect, test} from 'vitest';
import Polyline from "@arcgis/core/geometry/Polyline.js";
import Point from "@arcgis/core/geometry/Point.js";
import {replaceSegment} from "../handlers/migrate-ref-block";
import {CoordinateSystem} from "../enums/coordinate-system";

const SR = { wkid: CoordinateSystem.ED50};

function polyline(coords: number[][]): Polyline {
    return new Polyline({ paths: [coords], spatialReference: SR });
}

function point(x: number, y: number): Point {
    return new Point({ x, y, spatialReference: SR });
}

function pathCoords(line: Polyline): number[][] {
    return line.paths[0];
}

describe("replaceSegment", () => {

    // Ref block:  A(0,0) -- B(1,0) -- C(2,0) -- D(3,0) -- E(4,0)
    // License:    P(1,1) -- Q(2,1)
    // Start intersection at (1, 0.5), end intersection at (2, 0.5)
    // Replace B--C on the ref block with the license segment

    test("same direction - replaces middle segment with license points", () => {
        const refBlock = polyline([[0, 0], [1, 0], [2, 0], [3, 0], [4, 0]]);
        const license = polyline([[1, 1], [1.5, 1.2], [2, 1]]);
        const start = point(1, 0.5);
        const end = point(2, 0.5);

        const result = replaceSegment(refBlock, license, start, end, true);
        const coords = pathCoords(result);

        expect(coords).toEqual([
            [0, 0],       // Before replacement
            [1, 0.5],     // Intersection start point
            [1, 1],       // License points
            [1.5, 1.2],
            [2, 1],
            [2, 0.5],     // Intersection end point
            [3, 0],       // After replacement
            [4, 0],
        ]);
    });

    test("opposite direction - reverses license points", () => {
        const refBlock = polyline([[0, 0], [1, 0], [2, 0], [3, 0], [4, 0]]);
        // License goes in the opposite direction to the ref block
        const license = polyline([[2, 1], [1.5, 1.2], [1, 1]]);
        const start = point(1, 0.5);
        const end = point(2, 0.5);

        const result = replaceSegment(refBlock, license, start, end, false);
        const coords = pathCoords(result);

        // startPoint nearest on license is index 2 ([1,1]), endPoint nearest is index 0 ([2,1])
        // licenseStartIndex(2) > licenseEndIndex(0), so fromPoint=endPoint, toPoint=startPoint
        // Opposite direction: [toPoint, ...reversed segment, fromPoint]
        //   = [(1,0.5), [1,1], [1.5,1.2], [2,1], (2,0.5)]
        expect(coords).toEqual([
            [0, 0],
            [1, 0.5],
            [1, 1],
            [1.5, 1.2],
            [2, 1],
            [2, 0.5],
            [3, 0],
            [4, 0],
        ]);
    });

    test("replacement at the start of the ref block", () => {
        const refBlock = polyline([[0, 0], [1, 0], [2, 0], [3, 0]]);
        const license = polyline([[0, 1], [0.5, 1.5], [1, 1]]);
        const start = point(0, 0.5);
        const end = point(1, 0.5);

        const result = replaceSegment(refBlock, license, start, end, true);
        const coords = pathCoords(result);

        expect(coords).toEqual([
            [0, 0.5],     // Intersection start point
            [0, 1],       // License points
            [0.5, 1.5],
            [1, 1],
            [1, 0.5],     // Intersection end point
            [2, 0],       // After replacement
            [3, 0],
        ]);
    });

    test("replacement at the end of the ref block", () => {
        const refBlock = polyline([[0, 0], [1, 0], [2, 0], [3, 0]]);
        const license = polyline([[2, 1], [2.5, 1.5], [3, 1]]);
        const start = point(2, 0.5);
        const end = point(3, 0.5);

        const result = replaceSegment(refBlock, license, start, end, true);
        const coords = pathCoords(result);

        expect(coords).toEqual([
            [0, 0],       // Before replacement
            [1, 0],
            [2, 0.5],     // Intersection start point
            [2, 1],       // License points
            [2.5, 1.5],
            [3, 1],
            [3, 0.5],     // Intersection end point
        ]);
    });

    test("replacement spans entire ref block", () => {
        const refBlock = polyline([[0, 0], [1, 0], [2, 0]]);
        const license = polyline([[0, 1], [1, 1.5], [2, 1]]);
        const start = point(0, 0.5);
        const end = point(2, 0.5);

        const result = replaceSegment(refBlock, license, start, end, true);
        const coords = pathCoords(result);

        expect(coords).toEqual([
            [0, 0.5],     // Intersection start point
            [0, 1],       // License points
            [1, 1.5],
            [2, 1],
            [2, 0.5],     // Intersection end point
        ]);
    });

    test("license line with single point between intersections", () => {
        const refBlock = polyline([[0, 0], [1, 0], [2, 0], [3, 0]]);
        const license = polyline([[1, 1]]);
        const start = point(1, 0.5);
        const end = point(1, 0.5);

        const result = replaceSegment(refBlock, license, start, end, true);
        const coords = pathCoords(result);

        // Both start and end map to the same ref block vertex (index 1) and same license vertex (index 0)
        // Result: before segment + [fromPoint, license[0], toPoint] + after segment
        expect(coords).toEqual([
            [0, 0],
            [1, 0.5],
            [1, 1],
            [1, 0.5],
            [2, 0],
            [3, 0],
        ]);
    });

    test("intersection points preserve exact coordinates, not nearest license vertex", () => {
        const refBlock = polyline([[0, 0], [1, 0], [2, 0], [3, 0]]);
        const license = polyline([[0, 1], [0.5, 1.5], [1, 1]]);
        // Intersection points differ from the nearest license vertex
        const start = point(0, 0.3);
        const end = point(1, 0.3);

        const result = replaceSegment(refBlock, license, start, end, true);
        const coords = pathCoords(result);

        expect(coords).toEqual([
            [0, 0.3],     // Intersection start point (not [0, 1])
            [0, 1],        // License points
            [0.5, 1.5],
            [1, 1],
            [1, 0.3],     // Intersection end point (not [1, 1])
            [2, 0],        // After replacement
            [3, 0],
        ]);
    });

    test("start and end points provided in reverse order", () => {
        const refBlock = polyline([[0, 0], [1, 0], [2, 0], [3, 0], [4, 0]]);
        const license = polyline([[1, 1], [1.5, 1.2], [2, 1]]);
        // Start point is closer to index 2, end point is closer to index 1 on the ref block
        const start = point(2, 0.5);
        const end = point(1, 0.5);

        const result = replaceSegment(refBlock, license, start, end, true);
        const coords = pathCoords(result);

        expect(coords).toEqual([
            [0, 0],        // Before replacement
            [1, 0.5],     // Intersection point (normalised to correct order)
            [1, 1],        // License points
            [1.5, 1.2],
            [2, 1],
            [2, 0.5],     // Intersection point
            [3, 0],        // After replacement
            [4, 0],
        ]);
    });

    test("opposite direction at end of ref block", () => {
        const refBlock = polyline([[0, 0], [1, 0], [2, 0], [3, 0], [4, 0]]);
        const license = polyline([[2, 1], [2.5, 1.5], [3, 1]]);
        const start = point(2, 0.5);
        const end = point(3, 0.5);

        const result = replaceSegment(refBlock, license, start, end, false);
        const coords = pathCoords(result);

        expect(coords).toEqual([
            [0, 0],        // Before replacement
            [1, 0],
            [3, 0.5],     // Intersection end point
            [3, 1],        // License points (reversed)
            [2.5, 1.5],
            [2, 1],
            [2, 0.5],     // Intersection start point
            [4, 0],        // After replacement
        ]);
    });
});
