<template>
  <div>
    <point-card
      v-for="(point, index) in modelValue"
      :key="point.id"
      :index="index"
      :longitudeOriginalSrs="point.originalSrsLongitude"
      :latitudeOriginalSrs="point.originalSrsLatitude"
      @update:longitude="updateLongitude(index, $event)"
      @update:latitude="updateLatitude(index, $event)"
      @add-before="addBefore(index)"
      @add-after="addAfter(index)"
      @remove="remove(index)"
    />
  </div>
</template>

<script setup>

import PointCard from "./PointCard.vue";
import {bngToWgs84, bngWkid, ed50ToWgs84, ed50Wkid} from "../js/coordinate-system-utils";

const modelValue = defineModel({
  type: Array,
  required: true
});

const props = defineProps({
  srsWkid: {
    type: Number,
    required: true
  }
});

let nextId = 1;

function createPoint() {
  const point = { id: nextId++, originalSrsLongitude: 0, originalSrsLatitude: 0 };
  updateWgs84Coordinates(point);
  return point;
}

function updateWgs84Coordinates(point) {
  const originalSrsLon = Number(point.originalSrsLongitude);
  const originalSrsLat = Number(point.originalSrsLatitude);
  const isLonValid = point.originalSrsLongitude !== undefined && point.originalSrsLongitude !== '' && !isNaN(originalSrsLon);
  const isLatValid = point.originalSrsLatitude !== undefined && point.originalSrsLatitude !== '' && !isNaN(originalSrsLat);

  let [wgs84Lon, wgs84Lat] = [];
  if (isLonValid && isLatValid) {
    if (props.srsWkid === ed50Wkid) {
      [wgs84Lon, wgs84Lat] = ed50ToWgs84(originalSrsLon, originalSrsLat);
    } else if (props.srsWkid === bngWkid) {
      [wgs84Lon, wgs84Lat] = bngToWgs84(originalSrsLon, originalSrsLat);
    } else {
      throw new Error(`Unsupported SRS WKID: ${props.srsWkid}`);
    }

    point.longitude = wgs84Lon;
    point.latitude = wgs84Lat;
  } else {
    point.longitude = undefined;
    point.latitude = undefined;
  }
}

function updateLongitude(index, originalSrsLon) {
  const updated = [...modelValue.value];
  const point = { ...updated[index], originalSrsLongitude: originalSrsLon };
  updateWgs84Coordinates(point);
  updated[index] = point;
  modelValue.value = updated;
}

function updateLatitude(index, originalSrsLat) {
  const updated = [...modelValue.value];
  const point = { ...updated[index], originalSrsLatitude: originalSrsLat };
  updateWgs84Coordinates(point);
  updated[index] = point;
  modelValue.value = updated;
}

function addBefore(index) {
  const updated = [...modelValue.value];
  updated.splice(index, 0, createPoint());
  modelValue.value = updated;
}

function addAfter(index) {
  const updated = [...modelValue.value];
  updated.splice(index + 1, 0, createPoint());
  modelValue.value = updated;
}

function remove(index) {
  if (modelValue.value.length <= 1) {
    return;
  }
  const updated = [...modelValue.value];
  updated.splice(index, 1);
  modelValue.value = updated;
}
</script>

<style scoped>

</style>
