<template>
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

  <ol-overlay v-if="hoveredPoint" :position="hoveredPoint.coordinates" positioning="top-left" :offset="[8, 8]">
    <div class="snap-tooltip">
      {{ hoveredPoint.originalSrsCoordinates[1].toFixed(4) }}°N {{ hoveredPoint.originalSrsCoordinates[0].toFixed(4) }}°E
    </div>
  </ol-overlay>
</template>

<style scoped>
.snap-tooltip {
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  white-space: nowrap;
}
</style>

<script setup>
import proj4 from "proj4";
import {ref, watch} from "vue";
import {debounce} from "../../../../js/debounce";
import {getSnapPoints} from "../js/api/snap-points.api";

//definition for ED50 (EPSG:4230) as per https://spatialreference.org/ref/epsg/4230/
proj4.defs('EPSG:4230', '+proj=longlat +ellps=intl +towgs84=-87,-98,-121,0,0,0,0 +no_defs');

const MIN_SNAP_ZOOM = 11;
const points = ref([]);
const selectedPoint = ref(null);
const hoveredPoint = ref(null);
const previewLine = ref(null);

const props = defineProps({
  olMap: Object,
  lines: Array,
  srsWkid: Number
})

const emit = defineEmits(['update:lines'])

watch(() => props.lines, (lines) => {
  if (lines.length === 0) {
    selectedPoint.value = null;
    hoveredPoint.value = null;
    previewLine.value = null;
  }
});


watch(() => props.olMap, (olMap) => {
  const map = olMap?.map;
  if (!map) {
    return;
  }

  regeneratePointsOnBrowser();
  const debouncedRegenerate = debounce(regeneratePointsOnBrowser, 200);
  map.getView().on('change:resolution', debouncedRegenerate);
  map.getView().on('change:center', debouncedRegenerate);
  map.on('pointermove', handleMouseMove);
  map.on('click', handleMapClick);
  map.getViewport().addEventListener('pointerleave', handleMouseLeave);
});

//Decided not to use as this will just be used for visualisation. We dont need the NSTA approved transformation so we can avoid the SBA -> Node app calls.
const regeneratePointsNodeServer = async () => {
  const map = props.olMap?.map;
  if (!map) {
    return;
  }

  if (map.getView().getZoom() < MIN_SNAP_ZOOM) {
    points.value = [];
    return;
  }

  const [wgs84MinLon, wgs84MinLat, wgs84MaxLon, wgs84MaxLat] = map.getView().calculateExtent(map.getSize());
  points.value = await getSnapPoints(wgs84MinLon, wgs84MinLat, wgs84MaxLon, wgs84MaxLat, props.srsWkid);
};

const regeneratePointsOnBrowser = () => {
  const map = props.olMap?.map;
  if (!map) {
    return;
  }

  //Only generate points above this zoom level, to avoid running out of memory.
  if (map.getView().getZoom() < MIN_SNAP_ZOOM) {
    points.value = [];
    return;
  }

  // useGeographic() makes calculateExtent return WGS84 (EPSG:4326)
  const [wgs84MinLon, wgs84MinLat, wgs84MaxLon, wgs84MaxLat] = map.getView().calculateExtent(map.getSize());

  // Transform WGS84 viewport bounds to ED50 (EPSG:4230)
  const [originalSrsMinLon, originalSrsMinLat] = proj4('EPSG:4326', 'EPSG:4230', [wgs84MinLon, wgs84MinLat]);
  const [originalSrsMaxLon, originalSrsMaxLat] = proj4('EPSG:4326', 'EPSG:4230', [wgs84MaxLon, wgs84MaxLat]);

  const arcSecondSpacing = 30 / 3600; // 30 arc seconds in decimal degrees
  const originalSrsStartLon = Math.floor(originalSrsMinLon / arcSecondSpacing) * arcSecondSpacing;
  const originalSrsStartLat = Math.floor(originalSrsMinLat / arcSecondSpacing) * arcSecondSpacing;

  const pointsArray = [];
  for (let originalSrsLon = originalSrsStartLon; originalSrsLon <= originalSrsMaxLon; originalSrsLon += arcSecondSpacing) {
    for (let originalSrsLat = originalSrsStartLat; originalSrsLat <= originalSrsMaxLat; originalSrsLat += arcSecondSpacing) {
      // Transform ED50 (EPSG:4230) to WGS84 (EPSG:4326) for display
      const [wgs84Lon, wgs84Lat] = proj4('EPSG:4230', 'EPSG:4326', [originalSrsLon, originalSrsLat]);
      pointsArray.push({
        id: `${originalSrsLon},${originalSrsLat}`,
        coordinates: [wgs84Lon, wgs84Lat],
        originalSrsCoordinates: [originalSrsLon, originalSrsLat],
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

const pointsAreAligned = (pointA, pointB) => pointA.originalSrsCoordinates[0] === pointB.originalSrsCoordinates[0]
    || pointA.originalSrsCoordinates[1] === pointB.originalSrsCoordinates[1];

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

  emit('update:lines', [...props.lines, {
    id: `${selectedPoint.value.id},${hoveredPoint.value.id}`,
    coordinates: [selectedPoint.value.coordinates, hoveredPoint.value.coordinates],
    originalSrsCoordinates: [selectedPoint.value.originalSrsCoordinates, hoveredPoint.value.originalSrsCoordinates]
  }]);


  selectedPoint.value = hoveredPoint.value;
  hoveredPoint.value = null;
  previewLine.value = null;
};

const handleMouseLeave = () => {
  hoveredPoint.value = null;
  previewLine.value = null;
};
</script>