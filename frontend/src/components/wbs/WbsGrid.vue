<script setup>
import { computed } from 'vue'
import { AgGridVue } from 'ag-grid-vue3'
import { ElMessage } from 'element-plus'
import { useWbsStore } from '@/stores/wbs'
import { defaultColDef, localeText, GRID_THEME_CLASS, wbsStatusRenderer, progressRenderer, clickedAction } from '@/utils/grid'
import { WBS_STATUS } from '@/utils/constants'
import { wbsNameCellRenderer, delayedRenderer } from './wbsCellRenderers'

/**
 * WBS Grid (AG Grid Community, clientSide).
 * - rowData = store.visibleRows (접힌 노드의 하위 제외)를 복사한 평탄 목록. 트리는 level 들여쓰기로 표현.
 * - 인라인 편집(canEdit일 때): 이름/산출물/가중치/담당자는 모든 행, 상태/기간/진척률은 리프(rolledUp=false)만.
 * - cellValueChanged → 행 전체로 PUT 요청 → 성공/실패 모두 트리 재조회(실패 시 값이 원복됨) + 서버 메시지 표시.
 */
const emit = defineEmits(['open-edit'])
const store = useWbsStore()

// AG Grid가 편집 시 data 객체를 직접 수정하므로 스토어 객체를 복사해서 넘긴다.
// collapsed/selected를 행 데이터에 넣어 두면 값이 바뀔 때 AG Grid가 해당 행을 다시 그린다.
const rowData = computed(() =>
  store.visibleRows.map((item) => ({
    ...item,
    assigneeId: item.assignee?.id ?? '',
    collapsed: store.collapsedIds.has(item.id),
    selected: item.id === store.selectedId
  }))
)

// 담당자 select: '' = 미지정
const memberRefData = computed(() => {
  const map = { '': '미지정' }
  for (const m of store.members) map[m.id] = m.name
  return map
})

const statusRefData = Object.fromEntries(Object.entries(WBS_STATUS).map(([k, v]) => [k, v.label]))

const editableAll = () => store.canEdit
const editableLeaf = (p) => store.canEdit && !p.data.rolledUp

const columnDefs = computed(() => [
  { field: 'wbsCode', headerName: 'WBS', width: 90, pinned: 'left' },
  {
    field: 'name',
    headerName: '작업명',
    width: 280,
    pinned: 'left',
    editable: editableAll,
    cellEditor: 'agTextCellEditor',
    cellEditorParams: { maxLength: 200 },
    cellRenderer: wbsNameCellRenderer
  },
  { field: 'deliverable', headerName: '산출물', width: 160, editable: editableAll, cellEditorParams: { maxLength: 200 } },
  {
    field: 'assigneeId',
    headerName: '담당자',
    width: 110,
    editable: editableAll,
    cellEditor: 'agSelectCellEditor',
    cellEditorParams: { values: ['', ...store.members.map((m) => m.id)] },
    refData: memberRefData.value
  },
  {
    field: 'status',
    headerName: '상태',
    width: 110,
    editable: editableLeaf,
    cellEditor: 'agSelectCellEditor',
    cellEditorParams: { values: Object.keys(WBS_STATUS) },
    refData: statusRefData,
    cellRenderer: wbsStatusRenderer
  },
  {
    field: 'weight',
    headerName: '가중치',
    width: 90,
    type: 'numericColumn',
    editable: editableAll,
    cellEditor: 'agNumberCellEditor',
    cellEditorParams: { min: 0.01, max: 999.99, precision: 2 },
    valueFormatter: (p) => Number(p.value).toFixed(2)
  },
  { field: 'plannedStart', headerName: '계획 시작', width: 120, editable: editableLeaf, cellEditor: 'agDateStringCellEditor' },
  { field: 'plannedEnd', headerName: '계획 종료', width: 120, editable: editableLeaf, cellEditor: 'agDateStringCellEditor' },
  { field: 'actualStart', headerName: '실제 시작', width: 120, editable: editableLeaf, cellEditor: 'agDateStringCellEditor' },
  { field: 'actualEnd', headerName: '실제 종료', width: 120, editable: editableLeaf, cellEditor: 'agDateStringCellEditor' },
  {
    field: 'progress',
    headerName: '진척률',
    width: 140,
    editable: editableLeaf,
    cellEditor: 'agNumberCellEditor',
    cellEditorParams: { min: 0, max: 100, precision: 0 },
    cellRenderer: progressRenderer
  },
  { field: 'plannedDays', headerName: '일수', width: 80, type: 'numericColumn' },
  { field: 'delayed', headerName: '지연', width: 90, cellRenderer: delayedRenderer }
])

// 행 전체로 수정 payload 구성 (서버는 부모 노드의 기간/진척/상태 값을 무시한다)
function toPayload(row) {
  const blank = (v) => (v === '' || v === undefined ? null : v)
  return {
    name: row.name,
    description: row.description,
    deliverable: blank(row.deliverable),
    assigneeId: blank(row.assigneeId) === null ? null : Number(row.assigneeId),
    status: row.status,
    weight: Number(row.weight),
    plannedStart: blank(row.plannedStart),
    plannedEnd: blank(row.plannedEnd),
    actualStart: blank(row.actualStart),
    actualEnd: blank(row.actualEnd),
    progress: Math.round(Number(row.progress) || 0)
  }
}

async function onCellValueChanged(event) {
  if (event.oldValue === event.newValue) return
  try {
    await store.update(event.data.id, toPayload(event.data))
  } catch (e) {
    // 실패: 트리를 다시 읽어 값을 원복하고 서버 메시지 표시
    const data = e.response?.data
    const msg = data?.fieldErrors?.[0]?.message || data?.message
    if (data?.fieldErrors?.length) ElMessage.error(msg)
    await store.fetch(store.projectId)
  }
}

function onCellClicked(event) {
  if (clickedAction(event) === 'toggle') {
    store.toggle(event.data.id)
    return
  }
  store.selectedId = event.data.id
}

// 더블클릭: 편집 불가 셀(코드, 집계 컬럼 등)에서만 상세 편집 모달 (편집 가능 셀은 인라인 편집이 시작됨)
function onCellDoubleClicked(event) {
  const editable = event.colDef.editable
  const isEditable = typeof editable === 'function' ? editable(event) : !!editable
  if (!isEditable) emit('open-edit', event.data)
}

// 집계 행은 굵게+연한 배경, 선택 행은 파란 배경 (rowClassRules는 데이터 변경 시 재평가됨)
const rowClassRules = {
  'row-rolled-up': (p) => p.data.rolledUp,
  'wbs-row-selected': (p) => p.data.selected
}
</script>

<template>
  <div data-testid="wbs-grid">
    <ag-grid-vue
      :class="GRID_THEME_CLASS"
      style="width: 100%; height: 560px"
      :row-data="rowData"
      :column-defs="columnDefs"
      :default-col-def="defaultColDef"
      :locale-text="localeText"
      :get-row-id="(p) => String(p.data.id)"
      :row-class-rules="rowClassRules"
      :stop-editing-when-cells-lose-focus="true"
      @cell-value-changed="onCellValueChanged"
      @cell-clicked="onCellClicked"
      @cell-double-clicked="onCellDoubleClicked"
    />
  </div>
</template>

<style>
.wbs-toggle {
  display: inline-block;
  width: 16px;
  cursor: pointer;
  color: #606266;
  font-size: 10px;
}
.wbs-delayed {
  color: #f56c6c;
  font-weight: 600;
}
.ag-theme-quartz .wbs-row-selected {
  background: #ecf5ff !important;
}
</style>
