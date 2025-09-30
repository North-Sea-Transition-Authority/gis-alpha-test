<template>
  <ol-map class="map" ref="mapRef">
    <ol-view :center="[-14199.5, 6712165.3]" :zoom="17" :minZoom="13" />

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
  </ol-map>
</template>

<script setup>
  import { ref, onMounted } from 'vue';
  import { transform } from 'ol/proj';
  import { debounce } from './debounce';

  const polygonCoordinates = [
    [
      [0, 7361866],
      [500000, 7361866],
      [500000, 7861866],
      [0, 7861866],
      [0, 7361866]
    ]
  ];

  const mapRef = ref(null);
  const points = ref([]);
  let nextPointId = 0;

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
          id: nextPointId++,
          coordinates: [epsg3857Lon, epsg3857Lat]
        });
      }
    }

    points.value = pointsArray;
  };

  const debouncedRegeneratePoints = debounce(regeneratePoints, 200);

  onMounted(() => {
    regeneratePoints();

    const map = mapRef.value?.map;
    if (map) {
      map.getView().on('change:resolution', debouncedRegeneratePoints);
      map.getView().on('change:center', debouncedRegeneratePoints);
    }
  });
</script>

<style scoped>
  .map {
    aspect-ratio: 1 / 1;
  }
</style>
