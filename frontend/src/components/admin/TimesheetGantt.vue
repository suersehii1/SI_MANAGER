<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { buildGanttOption, GANTT_COLORS } from '@/utils/gantt'

// 타임시트 Gantt: y = 사용자·프로젝트, 계획 막대(연한) + 실제 막대(진한, asOf 적용된 유효 종료일까지)
// height: 보이는 영역 높이. 지정하면(예: '100%') 차트는 행 수만큼 커지고 이 영역 안에서 세로 스크롤된다.
//         지정하지 않으면 예전처럼 차트 높이만큼 늘어난다.
const props = defineProps({
  data: { type: Object, default: null },
  height: { type: String, default: null }
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

// 행 하나당 44px로 차트 높이를 정한다 (사용자가 많아도 막대가 찌그러지지 않도록)
const chartHeight = computed(() => `${Math.max(240, rows.value.length * 44 + 90)}px`)
</script>

<template>
  <div data-testid="timesheet-gantt" class="timesheet-gantt" :style="{ height: props.height || 'auto' }">
    <v-chart :option="option" autoresize :style="{ height: chartHeight }" />
  </div>
</template>

<style scoped>
.timesheet-gantt {
  overflow-y: auto;
}
</style>
