<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createProject, updateProject } from '@/api/admin'
import { PROJECT_STATUS, toOptions } from '@/utils/constants'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  project: { type: Object, default: null } // null이면 생성
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)
const serverErrors = reactive({})
const statusOptions = toOptions(PROJECT_STATUS)

const emptyForm = () => ({
  code: '', name: '', clientName: '', description: '', status: 'PLANNED',
  plannedStartDate: null, plannedEndDate: null, actualStartDate: null, actualEndDate: null
})
const form = reactive(emptyForm())
const isEdit = computed(() => !!props.project)

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

// 종료일 ≥ 시작일 검증 (서버 @DateRange와 동일 규칙)
function endAfter(startKey, requireStart) {
  return (rule, value, callback) => {
    const start = form[startKey]
    if (value && requireStart && !start) return callback(new Error('종료일을 입력하려면 시작일이 필요합니다.'))
    if (value && start && value < start) return callback(new Error('종료일은 시작일과 같거나 이후여야 합니다.'))
    callback()
  }
}

const rules = {
  code: [
    { required: true, message: '코드를 입력하세요.', trigger: 'blur' },
    { pattern: /^[A-Z0-9-]{2,30}$/, message: '영문 대문자/숫자/- 2~30자', trigger: 'blur' }
  ],
  name: [{ required: true, whitespace: true, message: '프로젝트명을 입력하세요.', trigger: 'blur' }],
  plannedStartDate: [{ required: true, message: '계획 시작일을 입력하세요.', trigger: 'change' }],
  plannedEndDate: [
    { required: true, message: '계획 종료일을 입력하세요.', trigger: 'change' },
    { validator: endAfter('plannedStartDate', false), trigger: 'change' }
  ],
  actualEndDate: [{ validator: endAfter('actualStartDate', true), trigger: 'change' }]
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    for (const k of Object.keys(serverErrors)) delete serverErrors[k]
    Object.assign(form, emptyForm(), props.project ? { ...props.project } : {})
    formRef.value?.clearValidate()
  },
  // 이미 열린 상태(modelValue=true)로 처음 마운트돼도 pre-fill 되도록 즉시 실행
  { immediate: true }
)

async function submit() {
  for (const k of Object.keys(serverErrors)) delete serverErrors[k]
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload = {
    code: form.code,
    name: form.name,
    clientName: form.clientName || null,
    description: form.description || null,
    status: form.status,
    plannedStartDate: form.plannedStartDate,
    plannedEndDate: form.plannedEndDate,
    actualStartDate: form.actualStartDate || null,
    actualEndDate: form.actualEndDate || null
  }
  saving.value = true
  try {
    const saved = isEdit.value ? await updateProject(props.project.id, payload) : await createProject(payload)
    ElMessage.success('저장되었습니다.')
    emit('saved', saved)
    visible.value = false
  } catch (e) {
    for (const fe of e.response?.data?.fieldErrors || []) serverErrors[fe.field] = fe.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" :title="isEdit ? '프로젝트 수정' : '프로젝트 등록'" width="600px" append-to-body>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" data-testid="project-form">
      <el-form-item label="코드" prop="code" :error="serverErrors.code">
        <el-input v-model="form.code" placeholder="SI-2026-003" />
      </el-form-item>
      <el-form-item label="프로젝트명" prop="name" :error="serverErrors.name">
        <el-input v-model="form.name" maxlength="200" />
      </el-form-item>
      <el-form-item label="고객사">
        <el-input v-model="form.clientName" maxlength="200" />
      </el-form-item>
      <el-form-item label="설명">
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="4000" />
      </el-form-item>
      <el-form-item label="상태">
        <el-select v-model="form.status" style="width: 160px">
          <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="계획 시작일" prop="plannedStartDate" :error="serverErrors.plannedStartDate">
        <el-date-picker v-model="form.plannedStartDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="계획 종료일" prop="plannedEndDate" :error="serverErrors.plannedEndDate">
        <el-date-picker v-model="form.plannedEndDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="실제 시작일" prop="actualStartDate" :error="serverErrors.actualStartDate">
        <el-date-picker v-model="form.actualStartDate" type="date" value-format="YYYY-MM-DD" clearable />
      </el-form-item>
      <el-form-item label="실제 종료일" prop="actualEndDate" :error="serverErrors.actualEndDate">
        <el-date-picker v-model="form.actualEndDate" type="date" value-format="YYYY-MM-DD" clearable />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">취소</el-button>
      <el-button type="primary" :loading="saving" data-testid="project-form-submit" @click="submit">저장</el-button>
    </template>
  </el-dialog>
</template>
