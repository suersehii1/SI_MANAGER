<script setup>
import { AgGridVue } from 'ag-grid-vue3'
import {
  defaultColDef, localeText, GRID_THEME_CLASS, assignmentStateRenderer, actionsRenderer, clickedAction
} from '@/utils/grid'
import { formatPeriod } from '@/utils/format'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  // false(일반 USER)면 수정/제외 작업 컬럼을 숨긴다
  canEdit: { type: Boolean, default: true }
})
const emit = defineEmits(['edit', 'delete'])

// 할당 목록은 서버가 계획 시작일·이름 순으로 전부 내려주므로 클라이언트 정렬만 사용
const columnDefs = [
  { headerName: '이름', valueGetter: (p) => p.data.user?.name, width: 120, sortable: true },
  { headerName: '아이디', valueGetter: (p) => p.data.user?.username, width: 110 },
  { field: 'roleInProject', headerName: '역할', width: 120, sortable: true },
  { field: 'allocationRate', headerName: '투입률', width: 90, type: 'numericColumn', valueFormatter: (p) => Number(p.value).toFixed(2) },
  { headerName: '계획 기간', width: 210, valueGetter: (p) => formatPeriod(p.data.plannedStart, p.data.plannedEnd) },
  {
    headerName: '실제 투입/철수',
    width: 210,
    valueGetter: (p) => (p.data.actualStart ? formatPeriod(p.data.actualStart, p.data.actualEnd) : '미투입')
  },
  { field: 'state', headerName: '상태', width: 100, cellRenderer: assignmentStateRenderer },
  { field: 'note', headerName: '비고', flex: 1, minWidth: 120 },
  {
    headerName: '작업',
    colId: 'actions',
    width: 130,
    pinned: 'right',
    hide: !props.canEdit,
    cellRenderer: actionsRenderer([
      { action: 'edit', label: '수정', type: 'primary', testid: 'assignment-edit' },
      { action: 'delete', label: '제외', type: 'danger', testid: 'assignment-delete' }
    ])
  }
]

function onCellClicked(event) {
  const action = clickedAction(event)
  if (action) emit(action, event.data)
}
</script>

<template>
  <div data-testid="assignment-table">
    <ag-grid-vue
      :class="GRID_THEME_CLASS"
      style="width: 100%; height: 400px"
      :row-data="rows"
      :column-defs="columnDefs"
      :default-col-def="defaultColDef"
      :locale-text="localeText"
      :loading="loading"
      :get-row-id="(p) => String(p.data.id)"
      @cell-clicked="onCellClicked"
    />
  </div>
</template>
