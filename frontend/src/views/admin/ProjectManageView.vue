<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdminProjects, deleteProject } from '@/api/admin'
import { useLookupStore } from '@/stores/lookup'
import { useAuthStore } from '@/stores/auth'
import { PROJECT_STATUS, toOptions } from '@/utils/constants'
import ProjectTable from '@/components/admin/ProjectTable.vue'
import ProjectFormModal from '@/components/admin/ProjectFormModal.vue'

const router = useRouter()
const lookup = useLookupStore()
// 일반 USER는 조회 전용: 등록/수정/삭제 버튼을 숨긴다 (서버도 403으로 막음)
const auth = useAuthStore()

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 0, size: 20, sort: 'plannedStartDate,desc', keyword: '', status: null })
const statusOptions = toOptions(PROJECT_STATUS)

const formVisible = ref(false)
const editing = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await listAdminProjects({
      page: query.page,
      size: query.size,
      sort: query.sort,
      keyword: query.keyword || undefined,
      status: query.status || undefined
    })
    rows.value = res.content
    total.value = res.totalElements
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 0
  load()
}

function onSortChange(sort) {
  query.sort = sort
  query.page = 0
  load()
}

function openCreate() {
  editing.value = null
  formVisible.value = true
}

function onEdit(row) {
  editing.value = row
  formVisible.value = true
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `'${row.name}' 프로젝트를 삭제할까요? 할당, 이슈, 첨부파일, WBS가 모두 삭제됩니다.`,
      '프로젝트 삭제',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    // 목록에서 불러온 버전을 함께 보낸다 → 그 사이 다른 사람이 수정했으면 409 (안내 메시지는 공통 처리기가 표시)
    await deleteProject(row.id, row.version)
  } catch (e) {
    if (e.response?.status === 409) load() // 최신 내용으로 다시 조회
    return
  }
  ElMessage.success('삭제되었습니다.')
  lookup.loadProjects(true).catch(() => {})
  load()
}

function onSaved() {
  lookup.loadProjects(true).catch(() => {})
  load()
}

onMounted(load)
</script>

<template>
  <div class="pm">
    <div class="page-header">
      <h2 class="page-title">프로젝트 관리</h2>
      <el-button v-if="auth.isAdmin" type="primary" data-testid="new-project-btn" @click="openCreate">
        <el-icon><Plus /></el-icon>&nbsp;프로젝트 등록
      </el-button>
    </div>
    <div class="card pm__card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="코드/이름/고객사" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.status" placeholder="상태" clearable style="width: 130px" @change="search">
          <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" @click="search">검색</el-button>
      </div>
      <ProjectTable
        class="pm__table"
        height="100%"
        :rows="rows"
        :sort="query.sort"
        :loading="loading"
        :can-edit="auth.isAdmin"
        @sort-change="onSortChange"
        @edit="onEdit"
        @delete="onDelete"
        @assignments="(row) => router.push({ name: 'admin-assignments', params: { id: row.id } })"
        @wbs="(row) => router.push({ name: 'project-wbs', params: { projectId: row.id } })"
      />
      <div class="pagination-bar">
        <el-pagination
          :current-page="query.page + 1"
          :page-size="query.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="(p) => { query.page = p - 1; load() }"
          @size-change="(s) => { query.size = s; query.page = 0; load() }"
        />
      </div>
    </div>
    <ProjectFormModal v-model="formVisible" :project="editing" @saved="onSaved" />
  </div>
</template>

<style scoped>
/* 화면 높이에 딱 맞춤 (Admin 탭 아래 남은 높이 = 100%): 제목·검색·페이지 이동은 고정, 표만 남은 높이를 채운다 */
.pm {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 400px;
}
.pm__card {
  flex: 1;
  min-height: 0;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
}
.pm__table {
  flex: 1;
  min-height: 0;
}
</style>
