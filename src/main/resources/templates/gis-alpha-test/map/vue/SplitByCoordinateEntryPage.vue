<template>
  <single-error :errorMessage="splitError"/>
  <notification-banner v-if="userTestingExtentText" title="Coordinate entry user testing" heading="Shape extent">
    {{ userTestingExtentText }}
  </notification-banner>
  <h1 class="govuk-heading-xl">Feature Map</h1>
  <Link :link-url="splitByPointAndClickUrl" link-text="Switch to split by point and click" link-class="fds-link-button"/>
  <div class="govuk-grid-row">
    <div class="govuk-grid-column-one-half">
      <coordinate-list v-model="coordinates" :srs-wkid="srsWkid"/>
    </div>
    <div class="govuk-grid-column-one-half">
      <Map v-model="coordinates" :display-snap-point-layer="false"  :feature-ids="currentFeatureIds" :srs-wkid="srsWkid"/>
      <split-actions :points="coordinates" :feature-ids="currentFeatureIds" :journey-id="props.journeyId" :undo-single-line-segment="false" @clear-all-lines="clear" @split-success="onSplitSuccess" @split-error="splitError = $event" />
    </div>
  </div>
</template>

<script setup>

import {computed, ref} from "vue";
import CoordinateList from "./CoordinateList.vue";
import Map from "./Map.vue";
import SplitActions from "./SplitActions.vue";
import SingleError from "./components/SingleError.vue";
import {bngToWgs84, bngWkid, ed50ToWgs84, ed50Wkid} from "../js/coordinate-system-utils";
import Link from "./components/Link.vue";
import NotificationBanner from "./components/NotificationBanner.vue";

const props = defineProps({
  featureIds: String,
  srsWkid: Number,
  journeyId: String,
  userTestingExtentText: {
    type: String,
    required: false
  }
});

const splitByPointAndClickUrl = computed(() => `/map/split/point-and-click?featureIds=${encodeURIComponent(currentFeatureIds.value)}`);

function createInitialCoordinate() {
  let wgs84Lon, wgs84Lat;

  if (props.srsWkid === ed50Wkid) {
    [wgs84Lon, wgs84Lat] = ed50ToWgs84(0, 0);
  } else if (props.srsWkid === bngWkid) {
    [wgs84Lon, wgs84Lat] = bngToWgs84(0, 0);
  } else {
    throw new Error(`Unsupported SRS WKID: ${props.srsWkid}`);
  }

  return { id: 0, longitude: wgs84Lon, latitude: wgs84Lat, originalSrsLongitude: 0, originalSrsLatitude: 0 };
}

const initialCoordinate = createInitialCoordinate();

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
