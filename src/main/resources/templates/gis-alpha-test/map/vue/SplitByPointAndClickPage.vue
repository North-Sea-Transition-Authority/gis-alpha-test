<template>
  <single-error :errorMessage="splitError"/>
  <h1 class="govuk-heading-xl">Feature Map</h1>
  <Link :link-url="splitByCoordinateEntryUrl" link-text="Switch to split by coordinate entry" link-class="fds-link-button"/>
  <Map v-model="points" :journey-id="journeyId" :refresh-counter="refreshCounter" :srs-wkid="srsWkid"/>
  <split-actions :points="points" :journey-id="journeyId" @undo-last-line="undoLastPoint" @split-success="onSplitSuccess" @split-error="splitError = $event" />
</template>

<script setup>
import {computed, ref} from 'vue';
import SingleError from "./components/SingleError.vue";
import Link from "./components/Link.vue";
import Map from "./Map.vue";
import SplitActions from "./SplitActions.vue";

const props = defineProps({
  journeyId: String,
  srsWkid: Number,
})
const refreshCounter = ref(0);
const points = ref([]);
const splitError = ref(null);
const splitByCoordinateEntryUrl = computed(() => `/map/split/coordinate-entry/${props.journeyId}`);

function undoLastPoint() {
  points.value = points.value.slice(0, -1);
}

function onSplitSuccess() {
  splitError.value = null;
  refreshCounter.value++;
  points.value = [];
}

</script>
