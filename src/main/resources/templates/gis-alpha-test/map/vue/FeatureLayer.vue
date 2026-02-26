<template>
  <ol-vector-layer :style="featureStyle" :declutter="true">
    <ol-source-vector :url="featuresUrl" :format="esriJson" @featuresloadend="fitToExtent">
    </ol-source-vector>
  </ol-vector-layer>
</template>

<script setup>
import {Fill, Stroke, Style, Text} from "ol/style";
import {EsriJSON} from "ol/format";

const props = defineProps({
  featureIds: String,
  olMap: Object
})

const featuresUrl = `/map/esrijson?featureIds=${encodeURIComponent(props.featureIds)}`;
const esriJson = new EsriJSON();

function featureStyle(feature) {
  return new Style({
    stroke: new Stroke({color: 'rgba(0, 100, 210, 0.8)', width: 2}),
    fill: new Fill({color: 'rgba(0, 100, 210, 0.15)'}),
    text: new Text({
      text: feature.get('featureName') || '',
      font: '14px sans-serif',
      fill: new Fill({color: '#000'}),
      stroke: new Stroke({color: '#fff', width: 3}),
      overflow: true,
    }),
  });
}

function fitToExtent(event) {
  const map = props.olMap?.map;
  if (!map) {
    return;
  }

  const source = event.target;
  const extent = source.getExtent();

  if (extent && isFinite(extent[0])) {
    map.getView().fit(extent, {
      padding: [50, 50, 50, 50],
    });
  }
}
</script>