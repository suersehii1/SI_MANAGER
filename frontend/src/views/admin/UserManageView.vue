<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listAdminUsers } from '@/api/admin'
import { useLookupStore } from '@/stores/lookup'
import { useAuthStore } from '@/stores/auth'
import UserTable from '@/components/admin/UserTable.vue'
import UserFormModal from '@/components/admin/UserFormModal.vue'

const lookup = useLookupStore()
// 일반 USER는 조회 전용: 등록/수정/삭제 버튼을 숨긴다 (서버도 403으로 막음)
const auth = useAuthStore()
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 0, size: 20, keyword: '', enabled: null })
const formVisible = ref(false)
const editing = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await listAdminUsers({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      enabled: query.enabled === null ? undefined : query.enabled
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

function openCreate() {
  editing.value = null
  formVisible.value = true
}

function onEdit(row) {
  editing.value = row
  formVisible.value = true
}

function onSaved() {
  lookup.loadUsers(true).catch(() => {})
  load()
}

onMounted(load)
</script>

<template>
  <div class="um">
    <div class="page-header">
      <h2 class="page-title">사용자 관리</h2>
      <el-button v-if="auth.isAdmin" type="primary" data-testid="new-user-btn" @click="openCreate"><el-icon><Plus /></el-icon>&nbsp;사용자 등록</el-button>
    </div>
    <div class="card um__card">
      <div class="toolbar">
        <el-input v-model="query.keyword" :placeholder="auth.isAdmin ? '아이디/이름/영어이름/이메일' : '이름/영어이름/이메일'" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.enabled" placeholder="상태" clearable style="width: 120px" @change="search">
          <el-option label="활성" :value="true" />
          <el-option label="비활성" :value="false" />
        </el-select>
        <el-button type="primary" @click="search">검색</el-button>
      </div>
      <UserTable class="um__table" height="100%" :rows="rows" :loading="loading" :can-edit="auth.isAdmin" @edit="onEdit" />
      <div class="pagination-bar">
        <el-pagination
          :current-page="query.page + 1"
          :page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="(p) => { query.page = p - 1; load() }"
        />
      </div>
    </div>
    <UserFormModal v-model="formVisible" :user="editing" @saved="onSaved" />
  </div>
</template>

<style scoped>
/* 화면 높이에 딱 맞춤 (Admin 탭 아래 남은 높이 = 100%): 제목·검색·페이지 이동은 고정, 표만 남은 높이를 채운다 */
.um {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 400px;
}
.um__card {
  flex: 1;
  min-height: 0;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
}
.um__table {
  flex: 1;
  min-height: 0;
}
</style>
