<template>
  <div>
    <point-card
      v-for="(point, index) in modelValue"
      :key="point.id"
      :index="index"
      :longitude="point.longitude"
      :latitude="point.latitude"
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
import {wgs84ToEd50} from "../js/coordinate-system-utils";

const modelValue = defineModel({
  type: Array,
  required: true
});

let nextId = 1;

function createPoint() {
  const point = { id: nextId++, longitude: 0, latitude: 0 };
  updateOriginalSrs(point);
  return point;
}

function updateOriginalSrs(point) {
  const lon = Number(point.longitude);
  const lat = Number(point.latitude);
  const isLonValid = point.longitude !== undefined && point.longitude !== '' && !isNaN(lon);
  const isLatValid = point.latitude !== undefined && point.latitude !== '' && !isNaN(lat);

  if (isLonValid && isLatValid) {
    const [originalSrsLon, originalSrsLat] = wgs84ToEd50(lon, lat);
    point.originalSrsLongitude = originalSrsLon;
    point.originalSrsLatitude = originalSrsLat;
  } else {
    point.originalSrsLongitude = undefined;
    point.originalSrsLatitude = undefined;
  }
}

function updateLongitude(index, value) {
  const updated = [...modelValue.value];
  const point = { ...updated[index], longitude: value };
  updateOriginalSrs(point);
  updated[index] = point;
  modelValue.value = updated;
}

function updateLatitude(index, value) {
  const updated = [...modelValue.value];
  const point = { ...updated[index], latitude: value };
  updateOriginalSrs(point);
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
