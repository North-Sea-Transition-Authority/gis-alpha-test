<template>
  <div class="coordinate-inputs">
    <div class="govuk-form-group govuk-!-margin-bottom-0">
      <label class="govuk-label" :for="'longitude-' + index">Easting</label>
      <input
          class="govuk-input govuk-input--width-5"
          :id="'longitude-' + index"
          :value="longitude"
          @input="$emit('update:longitude', $event.target.value)"
      />
    </div>
    <div class="govuk-form-group govuk-!-margin-bottom-0">
      <label class="govuk-label" :for="'latitude-' + index">Northing</label>
      <input
          class="govuk-input govuk-input--width-5"
          :id="'latitude-' + index"
          :value="latitude"
          @input="$emit('update:latitude', $event.target.value)"
      />
    </div>
  </div>
</template>

<script setup>
defineProps({
  index: { type: Number, required: true },
  longitude: { type: [String, Number], required: true },
  latitude: { type: [String, Number], required: true }
});

defineEmits(['update:longitude', 'update:latitude']);
</script>

<style scoped>
.coordinate-inputs {
  display: flex;
  gap: 20px;
}
</style>