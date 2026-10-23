<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="app-header">
    <router-link to="/dashboard" class="app-header__brand">SI Project Manager</router-link>
    <div class="app-header__user">
      <span data-testid="header-username">{{ auth.user?.name }} ({{ auth.user?.username }})</span>
      <el-tag v-if="auth.isAdmin" size="small" type="danger" effect="dark">ADMIN</el-tag>
      <el-button size="small" @click="logout">로그아웃</el-button>
    </div>
  </div>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.app-header__brand {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  text-decoration: none;
}
.app-header__user {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
