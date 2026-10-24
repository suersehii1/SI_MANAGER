<script setup>
import AppHeader from '@/components/common/AppHeader.vue'
import AppSidebar from '@/components/common/AppSidebar.vue'
</script>

<template>
  <el-container class="app-layout">
    <el-header class="app-layout__header" height="56px">
      <AppHeader />
    </el-header>
    <el-container>
      <el-aside width="200px" class="app-layout__aside">
        <AppSidebar />
      </el-aside>
      <el-main class="app-layout__main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-layout {
  height: 100%;
}
.app-layout__header {
  background: #1f2d3d;
  color: #fff;
  display: flex;
  align-items: center;
}
/* 색상은 Element Plus 변수 사용 → 낮/밤 스킨에 따라 자동으로 바뀐다 */
.app-layout__aside {
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
}
.app-layout__main {
  padding: 20px;
}
</style>
