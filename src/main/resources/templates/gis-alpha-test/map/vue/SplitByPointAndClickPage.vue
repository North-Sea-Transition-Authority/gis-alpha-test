<template>
  <single-error :errorMessage="splitError"/>
  <h1 class="govuk-heading-xl">Feature Map</h1>
  <Link :link-url="splitByCoordinateEntryUrl" link-text="Switch to split by coordinate entry" link-class="fds-link-button"/>
  <Map v-model="points" :feature-ids="currentFeatureIds" :srs-wkid="srsWkid"/>
  <split-actions :points="points" :feature-ids="currentFeatureIds" :journey-id="props.journeyId" @undo-last-line="undoLastPoint" @split-success="onSplitSuccess" @split-error="splitError = $event" />
</template>

<script setup>
import {computed, ref} from 'vue';
import SingleError from "./components/SingleError.vue";
import Link from "./components/Link.vue";
import Map from "./Map.vue";
import SplitActions from "./SplitActions.vue";

const props = defineProps({
  featureIds: String,
  srsWkid: Number,
  journeyId: String,
})
const points = ref([]);
const currentFeatureIds = ref(props.featureIds);
const splitError = ref(null);
const splitByCoordinateEntryUrl = computed(() => `/map/split/coordinate-entry?featureIds=${encodeURIComponent(currentFeatureIds.value)}`);

function undoLastPoint() {
  points.value = points.value.slice(0, -1);
}

function onSplitSuccess(newFeatureIds) {
  splitError.value = null;
  currentFeatureIds.value = newFeatureIds;
  points.value = [];
}

</script>
