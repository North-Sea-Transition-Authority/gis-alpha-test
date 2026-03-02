<template>
  <notification-banner :message="splitError"/>
  <ol-map class="map" ref="mapRef" tabindex="0">
    <ol-view :center="[0, 0]" :zoom="2" :maxZoom="15"/>
    <ol-tile-layer>
      <ol-source-osm />
    </ol-tile-layer>
    <nsta-quadrant-layer :ol-map="mapRef"/>
    <nsta-quadrant-block-layer :ol-map="mapRef"/>
    <snap-points-layer :ol-map="mapRef" v-model:lines="lines"/>
    <feature-layer :featureIds="currentFeatureIds" :ol-map="mapRef"/>
  </ol-map>
  <button class="govuk-button govuk-button--secondary govuk-!-margin-top-4" @click="deleteLines">Clear lines</button>
  <button class="govuk-button govuk-button--secondary govuk-!-margin-top-4 govuk-!-margin-left-2" @click="split" :disabled="splitDisabled">Split</button>
</template>

<script setup>
import {ref} from 'vue';
import {useGeographic} from "ol/proj";
import FeatureLayer from './FeatureLayer.vue';
import SnapPointsLayer from "./SnapPointsLayer.vue";
import NstaQuadrantLayer from "./NstaQuadrantLayer.vue";
import NstaQuadrantBlockLayer from "./NstaQuadrantBlockLayer.vue";
import NotificationBanner from "./NotificationBanner.vue";
import {splitRequest} from "../js/api/split.api";

const props = defineProps({
  featureIds: String
})
const mapRef = ref(null);
const lines = ref([]);
const splitDisabled = ref(false);
const currentFeatureIds = ref(props.featureIds);
const splitError = ref(null);

//Open layers uses the WebMercator coordinate system which is a projected system that uses metres for coordinates.
//`useGeographic` allows us to use WGS84 geographic coordinate system on the map that uses degrees for coordinates
//This allow us to use a common geographic system to display our geometries on the map.
useGeographic()

function deleteLines() {
  lines.value = [];
}

async function split() {
  if (!lines.value.length) {
    return;
  }
  splitDisabled.value = true;
  splitError.value = null;
  const newFeatureIds = await splitRequest(lines.value, currentFeatureIds.value);
  if (newFeatureIds.length > 0) {
    currentFeatureIds.value = newFeatureIds.join(",");
    deleteLines();
  } else {
    splitError.value = 'No split took place. Make sure your line crosses the feature boundary.';
  }
  splitDisabled.value = false;
}

</script>

<style scoped>
  .map {
    aspect-ratio: 1 / 1;
  }
</style>