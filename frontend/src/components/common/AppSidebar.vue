<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()

// 현재 경로의 첫 세그먼트로 활성 메뉴 결정 (/projects/:id/wbs 는 WBS 메뉴)
const active = computed(() => {
  const p = route.path
  if (p.startsWith('/admin')) return '/admin'
  if (p.startsWith('/issues')) return '/issues'
  if (p.startsWith('/wbs') || /^\/projects\/\d+\/wbs/.test(p)) return '/wbs'
  return '/dashboard'
})
</script>

<template>
  <el-menu :default-active="active" router class="app-sidebar">
    <el-menu-item index="/dashboard">
      <el-icon><DataAnalysis /></el-icon>
      <span>대시보드</span>
    </el-menu-item>
    <el-menu-item index="/issues">
      <el-icon><Tickets /></el-icon>
      <span>이슈 관리</span>
    </el-menu-item>
    <el-menu-item index="/wbs" data-testid="menu-wbs">
      <el-icon><Operation /></el-icon>
      <span>WBS</span>
    </el-menu-item>
    <!-- Admin 메뉴는 ADMIN 사용자에게만 표시 (라우트 가드가 별도로 차단) -->
    <el-menu-item v-if="auth.isAdmin" index="/admin" data-testid="menu-admin">
      <el-icon><Setting /></el-icon>
      <span>Admin 콘솔</span>
    </el-menu-item>
  </el-menu>
</template>

<style scoped>
.app-sidebar {
  border-right: none;
}
</style>
