# Approach for snap points that works for map projection and shape SRS

## Problem definition

Open layers and the shapes we want to display use different coordinate systems. We want to know what is the best approach
to display our shapes and other layers on a map while avoiding shifts in the position.

## Coordinate systems

There are different kinds of coordinates systems: Geographic and projected. Each system works a bit different.

### Geographic coordinate system

This system uses a sphere or ellipsoid to represent coordinates on a 3D shape that reassembles the earth or part of it.
They use degrees as a unit of measurement.

### Projected coordinate system

This system projects the earth into a 2D plane. They use linear units of measurement such as metres. This system is 
composed of a geographic coordinate system and a map projection together. The map projection 
contains the mathematical calculations to transform a geographic coordinate system into Cartesian coordinates
than can be used in the planar projected system.


## Open layers

Open layers uses the Web Mercator projection (EPSG:3857) by default. This is a projected system. However, it allows us to
provide us with geographic coordinates by calling the `useGeographic()` function.
This function instructs open layers to read all coordinates in its vector layers as WGS84 coordinates, which is a geographic
system in degrees.

Open layers will then internally transform from WGS84 to Web Mercator to display the geometries in the map. This is an exact
mathematical transformation as both systems use the same geographic ellipsoid.

## Transforming shapes between different coordinate systems

the ArcGis JS library supports transforming geometries between different coordinate systems (SRS) usign the `projectOperator`.
The operator provides a list of possible mathematical transformations for the given SRS. It defaults to the best approach but
this can be overridden.

The NSTA defines `ED_1950_To_WGS_1984_18`as the specific transformation to be used when converting between ED50 and WGS84 on 
this [document](https://www.nstauthority.co.uk/media/1405/2888-guidance-notes-use-of-coordinate-systems.pdf) Annex A section 4.


Example:

```typescript
import * as geographicTransformationUtils
    from "@arcgis/core/geometry/operators/support/geographicTransformationUtils.js";
import * as projectOperator from "@arcgis/core/geometry/operators/projectOperator.js";
import SpatialReference from "@arcgis/core/geometry/SpatialReference.js";
import GeographicTransformationStep from "@arcgis/core/geometry/operators/support/GeographicTransformationStep";
import Point from "@arcgis/core/geometry/Point.js";
import GeographicTransformation from "@arcgis/core/geometry/operators/support/GeographicTransformation";

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
```

The code above result on `x: 1.0000000493521939,` and `y: 53.00000011323153` which is within a 1-2 cm difference of the 
expected result, which is expected due to rounding of the decimal degrees.


### Issues when converting between different coordinate systems

Converting between coordinate systems that do not use the same ellipsoid is not an exact calculation and better considered
an approximation. This is the case when transforming ED50 shapes into WGS84.
The EPSG database has multiple ED50 → WGS84 transformations with different accuracy levels for different parts of the North Sea.
Using a region-specific transformation WKID rather than a global one would give better accuracy as per the code above.

### Changing the coordinate system open layers use

We can change the coordinate system the maps use. However, this is not ideal.

We could set the coordinate system to be ED50 and avoid transforming our geometries. But, there are no base layers we could
use for our map.

If we changed it to use WGS84 the map will look distorted, and different from what other online maps look like.

## Proposed approach

- Transform our geometries to WGS84 using the `projectOperator` and send them to the front end.
- Configure open layers to accept WGS84 coordinates by using `useGeographic()`

### For the snapping points

- Generate the snap points server side in the same coordinate system as our target geometry
- Convert them to WGS84 while mantaining the original ED50 coordinates attached to each point.
- Send them to the front end where the map will use the WGS84 coordinates to display them.
- When viewing coordinates on screen, for example when hovering over points or text description we will use the original coordinate system coordinates rather than WGS84 ones.
- When triggering any operation we will send back the original ED50 coordinates associated with each point.

This should ensure the snap points and geometries display on the same relative context, while still preserving the original SRS
coordinates required for operations where the coordinates selected by the user are important such as splits. 

## References

- https://pro.arcgis.com/en/pro-app/latest/help/mapping/properties/coordinate-systems-and-projections.htm
- https://openlayers.org/doc/faq.html#what-projection-is-openlayers-using-
- https://openlayers.org/en/latest/examples/geographic.html
- https://developers.arcgis.com/javascript/latest/api-reference/esri-geometry-operators-projectOperator.html
- https://www.nstauthority.co.uk/media/1405/2888-guidance-notes-use-of-coordinate-systems.pdf
- https://pro.arcgis.com/en/pro-app/latest/help/mapping/properties/pdf/geographic_transformations.pdf