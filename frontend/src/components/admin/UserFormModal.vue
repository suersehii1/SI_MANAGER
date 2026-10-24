<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createUser, updateUser } from '@/api/admin'
import { ROLES, JOB_TITLES } from '@/utils/constants'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  user: { type: Object, default: null } // null이면 생성
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)
const serverErrors = reactive({})
const isEdit = computed(() => !!props.user)

const emptyForm = () => ({ username: '', password: '', name: '', englishName: '', email: '', phone: '', jobTitle: '', roles: ['ROLE_USER'], enabled: true })
const form = reactive(emptyForm())

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

// 수정 시 비밀번호는 비워두면 변경하지 않음
function checkPassword(rule, value, callback) {
  if (!isEdit.value && !value) return callback(new Error('비밀번호를 입력하세요.'))
  if (value && (value.length < 8 || value.length > 64)) return callback(new Error('비밀번호는 8~64자입니다.'))
  callback()
}

const rules = {
  username: [
    { required: true, message: '아이디를 입력하세요.', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9._-]{3,50}$/, message: '영문/숫자/._- 3~50자', trigger: 'blur' }
  ],
  password: [{ validator: checkPassword, trigger: 'blur' }],
  name: [{ required: true, whitespace: true, message: '이름을 입력하세요.', trigger: 'blur' }],
  // 영어 이름·전화번호는 선택 입력: 입력했을 때만 형식 검사 (백엔드 UserFieldRules와 같은 규칙)
  englishName: [{ pattern: /^[A-Za-z][A-Za-z .'-]*$/, message: "영문자로 시작하고 영문자, 공백, . ' - 만 쓸 수 있습니다.", trigger: 'blur' }],
  email: [{ type: 'email', message: '이메일 형식이 아닙니다.', trigger: 'blur' }],
  phone: [{ pattern: /^0\d{1,2}-?\d{3,4}-?\d{4}$/, message: '전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)', trigger: 'blur' }],
  roles: [{ type: 'array', required: true, min: 1, message: '권한을 하나 이상 선택하세요.', trigger: 'change' }]
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    for (const k of Object.keys(serverErrors)) delete serverErrors[k]
    Object.assign(form, emptyForm())
    if (props.user) {
      Object.assign(form, {
        ...props.user,
        englishName: props.user.englishName || '',
        email: props.user.email || '',
        phone: props.user.phone || '',
        jobTitle: props.user.jobTitle || '',
        password: ''
      })
    }
    formRef.value?.clearValidate()
  },
  // 이미 열린 상태(modelValue=true)로 처음 마운트돼도 pre-fill 되도록 즉시 실행
  { immediate: true }
)

async function submit() {
  for (const k of Object.keys(serverErrors)) delete serverErrors[k]
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    let saved
    if (isEdit.value) {
      saved = await updateUser(props.user.id, {
        name: form.name, englishName: form.englishName || null, email: form.email || null, phone: form.phone || null,
        jobTitle: form.jobTitle || null, roles: form.roles, enabled: form.enabled, password: form.password || null
      })
    } else {
      saved = await createUser({
        username: form.username, password: form.password, name: form.name, englishName: form.englishName || null,
        email: form.email || null, phone: form.phone || null,
        jobTitle: form.jobTitle || null, roles: form.roles, enabled: form.enabled
      })
    }
    ElMessage.success('저장되었습니다.')
    emit('saved', saved)
    visible.value = false
  } catch (e) {
    for (const fe of e.response?.data?.fieldErrors || []) serverErrors[fe.field] = fe.message
    if (e.response?.status === 400 && !(e.response?.data?.fieldErrors || []).length) {
      ElMessage.error(e.response?.data?.message || '저장에 실패했습니다.')
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" :title="isEdit ? '사용자 수정' : '사용자 등록'" width="520px" append-to-body>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" data-testid="user-form">
      <el-form-item label="아이디" prop="username" :error="serverErrors.username">
        <el-input v-model="form.username" :disabled="isEdit" />
      </el-form-item>
      <el-form-item label="비밀번호" prop="password" :error="serverErrors.password">
        <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '변경 시에만 입력' : ''" />
      </el-form-item>
      <el-form-item label="이름" prop="name" :error="serverErrors.name">
        <el-input v-model="form.name" maxlength="100" />
      </el-form-item>
      <el-form-item label="영어 이름" prop="englishName" :error="serverErrors.englishName">
        <el-input v-model="form.englishName" maxlength="100" placeholder="예: Gil-dong Hong (선택)" data-testid="user-form-english-name" />
      </el-form-item>
      <el-form-item label="이메일" prop="email" :error="serverErrors.email">
        <el-input v-model="form.email" maxlength="200" />
      </el-form-item>
      <el-form-item label="전화번호" prop="phone" :error="serverErrors.phone">
        <el-input v-model="form.phone" maxlength="20" placeholder="예: 010-1234-5678 (선택)" data-testid="user-form-phone" />
      </el-form-item>
      <!-- 직급은 정해진 목록에서만 선택 (기존 사용자의 목록 밖 값은 그대로 표시되며 바꾸지 않으면 유지) -->
      <el-form-item label="직급" :error="serverErrors.jobTitle">
        <el-select v-model="form.jobTitle" placeholder="직급 선택" clearable style="width: 100%" data-testid="user-form-job-title">
          <el-option v-for="t in JOB_TITLES" :key="t" :label="t" :value="t" />
        </el-select>
      </el-form-item>
      <el-form-item label="권한" prop="roles" :error="serverErrors.roles">
        <el-checkbox-group v-model="form.roles">
          <el-checkbox v-for="r in ROLES" :key="r" :value="r">{{ r }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="활성">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">취소</el-button>
      <el-button type="primary" :loading="saving" @click="submit">저장</el-button>
    </template>
  </el-dialog>
</template>
