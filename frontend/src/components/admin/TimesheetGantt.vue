<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { buildGanttOption, GANTT_COLORS } from '@/utils/gantt'

// 타임시트 Gantt: y = 사용자·프로젝트, 계획 막대(연한) + 실제 막대(진한, asOf 적용된 유효 종료일까지)
const props = defineProps({
  data: { type: Object, default: null }
})

const rows = computed(() => {
  const list = []
  for (const u of props.data?.rows || []) {
    for (const p of u.projects) list.push({ user: u, p })
  }
  return list
})

const option = computed(() => {
  const asOf = props.data?.asOf
  const categories = rows.value.map(({ user, p }) => `${user.name} · ${p.projectCode}`)
  const bars = []
  rows.value.forEach(({ p }, row) => {
    const name = categories[row]
    bars.push({ row, layer: 'plan', start: p.plannedStart, end: p.plannedEnd, color: GANTT_COLORS.plan, name })
    if (p.actualStart && p.actualStart <= asOf) {
      const end = p.actualEnd && p.actualEnd < asOf ? p.actualEnd : asOf
      bars.push({ row, layer: 'actual', start: p.actualStart, end, color: GANTT_COLORS.actual, name })
    }
  })
  return buildGanttOption({ categories, bars, markDate: asOf })
})

const height = computed(() => `${Math.max(240, rows.value.length * 44 + 90)}px`)
</script>

<template>
  <div data-testid="timesheet-gantt">
    <v-chart :option="option" autoresize :style="{ height }" />
  </div>
</template>
