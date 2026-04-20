<template>
  <ol-map class="map" ref="mapRef" tabindex="0">
    <ol-view :center="[0, 0]" :zoom="2" :maxZoom="15"/>
    <ol-tile-layer>
      <ol-source-osm />
    </ol-tile-layer>
    <nsta-quadrant-layer :ol-map="mapRef"/>
    <nsta-quadrant-block-layer :ol-map="mapRef"/>
    <snap-points-layer v-if="displaySnapPointLayer" :ol-map="mapRef" v-model:coordinates="modelValue" :srsWkid="props.srsWkid"/>
    <feature-layer
        :key="currentDepthLevel"
        :journey-id="journeyId"
        :refresh-counter="refreshCounter"
        :ol-map="mapRef"
        :current-depth-level="currentDepthLevel"
        :view-multiple-depths="viewMultipleDepths"
    />
    <split-line-layer :coordinates="modelValue"/>
  </ol-map>
</template>

<script setup>
import {ref} from 'vue';
import {useGeographic} from "ol/proj";
import FeatureLayer from './FeatureLayer.vue';
import SnapPointsLayer from "./SnapPointsLayer.vue";
import SplitLineLayer from "./SplitLineLayer.vue";
import NstaQuadrantLayer from "./NstaQuadrantLayer.vue";
import NstaQuadrantBlockLayer from "./NstaQuadrantBlockLayer.vue";

const modelValue = defineModel({
  type: Array,
  required: true
})

const props = defineProps({
  journeyId: {
    type: String,
    required: true
  },
  refreshCounter: {
    type: Number,
    required: true
  },
  displaySnapPointLayer: {
    type: Boolean,
    default: true
  },
  srsWkid: {
    type: Number,
    required: true
  },
  currentDepthLevel: {
    type: Number,
    default: undefined
  },
  viewMultipleDepths: {
    type: Boolean,
    default: false
  },
})

const mapRef = ref(null);

//Open layers uses the WebMercator coordinate system which is a projected system that uses metres for coordinates.
//`useGeographic` allows us to use WGS84 geographic coordinate system on the map that uses degrees for coordinates
//This allow us to use a common geographic system to display our geometries on the map.
useGeographic()

</script>

<style scoped>
.map {
  aspect-ratio: 1 / 1;
}
</style>