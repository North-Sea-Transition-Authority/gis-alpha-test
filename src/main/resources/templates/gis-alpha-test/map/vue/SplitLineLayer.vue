<template>
  <ol-vector-layer>
    <ol-source-vector>
      <!-- The line connecting all points -->
      <ol-feature v-if="coordinates.length > 1">
        <ol-geom-line-string :coordinates="lineCoordinates" />
        <ol-style>
          <ol-style-stroke color="red" :width="2" />
        </ol-style>
      </ol-feature>

      <!-- Individual points -->
      <ol-feature v-for="point in coordinates" :key="point.id">
        <ol-geom-point :coordinates="[Number(point.longitude), Number(point.latitude)]" />
        <ol-style>
          <ol-style-circle :radius="4">
            <ol-style-fill color="blue" />
            <ol-style-stroke color="white" :width="1" />
          </ol-style-circle>
        </ol-style>
      </ol-feature>
    </ol-source-vector>
  </ol-vector-layer>
</template>

<script setup>
import {computed} from 'vue';

const props = defineProps({
  coordinates: {
    type: Array,
    required: true
  }
});

const lineCoordinates = computed(() => {
  return props.coordinates
    .filter(p => !Number.isNaN(p.longitude) && !Number.isNaN(p.latitude))
    .map(p => [Number(p.longitude), Number(p.latitude)]);
});
</script>
