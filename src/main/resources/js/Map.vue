<template>
  <ol-map class="map">
    <ol-view :center="[0, 7361866]" :zoom="7" />

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
  </ol-map>
</template>

<script setup>
  const polygonCoordinates = [
    [
      [0, 7361866],
      [500000, 7361866],
      [500000, 7861866],
      [0, 7861866],
      [0, 7361866]
    ]
  ];
</script>

<style scoped>
  .map {
    aspect-ratio: 1 / 1;
  }
</style>
