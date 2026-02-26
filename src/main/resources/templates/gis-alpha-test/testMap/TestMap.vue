<template>
  <ol-map class="map" ref="mapRef">
    <ol-view :center="[0, 7361866]" :zoom="15" :minZoom="14" />

    <ol-tile-layer>
      <ol-source-osm />
    </ol-tile-layer>

    <ol-vector-layer>
      <ol-source-vector>
        <ol-feature>
          <ol-geom-polygon :coordinates="polygonCoordinates" />
        </ol-feature>
      </ol-source-vector>
    </ol-vector-layer>

    <ol-vector-layer>
      <ol-source-vector>
        <ol-feature v-for="point in points" :key="point.id">
          <ol-geom-point :coordinates="point.coordinates" />
          <ol-style>
            <ol-style-circle :radius="3">
              <ol-style-fill color="grey" />
            </ol-style-circle>
          </ol-style>
        </ol-feature>
      </ol-source-vector>
    </ol-vector-layer>

    <ol-vector-layer>
      <ol-source-vector>
        <ol-feature v-if="hoveredPoint">
          <ol-geom-point :coordinates="hoveredPoint.coordinates" />
          <ol-style>
            <ol-style-circle :radius="3">
              <ol-style-fill color="red" />
            </ol-style-circle>
          </ol-style>
        </ol-feature>
      </ol-source-vector>
    </ol-vector-layer>

    <ol-vector-layer>
      <ol-source-vector>
        <ol-feature v-if="selectedPoint">
          <ol-geom-point :coordinates="selectedPoint.coordinates" />
          <ol-style>
            <ol-style-circle :radius="4">
              <ol-style-fill color="blue" />
            </ol-style-circle>
          </ol-style>
        </ol-feature>
      </ol-source-vector>
    </ol-vector-layer>

    <ol-vector-layer>
      <ol-source-vector>
        <ol-feature v-if="previewLine">
          <ol-geom-line-string :coordinates="previewLine.coordinates" />
          <ol-style>
            <ol-style-stroke color="red" :width="2" :lineDash="[5, 5]" />
          </ol-style>
        </ol-feature>
      </ol-source-vector>
    </ol-vector-layer>

    <ol-vector-layer>
      <ol-source-vector>
        <ol-feature v-for="line in lines" :key="line.id">
          <ol-geom-line-string :coordinates="line.coordinates" />
          <ol-style>
            <ol-style-stroke color="red" :width="2" />
          </ol-style>
        </ol-feature>
      </ol-source-vector>
    </ol-vector-layer>
  </ol-map>
</template>

<script setup>
import {onMounted, ref} from 'vue';
import {transform} from 'ol/proj';
import {debounce} from '../../../js/debounce';

const polygonCoordinates = [
    [
      [-1250, 7360616],
      [1250, 7360616],
      [1250, 7363116],
      [-1250, 7363116],
      [-1250, 7360616]
    ]
  ];

  const mapRef = ref(null);

  const points = ref([]);
  const hoveredPoint = ref(null);
  const selectedPoint = ref(null);

  const previewLine = ref(null);
  const lines = ref([]);

  const regeneratePoints = () => {
    const map = mapRef.value?.map;
    if (!map) {
      return;
    }

    // EPSG:3857 is the EPSG code for the Web Mercator projection, which is what openlayers uses.
    // EPSG:4326 is the EPSG code for the WGS84 projection.

    const view = map.getView();
    const [epsg3857MinLon, epsg3857MinLat, epsg3857MaxLon, epsg3857MaxLat] = view.calculateExtent(map.getSize());

    const [epsg4326MinLon, epsg4326MinLat] = transform([epsg3857MinLon, epsg3857MinLat], 'EPSG:3857', 'EPSG:4326');
    const [epsg4326MaxLon, epsg4326MaxLat] = transform([epsg3857MaxLon, epsg3857MaxLat], 'EPSG:3857', 'EPSG:4326');

    const arcSecondSpacing = 10 / 3600; // 10 arc seconds in decimal degrees

    const epsg4326StartLon = Math.floor(epsg4326MinLon / arcSecondSpacing) * arcSecondSpacing;
    const epsg4326StartLat = Math.floor(epsg4326MinLat / arcSecondSpacing) * arcSecondSpacing;

    const pointsArray = [];

    for (let epsg4326Lon = epsg4326StartLon; epsg4326Lon <= epsg4326MaxLon; epsg4326Lon += arcSecondSpacing) {
      for (let epsg4326Lat = epsg4326StartLat; epsg4326Lat <= epsg4326MaxLat; epsg4326Lat += arcSecondSpacing) {
        const [epsg3857Lon, epsg3857Lat] = transform([epsg4326Lon, epsg4326Lat], 'EPSG:4326', 'EPSG:3857');
        pointsArray.push({
          id: `${epsg4326Lon},${epsg4326Lat}`,
          coordinates: [epsg3857Lon, epsg3857Lat],
        });
      }
    }

    points.value = pointsArray;
  };

  const handleMouseMove = (event) => {
    const mouseCoordinates = event.coordinate;

    if (!selectedPoint.value) {
      hoveredPoint.value = findNearestPoint(mouseCoordinates);
      return;
    }

    const nearestAlignedPoint = findNearestPoint(mouseCoordinates, (point) => point.id !== selectedPoint.value.id && pointsAreAligned(point, selectedPoint.value));

    hoveredPoint.value = nearestAlignedPoint;

    if (nearestAlignedPoint) {
      previewLine.value = {
        coordinates: [selectedPoint.value.coordinates, nearestAlignedPoint.coordinates]
      };
    } else {
      previewLine.value = null;
    }
  };

  const handleMapClick = () => {
    if (!selectedPoint.value) {
      selectedPoint.value = hoveredPoint.value;
      hoveredPoint.value = null;
      return;
    }

    if (!hoveredPoint.value
        || hoveredPoint.value.id === selectedPoint.value.id
        || !pointsAreAligned(hoveredPoint.value, selectedPoint.value)) {
      return;
    }

    lines.value.push({
      id: `${selectedPoint.value.id},${hoveredPoint.value.id}`,
      coordinates: [selectedPoint.value.coordinates, hoveredPoint.value.coordinates]
    });

    selectedPoint.value = null;
    hoveredPoint.value = null;
    previewLine.value = null;
  };

  const findNearestPoint = (coordinates, filter) => {
    let nearestPoint = null;
    let minDistance = Infinity;

    for (const point of points.value) {
      if (filter && !filter(point)) {
        continue;
      }

      const lonDelta = point.coordinates[0] - coordinates[0];
      const latDelta = point.coordinates[1] - coordinates[1];
      const distance = Math.sqrt(lonDelta * lonDelta + latDelta * latDelta);

      if (distance < minDistance) {
        minDistance = distance;
        nearestPoint = point;
      }
    }

    return nearestPoint;
  };

  const pointsAreAligned = (pointA, pointB) => pointA.coordinates[0] === pointB.coordinates[0]
      || pointA.coordinates[1] === pointB.coordinates[1];

  onMounted(() => {
    regeneratePoints();

    const map = mapRef.value?.map;
    if (map) {
      const debouncedRegeneratePoints = debounce(regeneratePoints, 200);

      map.getView().on('change:resolution', debouncedRegeneratePoints);
      map.getView().on('change:center', debouncedRegeneratePoints);

      map.on('pointermove', handleMouseMove);
      map.on('click', handleMapClick);
    }
  });
</script>

<style scoped>
  .map {
    aspect-ratio: 1 / 1;
  }
</style>
