<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { useWbsStore } from '@/stores/wbs'
import { buildGanttOption, GANTT_COLORS } from '@/utils/gantt'

// WBS Gantt: y = "코드 작업명"(visibleRows 순서), 계획(연한) + 실제(진한, 종료 없으면 asOf까지) + 진척률 오버레이 + asOf 선
const store = useWbsStore()

const option = computed(() => {
  const rows = store.visibleRows
  const asOf = store.asOf
  const categories = rows.map((r) => `${r.wbsCode} ${r.name}`)
  const bars = []
  rows.forEach((r, row) => {
    const name = categories[row]
    if (r.plannedStart && r.plannedEnd) {
      bars.push({ row, layer: 'plan', start: r.plannedStart, end: r.plannedEnd, color: r.delayed ? '#fab6b6' : GANTT_COLORS.plan, name })
    }
    if (r.actualStart && (!asOf || r.actualStart <= asOf)) {
      const end = r.actualEnd || asOf
      const color = r.status === 'DONE' ? GANTT_COLORS.actualDone : r.delayed ? GANTT_COLORS.delayed : GANTT_COLORS.actual
      bars.push({ row, layer: 'actual', start: r.actualStart, end, color, name, progress: Number(r.progress) })
    }
  })
  return buildGanttOption({ categories, bars, markDate: asOf })
})

const height = computed(() => `${Math.max(260, store.visibleRows.length * 36 + 90)}px`)
</script>

<template>
  <div data-testid="wbs-gantt">
    <v-chart :option="option" autoresize :style="{ height }" />
  </div>
</template>
