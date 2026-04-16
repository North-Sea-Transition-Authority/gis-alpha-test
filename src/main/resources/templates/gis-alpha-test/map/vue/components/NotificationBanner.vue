<template>
  <div
      class="govuk-notification-banner"
      role="region"
      aria-labelledby="govuk-notification-banner-title"
      data-module="govuk-notification-banner"
  >
    <div class="govuk-notification-banner__header">
      <h2 class="govuk-notification-banner__title" id="govuk-notification-banner-title">
        {{ title }}
      </h2>
    </div>
    <div class="govuk-notification-banner__content">

      <h3 v-if="heading" class="govuk-notification-banner__heading">
        {{ heading }}
      </h3>

      <p class="govuk-body">
        <slot></slot>
      </p>

    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  heading: {
    type: String,
    default: ''
  }
})
</script>