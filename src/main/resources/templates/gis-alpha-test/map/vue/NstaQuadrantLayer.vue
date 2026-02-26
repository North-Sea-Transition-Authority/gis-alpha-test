<template>

</template>

<script setup>
import {watch} from "vue";
import VectorLayer from "ol/layer/Vector";
import {Fill, Stroke, Style} from "ol/style";
import {buildServiceUrl, createPaginatedVectorSource} from "../js/nsta-data-source"

const quadrantUrl = buildServiceUrl('UKCS_quadrants_(WGS84)', 'QUADRANT');

const props = defineProps({
  olMap: Object
})

watch(() => props.olMap, (olMap) => {
  const map = olMap?.map;
  if (!map) {
    return;
  }

  map.addLayer(new VectorLayer({
    source: createPaginatedVectorSource(quadrantUrl),
    style: quadrantStyle,
    declutter: true,
  }));
});

const quadrantStyle = new Style({
  fill: new Fill({color: 'rgba(0, 0, 0, 0)'}),
  stroke: new Stroke({color: 'rgba(0, 46, 109, 0.8)', width: 1.5}),
});

</script>