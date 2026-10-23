<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'

// 막대: 월별 계획/실제 MM, 선: 누계 계획/실제 MM. null(미래 월 실제값)은 선이 끊긴다.
const props = defineProps({
  series: { type: Array, default: () => [] }
})

const option = computed(() => {
  const months = props.series.map((s) => s.month)
  const num = (v) => (v === null || v === undefined ? null : Number(v))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['계획 MM', '실제 MM', '누계 계획', '누계 실제'] },
    grid: { left: 50, right: 50, top: 50, bottom: 40 },
    xAxis: { type: 'category', data: months },
    yAxis: [
      { type: 'value', name: '월 MM' },
      { type: 'value', name: '누계 MM' }
    ],
    series: [
      { name: '계획 MM', type: 'bar', data: props.series.map((s) => num(s.plannedMM)), itemStyle: { color: '#a0cfff' } },
      { name: '실제 MM', type: 'bar', data: props.series.map((s) => num(s.actualMM)), itemStyle: { color: '#409eff' } },
      { name: '누계 계획', type: 'line', yAxisIndex: 1, data: props.series.map((s) => num(s.cumulativePlannedMM)), lineStyle: { type: 'dashed' } },
      { name: '누계 실제', type: 'line', yAxisIndex: 1, connectNulls: false, data: props.series.map((s) => num(s.cumulativeActualMM)) }
    ]
  }
})
</script>

<template>
  <div data-testid="plan-actual-chart">
    <v-chart :option="option" autoresize style="height: 360px" />
  </div>
</template>
