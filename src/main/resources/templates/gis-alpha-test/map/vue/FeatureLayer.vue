<template>
  <ol-vector-layer :style="featureStyle" :declutter="true">
    <ol-source-vector
      ref="vectorSourceRef"
      :url="featuresUrl"
      :format="esriJson"
      @featuresloadend="fitToExtent"
  />
  </ol-vector-layer>
</template>

<script setup>
import {Fill, Stroke, Style, Text} from "ol/style";
import {EsriJSON} from "ol/format";
import {computed, nextTick, ref, watch} from "vue";

const props = defineProps({
  journeyId: {
    type: String,
    required: true
  },
  refreshCounter: {
    type: Number,
    required: true
  },
  olMap: Object,
  fillColor: {
    type: Array,
    required: false,
    default: [255, 221 ,0] //yellow
  },
  strokeColor:  {
    type: Array,
    required: false,
    default: [0, 0, 0, 1] //black
  },
  currentDepthLevel: {
    type: Number,
    default: null
  },
  viewMultipleDepths: {
    type: Boolean,
    default: false
  },
})
const esriJson = new EsriJSON();
const vectorSourceRef = ref(null);
const featuresUrl = computed(() => !props.viewMultipleDepths
    ? `/map/esrijson/${props.journeyId}`
    : `/map/block-and-subareas/esrijson/${props.journeyId}`
);

watch(() => props.refreshCounter, async () => {
  await nextTick();
  const source = vectorSourceRef.value?.source;
  if (source?.refresh) {
    source.refresh();
  }
});


function featureStyle(feature) {
  const isCurrentDepthEqualToStartDepth = props.viewMultipleDepths && (
      Number(feature.get("startDepth") ?? 0) >= Number(props.currentDepthLevel) &&
      Number(feature.get("endDepth") ?? -100000) < Number(props.currentDepthLevel)
  );

  if (!isCurrentDepthEqualToStartDepth) {
    return new Style({stroke: new Stroke({
        color: [0, 0, 0, 0, 0],
        width: 0
      }),
    });
  }
  return new Style({
    stroke: new Stroke({
      color: [...props.strokeColor, 1],
      width: 2
    }),
    fill: new Fill({
      color: [...props.fillColor, 0.50],
    }),
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