<script setup>
import { computed } from 'vue'
import { AgGridVue } from 'ag-grid-vue3'
import { defaultColDef, localeText, GRID_THEME_CLASS } from '@/utils/grid'
import { formatMM } from '@/utils/format'

/**
 * 월별 타임시트 Grid. AG Grid Community에는 tree/grouping이 없으므로
 * "사용자 행(굵게) → 그 사용자의 프로젝트 행(들여쓰기)" 순서로 평탄화해 표시한다.
 * 월 총합은 pinnedBottomRowData(하단 고정 행)로 표시. 사용자 월합이 1.00 초과(과다 할당)면 빨간색.
 */
const props = defineProps({
  data: { type: Object, default: null }
})

const rowData = computed(() => {
  if (!props.data) return []
  const rows = []
  for (const u of props.data.rows) {
    const over = new Set(u.overAllocatedMonths)
    const userRow = { id: `u-${u.userId}`, kind: 'user', label: `${u.name} (${u.username})`, total: u.total, over }
    for (const c of u.cells) userRow[c.month] = c.mm
    rows.push(userRow)
    for (const p of u.projects) {
      const projectRow = {
        id: `a-${p.assignmentId}`,
        kind: 'project',
        label: `${p.projectCode} ${p.projectName}`,
        role: `${p.roleInProject} · ${Number(p.allocationRate).toFixed(2)}`,
        total: p.total
      }
      for (const c of p.cells) projectRow[c.month] = c.mm
      rows.push(projectRow)
    }
  }
  return rows
})

const pinnedBottom = computed(() => {
  if (!props.data) return []
  const row = { id: 'total', kind: 'total', label: '월 총합', total: props.data.grandTotal }
  for (const c of props.data.monthTotals) row[c.month] = c.mm
  return [row]
})

const columnDefs = computed(() => {
  const months = props.data?.months || []
  return [
    {
      headerName: '사용자 / 프로젝트',
      field: 'label',
      pinned: 'left',
      width: 260,
      cellStyle: (p) => (p.data.kind === 'project' ? { paddingLeft: '32px' } : null)
    },
    { headerName: '역할·투입률', field: 'role', width: 150 },
    ...months.map((m) => ({
      headerName: m,
      field: m,
      width: 95,
      type: 'numericColumn',
      valueFormatter: (p) => formatMM(p.value),
      // 사용자 행에서 과다 할당 월이면 빨간색
      cellClassRules: { 'mm-over': (p) => p.data.kind === 'user' && p.data.over?.has(m) }
    })),
    { headerName: '합계', field: 'total', width: 100, pinned: 'right', type: 'numericColumn', valueFormatter: (p) => formatMM(p.value) }
  ]
})

function rowClass(params) {
  return params.data.kind === 'project' ? '' : 'row-group'
}
</script>

<template>
  <div data-testid="timesheet-grid">
    <ag-grid-vue
      :class="GRID_THEME_CLASS"
      style="width: 100%; height: 520px"
      :row-data="rowData"
      :pinned-bottom-row-data="pinnedBottom"
      :column-defs="columnDefs"
      :default-col-def="defaultColDef"
      :locale-text="localeText"
      :get-row-id="(p) => p.data.id"
      :get-row-class="rowClass"
    />
  </div>
</template>
