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
      <!-- 회사 로고: 원본(174×84)보다 크게 늘리면 흐려지므로 원본 크기로 표시.
           assets/aigentum-logo.png 는 원본(흰 배경)의 배경만 투명하게 바꾼 파일 -->
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
/* 배경: login-background.jpg (남색·청록 톤이 로고/헤더 색과 맞아 색은 그대로 사용).
   이미지 안에 가짜 로그인 폼·작은 글씨가 있어 실제 로그인 카드와 겹쳐 보이지 않도록
   살짝 흐리게(blur) + 남색 그라데이션으로 눌러서 '분위기'만 남긴다. */
.login {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #0d1b2a; /* 이미지 로딩 전 바탕색 */
}
.login::before {
  content: '';
  position: absolute;
  inset: -24px; /* blur 때문에 가장자리가 비치지 않도록 조금 크게 */
  background: url('../assets/login-background.jpg') center / cover no-repeat;
  filter: blur(4px) brightness(0.8);
}
.login::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, rgba(13, 27, 42, 0.3) 0%, rgba(13, 27, 42, 0.7) 100%);
}
/* 로그인 카드는 배경 위에 떠 있는 느낌으로 */
.login__card {
  position: relative;
  z-index: 1;
  width: 360px;
  border: none;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.4);
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
