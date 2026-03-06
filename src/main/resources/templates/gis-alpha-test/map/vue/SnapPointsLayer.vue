<template>
  <ol-vector-layer>
    <ol-source-vector>
      <ol-feature v-for="point in snapPoints" :key="point.id">
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
      <ol-feature v-if="previewLine">
        <ol-geom-line-string :coordinates="previewLine.coordinates" />
        <ol-style>
          <ol-style-stroke color="red" :width="2" :lineDash="[5, 5]" />
        </ol-style>
      </ol-feature>
    </ol-source-vector>
  </ol-vector-layer>

  <ol-overlay v-if="hoveredPoint" :position="hoveredPoint.coordinates" positioning="top-left" :offset="[8, 8]">
    <div class="snap-tooltip">
      {{ hoveredPoint.displayName }}
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
import {ref, watch} from "vue";
import {debounce} from "../../../../js/debounce";
import {getSnapPoints} from "../js/api/snap-points.api";
import {bngToWgs84, bngWkid, ed50ToWgs84, ed50Wkid, wgs84ToBng, wgs84ToEd50} from "../js/coordinate-system-utils";

const snapPoints = ref([]);
const selectedPoint = ref(null);
const hoveredPoint = ref(null);
const previewLine = ref(null);

const props = defineProps({
  olMap: Object,
  coordinates: Array,
  srsWkid: Number
})

const emit = defineEmits(['update:coordinates'])

const getMinSnapZoom = () => {
  if (props.srsWkid === ed50Wkid) {
    return 11;
  } else if (props.srsWkid === bngWkid) {
    return 12;
  }
  throw new Error(`Unsupported SRS WKID in SnapPointsLayer: ${props.srsWkid}`);
}

watch(() => props.coordinates, (points) => {
  if (points.length === 0) {
    selectedPoint.value = null;
    hoveredPoint.value = null;
    previewLine.value = null;
  } else {
    const lastPoint = points[points.length - 1];
    selectedPoint.value = {
      id: lastPoint.id,
      coordinates: [lastPoint.longitude, lastPoint.latitude],
      originalSrsCoordinates: [lastPoint.originalSrsLongitude, lastPoint.originalSrsLatitude],
    };
  }
}, { immediate: true });


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

  if (map.getView().getZoom() < getMinSnapZoom()) {
    snapPoints.value = [];
    return;
  }

  const [wgs84MinLon, wgs84MinLat, wgs84MaxLon, wgs84MaxLat] = map.getView().calculateExtent(map.getSize());
  snapPoints.value = await getSnapPoints(wgs84MinLon, wgs84MinLat, wgs84MaxLon, wgs84MaxLat, props.srsWkid);
};

const regeneratePointsOnBrowser = () => {
  const map = props.olMap?.map;
  if (!map) {
    return;
  }

  //Only generate points above this zoom level, to avoid running out of memory.
  if (map.getView().getZoom() < getMinSnapZoom()) {
    snapPoints.value = [];
    return;
  }

  // useGeographic() makes calculateExtent return WGS84 (EPSG:4326)
  const [wgs84MinLon, wgs84MinLat, wgs84MaxLon, wgs84MaxLat] = map.getView().calculateExtent(map.getSize());

  let [originalSrsMinLon, originalSrsMinLat] = [];
  let [originalSrsMaxLon, originalSrsMaxLat] = [];
  if (props.srsWkid === ed50Wkid) {
    [originalSrsMinLon, originalSrsMinLat] = wgs84ToEd50(wgs84MinLon, wgs84MinLat);
    [originalSrsMaxLon, originalSrsMaxLat] = wgs84ToEd50(wgs84MaxLon, wgs84MaxLat);
  } else if (props.srsWkid === bngWkid) {
    [originalSrsMinLon, originalSrsMinLat] = wgs84ToBng(wgs84MinLon, wgs84MinLat);
    [originalSrsMaxLon, originalSrsMaxLat] = wgs84ToBng(wgs84MaxLon, wgs84MaxLat);
  }


  let spacing;
  if (props.srsWkid === ed50Wkid) {
    spacing = 30 / 3600; // 30 arc seconds in decimal degrees
  } else if (props.srsWkid === bngWkid) {
    spacing = 500; //500 metres
  }
  const originalSrsStartLon = Math.floor(originalSrsMinLon / spacing) * spacing;
  const originalSrsStartLat = Math.floor(originalSrsMinLat / spacing) * spacing;

  const pointsArray = [];
  for (let originalSrsLon = originalSrsStartLon; originalSrsLon <= originalSrsMaxLon; originalSrsLon += spacing) {
    for (let originalSrsLat = originalSrsStartLat; originalSrsLat <= originalSrsMaxLat; originalSrsLat += spacing) {

      let [wgs84Lon, wgs84Lat] = [];
      if (props.srsWkid === ed50Wkid) {
        [wgs84Lon, wgs84Lat] = ed50ToWgs84(originalSrsLon, originalSrsLat);
      } else if (props.srsWkid === bngWkid) {
        [wgs84Lon, wgs84Lat] = bngToWgs84(originalSrsLon, originalSrsLat);
      }

      let displayName;
      if (props.srsWkid === ed50Wkid) {
        displayName = `${toDegreesMinutesSeconds(originalSrsLat, true)} ${toDegreesMinutesSeconds(originalSrsLon, false)}`;
      } else if (props.srsWkid === bngWkid) {
        displayName = `${originalSrsLon}E ${originalSrsLat}N`;
      }

      pointsArray.push({
        id: `${originalSrsLon},${originalSrsLat}`,
        coordinates: [wgs84Lon, wgs84Lat],
        originalSrsCoordinates: [originalSrsLon, originalSrsLat],
        displayName,
      });
    }
  }
  snapPoints.value = pointsArray;
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

  for (const point of snapPoints.value) {
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
  if (!hoveredPoint.value) {
    return;
  }

  if (selectedPoint.value && (hoveredPoint.value.id === selectedPoint.value.id || !pointsAreAligned(hoveredPoint.value, selectedPoint.value))) {
    return;
  }

  const newPoint = {
    id: hoveredPoint.value.id,
    longitude: hoveredPoint.value.coordinates[0],
    latitude: hoveredPoint.value.coordinates[1],
    originalSrsLongitude: hoveredPoint.value.originalSrsCoordinates[0],
    originalSrsLatitude: hoveredPoint.value.originalSrsCoordinates[1]
  };

  emit('update:coordinates', [...props.coordinates, newPoint]);

  selectedPoint.value = hoveredPoint.value;
  hoveredPoint.value = null;
  previewLine.value = null;
};

const handleMouseLeave = () => {
  hoveredPoint.value = null;
  previewLine.value = null;
};

const toDegreesMinutesSeconds = (decimal, isLat) => {
  const absolute = Math.abs(decimal);
  const degrees = Math.floor(absolute);
  const minutesDecimal = (absolute - degrees) * 60;
  const minutes = Math.floor(minutesDecimal);
  const seconds = ((minutesDecimal - minutes) * 60).toFixed(1);
  const dir = isLat ? (decimal >= 0 ? 'N' : 'S') : (decimal >= 0 ? 'E' : 'W');
  return `${degrees}°${String(minutes).padStart(2, '0')}'${String(seconds).padStart(4, '0')}"${dir}`;
};
</script>