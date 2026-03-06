<template>
  <button class="govuk-button govuk-button--secondary govuk-!-margin-top-4" @click="$emit('clear')">Clear lines</button>
  <button class="govuk-button govuk-button--secondary govuk-!-margin-top-4 govuk-!-margin-left-2" @click="split" :disabled="splitDisabled">Split</button>
</template>

<script setup>
import {ref} from 'vue';
import {splitRequest} from "../js/api/split.api";

const props = defineProps({
  points: {
    type: Array,
    required: true
  },
  featureIds: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['clear', 'split-success', 'split-error']);

const splitDisabled = ref(false);

async function split() {
  const validPoints = props.points.filter(p => p.originalSrsLongitude !== undefined && p.originalSrsLatitude !== undefined);
  if (validPoints.length < 2) {
    return;
  }

  splitDisabled.value = true;
  try {
    const newFeatureIds = await splitRequest(validPoints, props.featureIds);
    if (newFeatureIds.length > 0) {
      emit('split-success', newFeatureIds.join(","));
    } else {
      emit('split-error', 'No split took place. Make sure your line crosses the feature boundary.');
    }
  } catch (e) {
    emit('split-error', 'An error occurred while attempting to split the features. Please try again.');
  } finally {
    splitDisabled.value = false;
  }
}
</script>
