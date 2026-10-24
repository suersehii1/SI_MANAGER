<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminProject, listAssignments, deleteAssignment } from '@/api/admin'
import { useLookupStore } from '@/stores/lookup'
import { useAuthStore } from '@/stores/auth'
import { formatPeriod } from '@/utils/format'
import StatusTag from '@/components/common/StatusTag.vue'
import AssignmentTable from '@/components/admin/AssignmentTable.vue'
import AssignmentFormModal from '@/components/admin/AssignmentFormModal.vue'

const route = useRoute()
const router = useRouter()
const lookup = useLookupStore()
// 일반 USER는 조회 전용: 등록/수정/삭제 버튼을 숨긴다 (서버도 403으로 막음)
const auth = useAuthStore()

const projectId = computed(() => Number(route.params.id))
const project = ref(null)
const rows = ref([])
const loading = ref(false)
const formVisible = ref(false)
const editing = ref(null)

async function load() {
  loading.value = true
  try {
    const [p, list] = await Promise.all([getAdminProject(projectId.value), listAssignments(projectId.value)])
    project.value = p
    rows.value = list
  } finally {
    loading.value = false
  }
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
    await ElMessageBox.confirm(`${row.user.name} 님을 프로젝트에서 제외할까요?`, '리소스 제외', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteAssignment(projectId.value, row.id)
  ElMessage.success('제외되었습니다.')
  load()
}

onMounted(() => {
  lookup.loadUsers(true).catch(() => {})
  load()
})
</script>

<template>
  <div v-loading="loading" class="am">
    <div class="page-header">
      <div>
        <el-button link @click="router.push({ name: 'admin-projects' })">
          <el-icon><ArrowLeft /></el-icon>&nbsp;프로젝트 목록
        </el-button>
        <h2 class="page-title">인원 할당 관리</h2>
      </div>
      <el-button v-if="auth.isAdmin" type="primary" :disabled="!project" data-testid="new-assignment-btn" @click="openCreate">
        <el-icon><Plus /></el-icon>&nbsp;인원 추가
      </el-button>
    </div>
    <!-- ProjectInfoHeader -->
    <div v-if="project" class="card" data-testid="project-info-header">
      <el-descriptions :column="3" size="small">
        <el-descriptions-item label="프로젝트">{{ project.code }} {{ project.name }}</el-descriptions-item>
        <el-descriptions-item label="상태"><StatusTag kind="project" :value="project.status" /></el-descriptions-item>
        <el-descriptions-item label="고객사">{{ project.clientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="계획 기간">{{ formatPeriod(project.plannedStartDate, project.plannedEndDate) }}</el-descriptions-item>
        <el-descriptions-item label="실제 기간">
          {{ project.actualStartDate ? formatPeriod(project.actualStartDate, project.actualEndDate) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="할당 인원">{{ project.assignedHeadcount }}명</el-descriptions-item>
      </el-descriptions>
    </div>
    <div class="card am__card">
      <AssignmentTable class="am__table" height="100%" :rows="rows" :loading="loading" :can-edit="auth.isAdmin" @edit="onEdit" @delete="onDelete" />
    </div>
    <AssignmentFormModal
      v-if="project"
      v-model="formVisible"
      :project="project"
      :assignment="editing"
      :users="lookup.users"
      @saved="load"
    />
  </div>
</template>

<style scoped>
/* 화면 높이에 딱 맞춤 (Admin 탭 아래 남은 높이 = 100%): 제목·검색·페이지 이동은 고정, 표만 남은 높이를 채운다 */
.am {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 400px;
}
.am__card {
  flex: 1;
  min-height: 0;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
}
.am__table {
  flex: 1;
  min-height: 0;
}
</style>
