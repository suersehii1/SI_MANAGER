<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'

const props = defineProps({
  projects: { type: Array, default: () => [] }
})

// 프로젝트별 할당 인원 vs 현재 투입 인원 막대 차트
const option = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['할당 인원', '투입 중', '계획상 투입'] },
  grid: { left: 40, right: 20, top: 40, bottom: 40 },
  xAxis: { type: 'category', data: props.projects.map((p) => p.code) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    { name: '할당 인원', type: 'bar', data: props.projects.map((p) => p.assignedHeadcount) },
    { name: '투입 중', type: 'bar', data: props.projects.map((p) => p.activeHeadcount) },
    { name: '계획상 투입', type: 'bar', data: props.projects.map((p) => p.plannedActiveHeadcount) }
  ]
}))
</script>

<template>
  <v-chart :option="option" autoresize style="height: 300px" />
</template>
