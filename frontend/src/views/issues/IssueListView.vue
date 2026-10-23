<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useIssueStore } from '@/stores/issue'
import { useLookupStore } from '@/stores/lookup'
import IssueFilterBar from '@/components/issues/IssueFilterBar.vue'
import IssueTable from '@/components/issues/IssueTable.vue'
import IssueFormModal from '@/components/issues/IssueFormModal.vue'
import IssueDetailDrawer from '@/components/issues/IssueDetailDrawer.vue'

const issueStore = useIssueStore()
const lookup = useLookupStore()
const route = useRoute()
const router = useRouter()

const formVisible = ref(false)
const formMode = ref('create')
const editingIssue = ref(null)
const drawerVisible = ref(false)
const drawerIssueId = ref(null)
const drawerRef = ref()

onMounted(async () => {
  lookup.loadProjects().catch(() => {})
  lookup.loadUsers().catch(() => {})
  await issueStore.fetchList()
})

// /issues/:id 로 진입하면 상세 Drawer 자동 오픈
watch(
  () => route.params.id,
  (id) => {
    if (id) {
      drawerIssueId.value = Number(id)
      drawerVisible.value = true
    } else {
      drawerVisible.value = false
    }
  },
  { immediate: true }
)

watch(drawerVisible, (open) => {
  if (!open && route.name === 'issue-detail') {
    router.push({ name: 'issues' })
  }
})

function onSearch(filters) {
  issueStore.setFilters(filters)
}

function onSortChange(sort) {
  issueStore.setSort(sort)
}

// el-pagination은 1-based, 스토어/API는 0-based
function onPageChange(page) {
  issueStore.setPage(page - 1)
}

function onSizeChange(size) {
  issueStore.setPage(0, size)
}

function openCreate() {
  formMode.value = 'create'
  editingIssue.value = null
  formVisible.value = true
}

async function openEdit(row) {
  // 그리드 행에는 description/attachments가 없으므로 상세를 조회해 pre-fill
  editingIssue.value = await issueStore.fetchOne(row.id)
  formMode.value = 'edit'
  formVisible.value = true
}

function openDetail(row) {
  router.push({ name: 'issue-detail', params: { id: row.id } })
}

// 저장 후 Grid 즉시 반영 + 열린 Drawer 갱신
async function onSaved() {
  // 신규 등록은 첫 페이지로 돌아가야 새 행(createdAt desc 기준 최상단)이 보인다
  if (formMode.value === 'create') {
    issueStore.page = 0
  }
  await issueStore.fetchList()
  if (drawerVisible.value) {
    drawerRef.value?.reload()
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`'${row.title}' 이슈를 삭제할까요? 첨부파일도 함께 삭제됩니다.`, '이슈 삭제', { type: 'warning' })
  } catch (e) {
    return
  }
  await issueStore.remove(row.id)
  ElMessage.success('삭제되었습니다.')
  drawerVisible.value = false
  await issueStore.fetchList()
}
</script>

<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">이슈 관리</h2>
      <el-button type="primary" data-testid="new-issue-btn" @click="openCreate">
        <el-icon><Plus /></el-icon>&nbsp;신규 이슈
      </el-button>
    </div>
    <div class="card">
      <IssueFilterBar
        :filters="issueStore.filters"
        :projects="lookup.projects"
        :users="lookup.users"
        @search="onSearch"
        @reset="onSearch"
      />
      <IssueTable
        :rows="issueStore.items"
        :sort="issueStore.sort"
        :loading="issueStore.loading"
        @sort-change="onSortChange"
        @row-click="openDetail"
        @edit="openEdit"
        @delete="onDelete"
      />
      <div class="pagination-bar">
        <el-pagination
          :current-page="issueStore.page + 1"
          :page-size="issueStore.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="issueStore.total"
          layout="total, sizes, prev, pager, next"
          background
          data-testid="issue-pagination"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <IssueFormModal
      v-model="formVisible"
      :mode="formMode"
      :issue="editingIssue"
      :default-project-id="issueStore.filters.projectId"
      @saved="onSaved"
    />
    <IssueDetailDrawer
      ref="drawerRef"
      v-model="drawerVisible"
      :issue-id="drawerIssueId"
      @edit="openEdit"
      @delete="onDelete"
      @changed="issueStore.fetchList()"
    />
  </div>
</template>
