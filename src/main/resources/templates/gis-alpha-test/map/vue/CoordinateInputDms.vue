<template>
  <div class="coordinate-groups">
    <div :class="['govuk-form-group govuk-!-margin-bottom-0', { 'govuk-form-group--error': latError }]">
      <fieldset class="govuk-fieldset">
        <legend class="govuk-fieldset__legend govuk-fieldset__legend--s">Latitude</legend>

        <p v-if="latError" :id="'lat-error-' + index" class="govuk-error-message">
          <span class="govuk-visually-hidden">Error:</span> {{ latError }}
        </p>

        <div class="coordinate-inputs">
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="'lat-deg-' + index">Degrees</label>
            <input
                :class="['govuk-input govuk-input--width-3', { 'govuk-input--error': latError }]"
                :id="'lat-deg-' + index"
                type="number"
                v-model="localLat.d"
                @input="validateAndEmitLat"
            />
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="'lat-min-' + index">Minutes</label>
            <input
                :class="['govuk-input govuk-input--width-3', { 'govuk-input--error': latError }]"
                :id="'lat-min-' + index"
                type="number"
                v-model="localLat.m"
                @input="validateAndEmitLat"
            />
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="'lat-sec-' + index">Seconds</label>
            <input
                :class="['govuk-input govuk-input--width-4', { 'govuk-input--error': latError }]"
                :id="'lat-sec-' + index"
                type="number"
                v-model="localLat.s"
                @input="validateAndEmitLat"
            />
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="'lat-hemi-' + index">Hemisphere</label>
            <select
                :class="['govuk-select hemisphere-select', { 'govuk-select--error': latError }]"
                :id="'lat-hemi-' + index"
                v-model="localLat.hemi"
                @change="validateAndEmitLat"
            >
              <option value="N">North</option>
              <option value="S">South</option>
            </select>
          </div>
        </div>
      </fieldset>
    </div>
    <div :class="['govuk-form-group', { 'govuk-form-group--error': lonError }]">
      <fieldset class="govuk-fieldset govuk-!-margin-bottom-3">
        <legend class="govuk-fieldset__legend govuk-fieldset__legend--s">Longitude</legend>

        <p v-if="lonError" :id="'lon-error-' + index" class="govuk-error-message">
          <span class="govuk-visually-hidden">Error:</span> {{ lonError }}
        </p>

        <div class="coordinate-inputs">
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="'lon-deg-' + index">Degrees</label>
            <input
                :class="['govuk-input govuk-input--width-3', { 'govuk-input--error': lonError }]"
                :id="'lon-deg-' + index"
                type="number"
                v-model="localLon.d"
                @input="validateAndEmitLon"
            />
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="'lon-min-' + index">Minutes</label>
            <input
                :class="['govuk-input govuk-input--width-3', { 'govuk-input--error': lonError }]"
                :id="'lon-min-' + index"
                type="number"
                v-model="localLon.m"
                @input="validateAndEmitLon"
            />
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="'lon-sec-' + index">Seconds</label>
            <input
                :class="['govuk-input govuk-input--width-4', { 'govuk-input--error': lonError }]"
                :id="'lon-sec-' + index"
                type="number"
                v-model="localLon.s"
                @input="validateAndEmitLon"
            />
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="'lon-hemi-' + index">Hemisphere</label>
            <select
                :class="['govuk-select hemisphere-select', { 'govuk-select--error': lonError }]"
                :id="'lon-hemi-' + index"
                v-model="localLon.hemi"
                @change="validateAndEmitLon"
            >
              <option value="E">East</option>
              <option value="W">West</option>
            </select>
          </div>
        </div>
      </fieldset>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref} from 'vue';
import {toDecimalDegrees, toDMS} from '../js/coordinate-system-utils';

const props = defineProps({
  index: { type: Number, required: true },
  longitude: { type: [String, Number], required: true },
  latitude: { type: [String, Number], required: true }
});

const emit = defineEmits(['update:longitude', 'update:latitude']);

const localLon = reactive(toDMS(props.longitude, false));
const localLat = reactive(toDMS(props.latitude, true));

const lonError = ref('');
const latError = ref('');

function validateAndEmitLon() {
  lonError.value = '';

  const d = parseFloat(localLon.d);
  const m = parseFloat(localLon.m);
  const s = parseFloat(localLon.s);

  if (isNaN(d) || isNaN(m) || isNaN(s)) {
    return;
  }

  if (d < 0 || d > 180) {
    lonError.value = 'Longitude degrees must be between 0 and 180.';
    return;
  }
  if (m < 0 || m > 59) {
    lonError.value = 'Minutes must be between 0 and 59.';
    return;
  }
  if (s < 0 || s >= 60) {
    lonError.value = 'Seconds must be between 0 and 59.999.';
    return;
  }

  emit('update:longitude', toDecimalDegrees(d, m, s, localLon.hemi));
}

function validateAndEmitLat() {
  latError.value = '';

  const d = parseFloat(localLat.d);
  const m = parseFloat(localLat.m);
  const s = parseFloat(localLat.s);

  if (isNaN(d) || isNaN(m) || isNaN(s)) {
    return;
  }

  if (d < 0 || d > 90) {
    latError.value = 'Latitude degrees must be between 0 and 90.';
    return;
  }
  if (m < 0 || m > 59) {
    latError.value = 'Minutes must be between 0 and 59.';
    return;
  }
  if (s < 0 || s >= 60) {
    latError.value = 'Seconds must be between 0 and 59.999.';
    return;
  }

  emit('update:latitude', toDecimalDegrees(d, m, s, localLat.hemi));
}
</script>

<style scoped>
.coordinate-groups {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.coordinate-inputs {
  display: flex;
  gap: 15px;
}
.hemisphere-select {
  width: 120px;
  min-width: 120px;
}
</style>