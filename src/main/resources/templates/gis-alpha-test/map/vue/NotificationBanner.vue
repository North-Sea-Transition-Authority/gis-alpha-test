<template>
  <div v-if="message" class="govuk-notification-banner" role="alert">
    <div class="govuk-notification-banner__header">
      <h2 class="govuk-notification-banner__title">Important</h2>
    </div>
    <div class="govuk-notification-banner__content">
      <p class="govuk-body">{{ message }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  message: String
})
</script>
