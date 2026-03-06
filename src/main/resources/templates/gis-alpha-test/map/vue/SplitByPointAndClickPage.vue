<template>
  <notification-banner :message="splitError"/>
  <Map v-model="points" :feature-ids="currentFeatureIds" :srs-wkid="srsWkid"/>
  <split-actions :points="points" :feature-ids="currentFeatureIds" @clear="deleteLines" @split-success="onSplitSuccess" @split-error="splitError = $event" />
</template>

<script setup>
import {ref} from 'vue';
import NotificationBanner from "./NotificationBanner.vue";
import Map from "./Map.vue";
import SplitActions from "./SplitActions.vue";

const props = defineProps({
  featureIds: String,
  srsWkid: Number
})
const points = ref([]);
const currentFeatureIds = ref(props.featureIds);
const splitError = ref(null);

function deleteLines() {
  points.value = [];
}

function onSplitSuccess(newFeatureIds) {
  splitError.value = null;
  currentFeatureIds.value = newFeatureIds;
  deleteLines();
}

</script>
