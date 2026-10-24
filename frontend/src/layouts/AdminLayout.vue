<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 할당 관리 화면은 "프로젝트" 탭에 속한 것으로 표시
const activeTab = computed(() => (route.name === 'admin-assignments' ? 'admin-projects' : route.name))

function onTabChange(name) {
  router.push({ name })
}
</script>

<template>
  <div class="admin-layout">
    <div class="admin-layout__tabs">
      <el-tabs :model-value="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="프로젝트" name="admin-projects" />
        <el-tab-pane label="사용자" name="admin-users" />
        <el-tab-pane label="월별 타임시트" name="admin-timesheet" />
        <el-tab-pane label="계획 vs 실제" name="admin-plan-vs-actual" />
      </el-tabs>
      <!-- 일반 USER는 조회만 가능하다는 안내 (등록/수정/삭제 버튼은 각 화면에서 숨김) -->
      <el-tag v-if="!auth.isAdmin" type="info" effect="plain" class="admin-layout__readonly" data-testid="admin-readonly">
        조회 전용 (등록·수정·삭제는 ADMIN만 가능)
      </el-tag>
    </div>
    <!-- 탭 아래 남은 높이를 각 화면에 넘겨준다 (계획 vs 실제처럼 한 화면에 맞추는 화면은 이 높이를 채움) -->
    <div class="admin-layout__content">
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.admin-layout__tabs {
  position: relative;
}
/* 탭 줄 오른쪽 끝에 안내 표시 */
.admin-layout__readonly {
  position: absolute;
  top: 8px;
  right: 0;
}
.admin-layout__content {
  flex: 1;
  min-height: 0;
}
</style>
