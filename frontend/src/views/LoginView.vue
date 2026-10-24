<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import aigentumLogo from '@/assets/aigentum-logo.png'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const formRef = ref()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const rules = {
  username: [{ required: true, message: '아이디를 입력하세요.', trigger: 'blur' }],
  password: [{ required: true, message: '비밀번호를 입력하세요.', trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.push(redirect)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '로그인에 실패했습니다.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <el-card class="login__card">
      <!-- 회사 로고: 원본(174×84)보다 크게 늘리면 흐려지므로 원본 크기로 표시 -->
      <div class="login__brand">
        <img :src="aigentumLogo" alt="(주)에이젠텀 Aigentum" class="login__logo" width="174" height="84" data-testid="login-logo" />
      </div>
      <h2 class="login__title">전사 Project 관리 시스템</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" data-testid="login-form" @submit.prevent="submit">
        <el-form-item label="아이디" prop="username">
          <el-input v-model="form.username" autocomplete="username" data-testid="login-username" />
        </el-form-item>
        <el-form-item label="비밀번호" prop="password">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" data-testid="login-password" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%" data-testid="login-submit">로그인</el-button>
      </el-form>
      <p class="text-muted login__hint">관리자 계정: admin / admin1234</p>
    </el-card>
  </div>
</template>

<style scoped>
.login {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.login__card {
  width: 360px;
}
/* 로고 아래 얇은 구분선 → 그 아래 시스템 이름 */
.login__brand {
  display: flex;
  justify-content: center;
  padding-bottom: 12px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.login__logo {
  display: block;
  max-width: 100%;
  height: auto;
  /* 로고 이미지 배경이 순백이 아닌 미색(254,253)이라 카드 위에 옅은 사각형이 보인다.
     multiply로 섞으면 흰 배경은 카드 색에 흡수되고 남색 로고는 그대로 보인다 */
  mix-blend-mode: multiply;
}
.login__title {
  margin: 0 0 16px;
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.login__hint {
  margin-bottom: 0;
  text-align: center;
}
</style>
