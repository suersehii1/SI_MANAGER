<script setup>
import AppHeader from '@/components/common/AppHeader.vue'
import AppSidebar from '@/components/common/AppSidebar.vue'
</script>

<template>
  <el-container class="app-layout">
    <el-header class="app-layout__header" height="56px">
      <AppHeader />
    </el-header>
    <el-container class="app-layout__body">
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
/* 헤더 아래 영역을 화면 높이에 고정: 내용이 길어도 사이드바(하단 스킨 선택 포함)는 그대로,
   본문(el-main)만 안에서 스크롤된다 */
.app-layout__body {
  min-height: 0;
  overflow: hidden;
}
.app-layout__main {
  padding: 20px;
  overflow: auto;
}
</style>
