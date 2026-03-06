<template>
  <div class="govuk-summary-card">
    <div class="govuk-summary-card__title-wrapper">
      <h3 class="govuk-summary-card__title">Point {{ index + 1 }}</h3>
      <ul class="govuk-summary-card__actions">
        <li class="govuk-summary-card__action">
          <button class="fds-link-button" type="button" @click="$emit('add-before')">Add before</button>
        </li>
        <li class="govuk-summary-card__action">
          <button class="fds-link-button" type="button" @click="$emit('add-after')">Add after</button>
        </li>
        <li class="govuk-summary-card__action">
          <button class="fds-link-button" type="button" @click="$emit('remove')">Remove</button>
        </li>
      </ul>
    </div>
    <div class="govuk-summary-card__content">
      <div class="coordinate-inputs">
        <div class="govuk-form-group govuk-!-margin-bottom-0">
          <label class="govuk-label" :for="'longitude-' + index">Easting</label>
          <input class="govuk-input govuk-input--width-5" :id="'longitude-' + index" :value="longitude" @input="$emit('update:longitude', $event.target.value)"/>
        </div>
        <div class="govuk-form-group govuk-!-margin-bottom-0">
          <label class="govuk-label" :for="'latitude-' + index">Northing</label>
          <input class="govuk-input govuk-input--width-5" :id="'latitude-' + index" :value="latitude" @input="$emit('update:latitude', $event.target.value)"/>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>

defineProps({
  index: {
    type: Number,
    required: true
  },
  longitude: {
    type: [String, Number],
    default: 0
  },
  latitude: {
    type: [String, Number],
    default: 0
  }
});

defineEmits(['update:longitude', 'update:latitude', 'add-before', 'add-after', 'remove']);

</script>

<style scoped>
.coordinate-inputs {
  display: flex;
  gap: 20px;
}

.govuk-summary-card__action {
  display: inline-block;
  padding-left: 0;
}

.govuk-summary-card__action:not(:last-child):after {
  content: "|";
  margin: 0 5px;
  color: #b1b4b6;
  text-decoration: none;
  display: inline-block;
}

.govuk-summary-card__actions {
  padding-left: 0;
}
</style>
