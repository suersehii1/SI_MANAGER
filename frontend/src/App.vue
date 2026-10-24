<script setup>
import { computed, provide } from 'vue'
import { THEME_KEY } from 'vue-echarts'
import { useAuthStore } from '@/stores/auth'
import { CHART_DARK_THEME } from '@/utils/theme'

// 모든 차트(vue-echarts)에 현재 스킨에 맞는 테마를 넘긴다 (밤이면 다크 차트 테마, 낮이면 기본)
const auth = useAuthStore()
provide(THEME_KEY, computed(() => (auth.theme === 'DARK' ? CHART_DARK_THEME : undefined)))
</script>

<template>
  <router-view />
</template>
