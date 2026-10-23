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
.app-layout__aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
}
.app-layout__main {
  padding: 20px;
}
</style>
