<template>
  <button class="govuk-button govuk-button--secondary govuk-!-margin-top-4" @click="$emit('clear')">Clear lines</button>
  <button class="govuk-button govuk-button--secondary govuk-!-margin-top-4 govuk-!-margin-left-2" @click="split" :disabled="splitDisabled">Split</button>
  <button class="govuk-button govuk-button--secondary govuk-!-margin-top-4 govuk-!-margin-left-2" @click="undo" :disabled="undoDisabled">Undo</button>
  <button class="govuk-button govuk-button--secondary govuk-!-margin-top-4 govuk-!-margin-left-2" @click="redo" :disabled="redoDisabled">Redo</button>
</template>

<script setup>
import {onMounted, ref} from 'vue';
import {redoSplit, splitRequest, undoSplit} from "../js/api/split.api";

const journeyId = ref(null);
const splitDisabled = ref(false);
const undoDisabled = ref(false);
const redoDisabled = ref(false);

const props = defineProps({
  points: {
    type: Array,
    required: true
  },
  featureIds: {
    type: String,
    required: true
  },
  journeyId: {
    type: String,
    required: false
  }
})

onMounted(() => {
  journeyId.value = props.journeyId;
})

const emit = defineEmits(['clear', 'split-success', 'split-error']);

async function split() {
  const validPoints = props.points.filter(p => p.originalSrsLongitude !== undefined && p.originalSrsLatitude !== undefined);
  if (validPoints.length < 2) {
    return;
  }

  splitDisabled.value = true;
  try {
    const splitResponse = await splitRequest(validPoints, props.featureIds);
    if (splitResponse.outputFeatureIds.length > 0) {
      journeyId.value = splitResponse.commandJourneyId;
      emit('split-success', splitResponse.outputFeatureIds.join(","));
    } else {
      emit('split-error', 'No split took place. Make sure your line crosses the feature boundary.');
    }
  } catch (e) {
    emit('split-error', 'An error occurred while attempting to split the features. Please try again.');
  } finally {
    splitDisabled.value = false;
  }
}

async function undo() {
  if (!journeyId.value) {
    console.log("No journey ID");
    emit('split-error', 'Nothing to undo');
    return;
  }
  console.log('journeyId: ', journeyId.value);
  undoDisabled.value = true;
  try {
    const response = await undoSplit(journeyId.value);
    console.log(response);
    const newFeatureIds = response.outputFeatureIds;
    if (newFeatureIds.length > 0) {
      emit('split-success', newFeatureIds.join(","));
      journeyId.value = response.commandJourneyId;
    } else {
      emit('split-error', 'Nothing to undo');
    }
  } catch (e) {
    emit('split-error', 'An error occurred while attempting to undo. Please try again.');
  } finally {
    undoDisabled.value = false;
  }
}

async function redo() {
  if (!journeyId.value) {
    console.log("No journey ID");
    emit('split-error', 'Nothing to redo.');
    return;
  }
  console.log('journeyId: ', journeyId.value);
  redoDisabled.value = true;
  try {
    const response = await redoSplit(journeyId.value);
    console.log(response);
    const newFeatureIds = response.outputFeatureIds;
    if (newFeatureIds.length > 0) {
      emit('split-success', newFeatureIds.join(","));
      journeyId.value = response.commandJourneyId;
    } else {
      emit('split-error', 'Nothing to redo.');
    }
  } catch (e) {
    emit('split-error', 'An error occurred while attempting to redo. Please try again.');
  } finally {
    redoDisabled.value = false;
  }
}
</script>
