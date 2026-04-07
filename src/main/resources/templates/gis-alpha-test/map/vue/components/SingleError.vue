<template>
  <div
      v-if="errorMessage"
      class="govuk-error-summary"
      aria-labelledby="error-summary-title"
      role="alert"
      data-module="govuk-error-summary"
  >
    <p class="govuk-error-message govuk-error-message--inline">
      {{ errorMessage }}
    </p>
  </div>
</template>

<script setup>
defineProps({
  errorMessage: {
    type: String
  }
})
</script>
