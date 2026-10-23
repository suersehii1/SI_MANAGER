<script setup>
import { AgGridVue } from 'ag-grid-vue3'
import { defaultColDef, localeText, projectStatusRenderer, GRID_THEME_CLASS } from '@/utils/grid'
import { formatMM, formatPeriod } from '@/utils/format'

defineProps({
  projects: { type: Array, default: () => [] }
})

// 대시보드 프로젝트 요약 (클라이언트 정렬)
const columnDefs = [
  { field: 'code', headerName: '코드', width: 130, pinned: 'left', sortable: true },
  { field: 'name', headerName: '프로젝트명', flex: 1, minWidth: 160, sortable: true },
  { field: 'status', headerName: '상태', width: 100, cellRenderer: projectStatusRenderer },
  {
    headerName: '계획 기간',
    width: 220,
    valueGetter: (p) => formatPeriod(p.data.plannedStartDate, p.data.plannedEndDate)
  },
  {
    headerName: '실제 기간',
    width: 220,
    valueGetter: (p) => (p.data.actualStartDate ? formatPeriod(p.data.actualStartDate, p.data.actualEndDate) : '-')
  },
  { field: 'plannedDurationDays', headerName: '계획 일수', width: 100, type: 'numericColumn' },
  { field: 'actualDurationDays', headerName: '실제 일수', width: 100, type: 'numericColumn', valueFormatter: (p) => p.value ?? '-' },
  { field: 'assignedHeadcount', headerName: '할당', width: 80, type: 'numericColumn', sortable: true },
  { field: 'activeHeadcount', headerName: '투입 중', width: 90, type: 'numericColumn', sortable: true },
  { field: 'totalPlannedMM', headerName: '계획 MM', width: 100, type: 'numericColumn', valueFormatter: (p) => formatMM(p.value) },
  { field: 'plannedMMToDate', headerName: '계획 MM(기준일)', width: 130, type: 'numericColumn', valueFormatter: (p) => formatMM(p.value) },
  { field: 'actualMMToDate', headerName: '실제 MM', width: 100, type: 'numericColumn', valueFormatter: (p) => formatMM(p.value) }
]
</script>

<template>
  <div data-testid="project-summary-table">
    <ag-grid-vue
      :class="GRID_THEME_CLASS"
      style="width: 100%; height: 320px"
      :row-data="projects"
      :column-defs="columnDefs"
      :default-col-def="defaultColDef"
      :locale-text="localeText"
      :get-row-id="(p) => String(p.data.id)"
    />
  </div>
</template>
