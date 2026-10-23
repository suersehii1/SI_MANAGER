<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useWbsStore } from '@/stores/wbs'
import { useLookupStore } from '@/stores/lookup'
import WbsToolbar from '@/components/wbs/WbsToolbar.vue'
import WbsGrid from '@/components/wbs/WbsGrid.vue'
import WbsGantt from '@/components/wbs/WbsGantt.vue'
import WbsFormModal from '@/components/wbs/WbsFormModal.vue'

const route = useRoute()
const router = useRouter()
const store = useWbsStore()
const lookup = useLookupStore()

const view = ref('grid')
const formVisible = ref(false)
const formMode = ref('create')
const formItem = ref(null)
const formParent = ref(null)
const formInsertAfter = ref(null)

// /projects/:projectId/wbs 면 해당 프로젝트, /wbs 면 선택 전 안내
const projectId = computed(() => (route.params.projectId ? Number(route.params.projectId) : null))

async function loadProject(id) {
  if (!id) {
    store.reset()
    return
  }
  store.selectedId = null
  store.collapsedIds = new Set()
  await Promise.all([store.fetch(id), store.fetchMembers(id)])
}

watch(projectId, (id) => loadProject(id).catch(() => {}), { immediate: true })

onMounted(() => {
  lookup.loadProjects().catch(() => {})
})

function onProjectChange(id) {
  router.replace({ name: 'project-wbs', params: { projectId: id } })
}

function openCreate(parent) {
  formMode.value = 'create'
  formItem.value = null
  formParent.value = parent
  formInsertAfter.value = null
  formVisible.value = true
}

function onAddRoot() {
  openCreate(null)
}

function onAddChild() {
  if (store.selectedItem) openCreate(store.selectedItem)
}

function openEdit(item) {
  const target = item || store.selectedItem
  if (!target) return
  formMode.value = 'edit'
  formItem.value = store.items.find((i) => i.id === target.id) || target
  formVisible.value = true
}

async function runMove(fn) {
  const id = store.selectedId
  if (id == null) return
  try {
    await fn(id)
  } catch (e) {
    // 공통 인터셉터가 메시지 표시 (400 WBS_INVALID_MOVE / WBS_DEPTH_EXCEEDED 등)
  }
}

async function onDelete() {
  const item = store.selectedItem
  if (!item) return
  // 하위 항목 수(전체 서브트리) 계산: DFS 순서에서 자신 뒤로 level이 더 큰 연속 항목
  const start = store.items.findIndex((i) => i.id === item.id)
  let descendants = 0
  for (let k = start + 1; k < store.items.length && store.items[k].level > item.level; k++) descendants++
  const extra = descendants > 0 ? ` 하위 항목 ${descendants}개도 함께 삭제됩니다.` : ''
  try {
    await ElMessageBox.confirm(`'${item.wbsCode} ${item.name}' 항목을 삭제할까요?${extra}`, 'WBS 삭제', { type: 'warning' })
  } catch (e) {
    return
  }
  await store.remove(item.id)
  ElMessage.success('삭제되었습니다.')
}
</script>

<template>
  <div v-loading="store.loading">
    <div class="page-header">
      <h2 class="page-title">WBS 관리</h2>
    </div>
    <div class="card">
      <WbsToolbar
        v-model:view="view"
        :projects="lookup.projects"
        :project-id="projectId"
        @project-change="onProjectChange"
        @add-root="onAddRoot"
        @add-child="onAddChild"
        @edit="openEdit()"
        @move-up="runMove(store.moveUp)"
        @move-down="runMove(store.moveDown)"
        @indent="runMove(store.indent)"
        @outdent="runMove(store.outdent)"
        @delete="onDelete"
        @expand-all="store.expandAll()"
        @collapse-all="store.collapseAll()"
      />
      <el-empty v-if="!projectId" description="프로젝트를 선택하세요." data-testid="wbs-empty" />
      <template v-else>
        <p class="text-muted">
          하위 항목이 있는 행(굵게)의 기간·진척률·상태는 하위 항목에서 자동 집계됩니다. 셀을 더블클릭해 바로 수정할 수 있습니다.
        </p>
        <WbsGrid v-if="view === 'grid'" @open-edit="openEdit" />
        <WbsGantt v-else />
      </template>
    </div>
    <WbsFormModal
      v-model="formVisible"
      :mode="formMode"
      :item="formItem"
      :parent="formParent"
      :insert-after-id="formInsertAfter"
    />
  </div>
</template>
