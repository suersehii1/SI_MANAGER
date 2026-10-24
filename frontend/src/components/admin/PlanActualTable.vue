<script setup>
import { AgGridVue } from 'ag-grid-vue3'
import { defaultColDef, localeText, GRID_THEME_CLASS } from '@/utils/grid'
import { formatMM } from '@/utils/format'

// height: 표 높이 (기본 360px, 부모 영역을 꽉 채우려면 '100%')
defineProps({
  series: { type: Array, default: () => [] },
  height: { type: String, default: '360px' }
})

// 차이(음수 = 계획 대비 실제 부족)는 빨간색
const negative = { 'mm-over': (p) => p.value !== null && Number(p.value) < 0 }
const mm = (field, headerName, extra = {}) => ({
  field, headerName, width: 130, type: 'numericColumn', valueFormatter: (p) => formatMM(p.value), ...extra
})

const columnDefs = [
  { field: 'month', headerName: '월', width: 110, pinned: 'left' },
  mm('plannedMM', '계획 MM'),
  mm('actualMM', '실제 MM'),
  mm('varianceMM', '차이', { cellClassRules: negative }),
  mm('cumulativePlannedMM', '누계 계획'),
  mm('cumulativeActualMM', '누계 실제'),
  mm('cumulativeVarianceMM', '누계 차이', { cellClassRules: negative, flex: 1 })
]
</script>

<template>
  <div data-testid="plan-actual-table" :style="{ height }">
    <ag-grid-vue
      :class="GRID_THEME_CLASS"
      style="width: 100%; height: 100%"
      :row-data="series"
      :column-defs="columnDefs"
      :default-col-def="defaultColDef"
      :locale-text="localeText"
      :get-row-id="(p) => p.data.month"
    />
  </div>
</template>
