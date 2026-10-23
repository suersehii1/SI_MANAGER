<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 할당 관리 화면은 "프로젝트" 탭에 속한 것으로 표시
const activeTab = computed(() => (route.name === 'admin-assignments' ? 'admin-projects' : route.name))

function onTabChange(name) {
  router.push({ name })
}
</script>

<template>
  <div class="admin-layout">
    <el-tabs :model-value="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="프로젝트" name="admin-projects" />
      <el-tab-pane label="사용자" name="admin-users" />
      <el-tab-pane label="월별 타임시트" name="admin-timesheet" />
      <el-tab-pane label="계획 vs 실제" name="admin-plan-vs-actual" />
    </el-tabs>
    <router-view />
  </div>
</template>
