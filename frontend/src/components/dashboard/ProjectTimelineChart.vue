<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { buildGanttOption, GANTT_COLORS } from '@/utils/gantt'

const props = defineProps({
  projects: { type: Array, default: () => [] },
  asOf: { type: String, default: null }
})

// 프로젝트별 계획 기간(연한) + 실제 기간(진한, 진행 중이면 asOf까지) 간트
const option = computed(() => {
  const categories = props.projects.map((p) => `${p.code} ${p.name}`)
  const bars = []
  props.projects.forEach((p, row) => {
    bars.push({ row, layer: 'plan', start: p.plannedStartDate, end: p.plannedEndDate, color: GANTT_COLORS.plan, name: categories[row] })
    if (p.actualStartDate) {
      bars.push({
        row,
        layer: 'actual',
        start: p.actualStartDate,
        end: p.actualEndDate || props.asOf,
        color: GANTT_COLORS.actual,
        name: categories[row]
      })
    }
  })
  return buildGanttOption({ categories, bars, markDate: props.asOf })
})

const height = computed(() => `${Math.max(200, props.projects.length * 48 + 90)}px`)
</script>

<template>
  <v-chart :option="option" autoresize :style="{ height }" />
</template>
