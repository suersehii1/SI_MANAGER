<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { THEMES } from '@/utils/theme'

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

// 좌측 하단 스킨 선택 (낮 = 해, 밤 = 달). 선택 즉시 적용 + 내 사용자 정보에 저장
const theme = computed({
  get: () => auth.theme,
  set: (value) => {
    auth.setTheme(value).catch(() => ElMessage.error('스킨 저장에 실패했습니다.'))
  }
})
</script>

<template>
  <div class="app-sidebar">
    <el-menu :default-active="active" router class="app-sidebar__menu">
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
      <!-- Admin 콘솔은 모든 사용자에게 표시 (일반 USER는 조회 전용) -->
      <el-menu-item index="/admin" data-testid="menu-admin">
        <el-icon><Setting /></el-icon>
        <span>Admin 콘솔</span>
      </el-menu-item>
    </el-menu>

    <div class="app-sidebar__theme">
      <span class="app-sidebar__theme-label">스킨</span>
      <el-select v-model="theme" size="small" style="width: 110px" data-testid="theme-select">
        <template #prefix>
          <el-icon><component :is="THEMES[theme].icon" /></el-icon>
        </template>
        <el-option v-for="(t, key) in THEMES" :key="key" :label="t.label" :value="key">
          <span class="app-sidebar__theme-option">
            <el-icon><component :is="t.icon" /></el-icon>
            {{ t.label }}
          </span>
        </el-option>
      </el-select>
    </div>
  </div>
</template>

<style scoped>
/* 메뉴는 위, 스킨 선택은 사이드바 맨 아래에 붙인다 */
.app-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.app-sidebar__menu {
  flex: 1;
  border-right: none;
}
.app-sidebar__theme {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.app-sidebar__theme-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.app-sidebar__theme-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
