<script setup>
import { AgGridVue } from 'ag-grid-vue3'
import { defaultColDef, localeText, GRID_THEME_CLASS, actionsRenderer, clickedAction, escapeHtml } from '@/utils/grid'
import { formatDateTime } from '@/utils/format'

defineProps({
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['edit'])

const columnDefs = [
  { field: 'username', headerName: '아이디', width: 130 },
  { field: 'name', headerName: '이름', width: 120 },
  { field: 'email', headerName: '이메일', flex: 1, minWidth: 160 },
  { field: 'jobTitle', headerName: '직급', width: 100 },
  {
    field: 'roles',
    headerName: '권한',
    width: 200,
    cellRenderer: (p) => (p.value || []).map((r) => `<span class="el-tag el-tag--small ${r === 'ROLE_ADMIN' ? 'el-tag--danger' : ''}">${escapeHtml(r)}</span>`).join(' ')
  },
  {
    field: 'enabled',
    headerName: '상태',
    width: 90,
    cellRenderer: (p) => (p.value ? '<span class="el-tag el-tag--small el-tag--success">활성</span>' : '<span class="el-tag el-tag--small el-tag--info">비활성</span>')
  },
  { field: 'createdAt', headerName: '생성일', width: 150, valueFormatter: (p) => formatDateTime(p.value) },
  {
    headerName: '작업',
    colId: 'actions',
    width: 90,
    pinned: 'right',
    cellRenderer: actionsRenderer([{ action: 'edit', label: '수정', type: 'primary', testid: 'user-edit' }])
  }
]

function onCellClicked(event) {
  if (clickedAction(event) === 'edit') emit('edit', event.data)
}
</script>

<template>
  <div data-testid="user-table">
    <ag-grid-vue
      :class="GRID_THEME_CLASS"
      style="width: 100%; height: 480px"
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
