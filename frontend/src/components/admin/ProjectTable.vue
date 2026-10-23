<script setup>
import { AgGridVue } from 'ag-grid-vue3'
import {
  defaultColDef, localeText, GRID_THEME_CLASS, projectStatusRenderer, actionsRenderer, clickedAction
} from '@/utils/grid'
import { formatPeriod } from '@/utils/format'

/**
 * Admin 프로젝트 Grid. 서버 사이드 페이징/정렬은 IssueTable과 동일한 방식:
 * comparator=0으로 로컬 재정렬을 막고, sortChanged에서 'field,dir'를 부모에 알린다.
 */
const props = defineProps({
  rows: { type: Array, default: () => [] },
  sort: { type: String, default: 'plannedStartDate,desc' },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['sort-change', 'edit', 'delete', 'assignments', 'wbs'])

const keepServerOrder = () => 0
const [initialField, initialDir] = props.sort.split(',')
function sortable(colId) {
  return { colId, sortable: true, comparator: keepServerOrder, initialSort: initialField === colId ? initialDir : null }
}

const columnDefs = [
  { ...sortable('code'), field: 'code', headerName: '코드', width: 130, pinned: 'left' },
  { ...sortable('name'), field: 'name', headerName: '프로젝트명', flex: 1, minWidth: 180 },
  { field: 'clientName', headerName: '고객사', width: 120 },
  { ...sortable('status'), field: 'status', headerName: '상태', width: 100, cellRenderer: projectStatusRenderer },
  {
    ...sortable('plannedStartDate'),
    headerName: '계획 기간',
    width: 210,
    valueGetter: (p) => formatPeriod(p.data.plannedStartDate, p.data.plannedEndDate)
  },
  {
    ...sortable('actualStartDate'),
    headerName: '실제 기간',
    width: 210,
    valueGetter: (p) => (p.data.actualStartDate ? formatPeriod(p.data.actualStartDate, p.data.actualEndDate) : '-')
  },
  { field: 'assignedHeadcount', headerName: '인원', width: 80, type: 'numericColumn' },
  {
    headerName: '작업',
    colId: 'actions',
    width: 260,
    pinned: 'right',
    cellRenderer: actionsRenderer([
      { action: 'assignments', label: '할당 관리', type: 'success', testid: 'project-assignments' },
      { action: 'wbs', label: 'WBS', type: 'warning', testid: 'project-wbs' },
      { action: 'edit', label: '수정', type: 'primary', testid: 'project-edit' },
      { action: 'delete', label: '삭제', type: 'danger', testid: 'project-delete' }
    ])
  }
]

function onSortChanged(event) {
  const sorted = event.api.getColumnState().find((c) => c.sort)
  const next = sorted ? `${sorted.colId},${sorted.sort}` : 'plannedStartDate,desc'
  if (next !== props.sort) emit('sort-change', next)
}

function onCellClicked(event) {
  const action = clickedAction(event)
  if (action) emit(action, event.data)
}
</script>

<template>
  <div data-testid="project-table">
    <ag-grid-vue
      :class="GRID_THEME_CLASS"
      style="width: 100%; height: 480px"
      :row-data="rows"
      :column-defs="columnDefs"
      :default-col-def="defaultColDef"
      :locale-text="localeText"
      :loading="loading"
      :get-row-id="(p) => String(p.data.id)"
      @sort-changed="onSortChanged"
      @cell-clicked="onCellClicked"
    />
  </div>
</template>
