<script setup>
import { AgGridVue } from 'ag-grid-vue3'
import {
  defaultColDef, localeText, GRID_THEME_CLASS, issueStatusRenderer, issuePriorityRenderer,
  progressRenderer, actionsRenderer, clickedAction
} from '@/utils/grid'
import { formatDate, formatDateTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

/**
 * 이슈 메인 Grid (AG Grid Community, clientSide row model).
 * 서버 사이드 정렬/페이징 연결 방식:
 * - rowData는 현재 페이지의 행만 담는다(서버가 이미 정렬해서 준 순서).
 * - 정렬 가능한 컬럼의 colId는 API sort 필드명(id, title, status, priority, progress, dueDate, createdAt, updatedAt)과 같다.
 * - comparator가 항상 0을 돌려주므로 AG Grid가 로컬에서 행 순서를 바꾸지 않는다(정렬 아이콘만 표시).
 * - sortChanged 이벤트에서 'field,dir' 문자열을 만들어 부모에 알리고, 부모가 API를 다시 호출한다.
 */
const props = defineProps({
  rows: { type: Array, default: () => [] },
  sort: { type: String, default: 'createdAt,desc' },
  loading: { type: Boolean, default: false },
  // 표 높이 (기본 520px, 부모 영역을 꽉 채우려면 '100%' → 행이 많으면 표 안에서 스크롤)
  height: { type: String, default: '520px' }
})
const emit = defineEmits(['sort-change', 'row-click', 'edit', 'delete'])

const auth = useAuthStore()
const keepServerOrder = () => 0

function sortable(colId) {
  const [field, dir] = props.sort.split(',')
  return {
    colId,
    sortable: true,
    comparator: keepServerOrder,
    initialSort: field === colId ? dir : null
  }
}

// 삭제 버튼: reporter 본인 또는 ADMIN
function canDelete(row) {
  return auth.isAdmin || row?.reporter?.id === auth.user?.id
}

// 컬럼 정의는 최초 1회만 만든다 (initialSort는 첫 렌더링에만 적용)
const columnDefs = [
  { ...sortable('id'), field: 'id', headerName: 'ID', width: 80 },
  { field: 'projectCode', headerName: '프로젝트', width: 130 },
  { ...sortable('title'), field: 'title', headerName: '제목', flex: 1, minWidth: 200 },
  { ...sortable('status'), field: 'status', headerName: '상태', width: 110, cellRenderer: issueStatusRenderer },
  { ...sortable('priority'), field: 'priority', headerName: '우선순위', width: 110, cellRenderer: issuePriorityRenderer },
  { ...sortable('progress'), field: 'progress', headerName: '진척률', width: 140, cellRenderer: progressRenderer },
  { headerName: '담당자', width: 110, valueGetter: (p) => p.data.assignee?.name || '-' },
  { ...sortable('dueDate'), field: 'dueDate', headerName: '마감일', width: 120, valueFormatter: (p) => formatDate(p.value) },
  { field: 'attachmentCount', headerName: '첨부', width: 70, type: 'numericColumn' },
  { ...sortable('createdAt'), field: 'createdAt', headerName: '등록일', width: 150, valueFormatter: (p) => formatDateTime(p.value) },
  {
    headerName: '작업',
    colId: 'actions',
    width: 130,
    pinned: 'right',
    cellRenderer: actionsRenderer([
      { action: 'edit', label: '수정', type: 'primary', testid: 'issue-edit' },
      { action: 'delete', label: '삭제', type: 'danger', testid: 'issue-delete', visible: canDelete }
    ])
  }
]

function onSortChanged(event) {
  const sorted = event.api.getColumnState().find((c) => c.sort)
  const next = sorted ? `${sorted.colId},${sorted.sort}` : 'createdAt,desc'
  if (next !== props.sort) {
    emit('sort-change', next)
  }
}

// 액션 버튼 클릭 → edit/delete, 그 외 영역 클릭 → 상세 Drawer
function onCellClicked(event) {
  const action = clickedAction(event)
  if (action === 'edit') emit('edit', event.data)
  else if (action === 'delete') emit('delete', event.data)
}

function onRowClicked(event) {
  if (clickedAction(event)) return
  emit('row-click', event.data)
}
</script>

<template>
  <div data-testid="issue-table" :style="{ height: props.height }">
    <ag-grid-vue
      :class="GRID_THEME_CLASS"
      style="width: 100%; height: 100%"
      :row-data="rows"
      :column-defs="columnDefs"
      :default-col-def="defaultColDef"
      :locale-text="localeText"
      :loading="loading"
      :get-row-id="(p) => String(p.data.id)"
      :row-style="{ cursor: 'pointer' }"
      @sort-changed="onSortChanged"
      @cell-clicked="onCellClicked"
      @row-clicked="onRowClicked"
    />
  </div>
</template>
