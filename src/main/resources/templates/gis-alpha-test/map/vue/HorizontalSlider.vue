<template>
  <div class="container">
      <input
          type="range"
          class="slider"
          :min="0"
          :max="Math.max(sortedSnapPoints.length - 1, 0)"
          :value="currentIndex"
          @input="onSliderInput"
      />
      <div class="markers">
        <div
            v-for="(id, index) in sortedSnapPoints"
            :key="id"
            class="marker"
            :class="{ 'marker--active': id === modelValue }"
            :style="{ left: getSnapPointPosition(index) + '%' }"
            @click="setDepth(id)"
        >
          <span class="marker-label govuk-label">{{ options.get(id) }}</span>
        </div>
      </div>
  </div>
</template>

<script setup>

import {computed} from 'vue';

const props = defineProps({
  options: {
    type: Map,
    required: true
  }
});

const modelValue = defineModel({
  type: Number,
  required: true
})

const sortedSnapPoints = computed(() => {
  return [...props.options.keys()].sort((a, b) => a - b);
});

const currentIndex = computed(() => {
  const index = sortedSnapPoints.value.indexOf(modelValue.value);
  return index >= 0 ? index : 0;
});

function getSnapPointPosition(index) {
  if (sortedSnapPoints.value.length <= 1) {
    return 50;
  }
  return  (index / (sortedSnapPoints.value.length - 1)) * 100;
}

function setDepth(depth) {
  modelValue.value = depth;
}

function onSliderInput(event) {
  const index = Number(event.target.value);
  const depth = sortedSnapPoints.value[index];

  if (depth !== undefined) {
    setDepth(depth);
  }
}

</script>

<style scoped>
.container {
  flex: 1;
  min-width: 300px;

  position: relative;
  padding-top: 12px;
  padding-bottom: 35px;
}

/* slider */

.slider {
  width: 80%;
  height: 8px;
  -webkit-appearance: none;
  appearance: none;
  background: #0b0c0c;
  cursor: pointer;
  position: relative;
  z-index: 2;
  left: 10%
}

.slider:focus {
  outline: 3px solid #ffdd00;
}

/* thumb */
.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 28px;
  background: #ffffff;
  cursor: pointer;
  border: 2px solid #0b0c0c;
}

.slider::-moz-range-thumb {
  width: 20px;
  height: 28px;
  background: #ffffff;
  border-radius: 0;
  cursor: pointer;
  border: 2px solid #0b0c0c;
}

.slider:focus::-webkit-slider-thumb {
  box-shadow: 0 0 0 3px #ffdd00;
}

.slider:focus::-moz-range-thumb {
  box-shadow: 0 0 0 3px #ffdd00;
}

/* markers */
.markers {
  width: 80%;
  position: relative;
  top: 0.5rem;
  left: 10%;
  right: 0;
  height: 30px;
  z-index: 1;
}

.marker {
  position: absolute;
  transform: translateX(-50%);
  cursor: pointer;
}

.marker::before {
  content: '';
  display: block;
  width: 0.2rem;
  height: 0.5rem;
  background: #505a5f;
  margin: 0 auto;
}

.marker--active::before {
  background: #0b0c0c;
  width: 0.3rem;
}

/* label */
.marker-label {
  display: block;
  white-space: nowrap;
}

.marker--active .marker-label {
  font-weight: bold;
}

</style>
