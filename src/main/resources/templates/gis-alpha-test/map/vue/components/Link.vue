<template>
  <a :href="linkUrl" :class="linkClass">
    {{ linkText }}
  </a>
</template>

<script setup>
defineProps({
  linkText: {
    type: String,
    required: true
  },
  linkUrl: {
    type: String,
    required: true
  },
  linkClass: {
    type: String,
    default: "govuk-link"
  }
})
</script>