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

// 행 하나당 36px로 차트 높이를 정한다 (항목이 많아도 막대가 찌그러지지 않도록)
const chartHeight = computed(() => `${Math.max(260, store.visibleRows.length * 36 + 90)}px`)

// height: 보이는 영역 높이. 지정하면(예: '100%') 차트는 행 수만큼 커지고 이 영역 안에서 세로 스크롤된다.
const props = defineProps({
  height: { type: String, default: null }
})
</script>

<template>
  <div data-testid="wbs-gantt" class="wbs-gantt" :style="{ height: props.height || 'auto' }">
    <v-chart :option="option" autoresize :style="{ height: chartHeight }" />
  </div>
</template>

<style scoped>
.wbs-gantt {
  overflow-y: auto;
}
</style>
