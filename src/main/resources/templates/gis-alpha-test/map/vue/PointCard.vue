<template>
  <div class="govuk-summary-card">
    <div class="govuk-summary-card__title-wrapper">
      <h3 class="govuk-summary-card__title">Point {{ index + 1 }}</h3>
      <ul class="govuk-summary-card__actions">
        <li class="govuk-summary-card__action">
          <button class="fds-link-button" type="button" @click="$emit('add-before')">Add before</button>
        </li>
        <li class="govuk-summary-card__action govuk-!-padding-left-2">
          <button class="fds-link-button" type="button" @click="$emit('add-after')">Add after</button>
        </li>
        <li class="govuk-summary-card__action">
          <button class="fds-link-button" type="button" @click="$emit('remove')">Remove</button>
        </li>
      </ul>
    </div>
    <div class="govuk-summary-card__content">

      <CoordinateGridInput
          v-if="srsWkid === bngWkid"
          :index="index"
          :longitude="longitudeOriginalSrs"
          :latitude="latitudeOriginalSrs"
          @update:longitude="$emit('update:longitude', $event)"
          @update:latitude="$emit('update:latitude', $event)"
      />

      <CoordinateInputDms
          v-else-if="srsWkid === ed50Wkid"
          :index="index"
          :longitude="longitudeOriginalSrs"
          :latitude="latitudeOriginalSrs"
          @update:longitude="$emit('update:longitude', $event)"
          @update:latitude="$emit('update:latitude', $event)"
      />

    </div>
  </div>
</template>

<script setup>
import {bngWkid, ed50Wkid} from "../js/coordinate-system-utils";
import CoordinateGridInput from './CoordinateGridInput.vue';
import CoordinateInputDms from './CoordinateInputDms.vue';

defineProps({
  index: {
    type: Number,
    required: true
  },
  longitudeOriginalSrs: {
    type: [String, Number],
    default: 0
  },
  latitudeOriginalSrs: {
    type: [String, Number],
    default: 0
  },
  srsWkid: {
    type:
    Number,
    required: true
  }
});

defineEmits(['update:longitude', 'update:latitude', 'add-before', 'add-after', 'remove']);

</script>
