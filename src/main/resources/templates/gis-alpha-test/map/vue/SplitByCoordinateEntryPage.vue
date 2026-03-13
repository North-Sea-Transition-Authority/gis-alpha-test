<template>
  <notification-banner :message="splitError" />
  <div class="govuk-grid-row">
    <div class="govuk-grid-column-one-half">
      <coordinate-list v-model="coordinates" />
    </div>
    <div class="govuk-grid-column-one-half">
      <Map v-model="coordinates" :display-snap-point-layer="false"  :feature-ids="currentFeatureIds" :srs-wkid="srsWkid"/>
      <split-actions :points="coordinates" :feature-ids="currentFeatureIds" :journey-id="props.journeyId" @clear="clear" @split-success="onSplitSuccess" @split-error="splitError = $event" />
    </div>
  </div>
</template>

<script setup>

import {ref} from "vue";
import CoordinateList from "./CoordinateList.vue";
import Map from "./Map.vue";
import SplitActions from "./SplitActions.vue";
import NotificationBanner from "./NotificationBanner.vue";
import {wgs84ToEd50} from "../js/coordinate-system-utils";

const props = defineProps({
  featureIds: String,
  srsWkid: Number,
  journeyId: String,
});

const [initialOriginalSrsLon, initialOriginalSrsLat] = wgs84ToEd50(0, 0);
const initialCoordinate = { id: 0, longitude: 0, latitude: 0, originalSrsLongitude: initialOriginalSrsLon, originalSrsLatitude: initialOriginalSrsLat };

const coordinates = ref([initialCoordinate]);
const currentFeatureIds = ref(props.featureIds);
const splitError = ref(null);

function clear() {
  coordinates.value = [{ ...initialCoordinate }];
}

function onSplitSuccess(newFeatureIds) {
  splitError.value = null;
  currentFeatureIds.value = newFeatureIds;
  clear();
}

</script>
