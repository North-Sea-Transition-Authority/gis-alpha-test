<template>

</template>

<script setup>
import {watch} from "vue";
import VectorLayer from "ol/layer/Vector";
import {Fill, Stroke, Style} from "ol/style";
import {buildServiceUrl, createPaginatedVectorSource} from "../js/nsta-data-source"

const quadBlockUrl = buildServiceUrl('UKCS_blocks_(WGS84)', 'BLOCK_REF');

const props = defineProps({
  olMap: Object
})

watch(() => props.olMap, (olMap) => {
  const map = olMap?.map;
  if (!map) {
    return;
  }

  map.addLayer(new VectorLayer({
    source: createPaginatedVectorSource(quadBlockUrl),
    style: quadBlockStyle,
    declutter: true,
  }));
});

const quadBlockStyle = new Style({
  fill: new Fill({color: 'rgba(0, 0, 0, 0)'}),
  stroke: new Stroke({color: 'rgba(0, 46, 109, 0.8)', width: 0.5}),
});

</script>