<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppShell from './components/AppShell.vue'

const route = useRoute()
const isProtected = computed(() => route.meta.requiresAuth === true)
</script>

<template>
  <AppShell v-if="isProtected"><RouterView /></AppShell>
  <RouterView v-else />
</template>
