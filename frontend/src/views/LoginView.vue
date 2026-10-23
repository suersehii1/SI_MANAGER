<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

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
      <h2 class="login__title">SI Project Manager</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" data-testid="login-form" @submit.prevent="submit">
        <el-form-item label="아이디" prop="username">
          <el-input v-model="form.username" autocomplete="username" data-testid="login-username" />
        </el-form-item>
        <el-form-item label="비밀번호" prop="password">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" data-testid="login-password" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%" data-testid="login-submit">로그인</el-button>
      </el-form>
      <p class="text-muted login__hint">시드 계정: admin / admin1234, user / user1234</p>
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
.login__title {
  margin-top: 0;
  text-align: center;
}
.login__hint {
  margin-bottom: 0;
  text-align: center;
}
</style>
