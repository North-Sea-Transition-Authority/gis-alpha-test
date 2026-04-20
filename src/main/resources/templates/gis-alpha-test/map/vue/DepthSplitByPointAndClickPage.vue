<template>
  <HorizontalSlider v-model="currentDepthLevel" :options="depths"/>
  <MapView
      v-model="points"
      :journey-id="featureId"
      :refresh-counter="refreshCounter"
      :current-depth-level="currentDepthLevel"
      :srs-wkid="srsWkid"
      :view-multiple-depths="true"
  />
</template>

<script setup>
import {computed, ref} from 'vue';
import MapView from "./Map.vue";
import HorizontalSlider from "./HorizontalSlider.vue";

const props = defineProps({
  featureId: String,
  featureIdAndDepths: {
    type: Array,
    default: () => []
  },
  srsWkid: Number,
})

const refreshCounter = ref(0);

const points = ref([]);

const depths = computed(() => {
  const startDepths = [...new Set(
      props.featureIdAndDepths.map((item) => (item.startDepth === null ? 0 : item.startDepth))
  )].sort((left, right) => left - right);

  if (!startDepths.length) {
    return new Map([[0, "All depths"]]);
  }

  const depthMap = new Map([[startDepths[0], `-infinity to ${startDepths[0]}`]]);

  for (let i = 1; i < startDepths.length; i += 1) {
    depthMap.set(startDepths[i], `${startDepths[i - 1]} to ${startDepths[i]}`);
  }
  return depthMap;
});


const currentDepthLevel = ref(Math.max(...depths.value.keys()));
</script>
