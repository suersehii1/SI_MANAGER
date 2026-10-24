<script setup>
import { AgGridVue } from 'ag-grid-vue3'
import { defaultColDef, localeText, GRID_THEME_CLASS, actionsRenderer, clickedAction, escapeHtml } from '@/utils/grid'
import { formatDateTime } from '@/utils/format'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  // false(일반 USER)면 수정 작업 컬럼을 숨긴다
  canEdit: { type: Boolean, default: true },
  // 표 높이 (기본 480px, 부모 영역을 꽉 채우려면 '100%' → 행이 많으면 표 안에서 스크롤)
  height: { type: String, default: '480px' }
})
const emit = defineEmits(['edit'])

// 컬럼 순서: (아이디) 이름 · 영어이름 · 이메일 · 전화번호 · 직급 ...
// 아이디는 ADMIN(canEdit)에게만 보인다 (서버도 일반 USER에게는 아이디를 비워서 보냄)
const columnDefs = [
  { field: 'username', headerName: '아이디', width: 130, hide: !props.canEdit },
  { field: 'name', headerName: '이름', width: 140 },
  { field: 'englishName', headerName: '영어이름', width: 150 },
  { field: 'email', headerName: '이메일', flex: 1, minWidth: 180 },
  { field: 'phone', headerName: '전화번호', width: 140 },
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
    hide: !props.canEdit,
    cellRenderer: actionsRenderer([{ action: 'edit', label: '수정', type: 'primary', testid: 'user-edit' }])
  }
]

function onCellClicked(event) {
  if (clickedAction(event) === 'edit') emit('edit', event.data)
}
</script>

<template>
  <div data-testid="user-table" :style="{ height: props.height }">
    <ag-grid-vue
      :class="GRID_THEME_CLASS"
      style="width: 100%; height: 100%"
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
