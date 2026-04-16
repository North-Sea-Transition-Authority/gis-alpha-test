<template>
  <div class="govuk-button-group govuk-!-margin-top-4">
    <button class="govuk-button" @click="split" :disabled="splitDisabled">Split</button>
    <button v-if="!undoSingleLineSegment" class="govuk-button govuk-button--secondary" @click="$emit('clear-all-lines')">Clear points</button>
    <button v-if="undoSingleLineSegment" class="govuk-button govuk-button--secondary" @click="$emit('undo-last-line')">Undo line segment</button>
    <button class="govuk-button govuk-button--secondary" @click="undo" :disabled="undoDisabled">Undo change</button>
    <button class="govuk-button govuk-button--secondary" @click="redo" :disabled="redoDisabled">Redo change</button>
  </div>
</template>

<script setup>
import {ref} from 'vue';
import {redoSplit, splitRequest, undoSplit} from "../js/api/split.api";

const splitDisabled = ref(false);
const undoDisabled = ref(false);
const redoDisabled = ref(false);

const props = defineProps({
  points: {
    type: Array,
    required: true
  },
  journeyId: {
    type: String,
    required: true
  },
  undoSingleLineSegment: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['undo-last-line', "clear-all-lines", 'split-success', 'split-error']);

async function split() {
  const validPoints = props.points.filter(p => p.originalSrsLongitude !== undefined && p.originalSrsLatitude !== undefined);
  if (validPoints.length < 2) {
    emit('split-error', 'Enter a line with at least two points.');
    return;
  }

  splitDisabled.value = true;
  try {
    const splitResponse = await splitRequest(validPoints, props.journeyId);
    if (splitResponse.outputFeatureIds.length > 0)  {
      emit('split-success');
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
  undoDisabled.value = true;
  try {
    const response = await undoSplit(props.journeyId);
    console.log(response);
    const newFeatureIds = response.outputFeatureIds;
    if (newFeatureIds.length > 0) {
      emit('split-success');
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
  redoDisabled.value = true;
  try {
    const response = await redoSplit(props.journeyId);
    console.log(response);
    const newFeatureIds = response.outputFeatureIds;
    if (newFeatureIds.length > 0) {
      emit('split-success');
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
