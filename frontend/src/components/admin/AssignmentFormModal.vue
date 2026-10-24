<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createAssignment, updateAssignment } from '@/api/admin'
import { PROJECT_ROLES } from '@/utils/constants'

/**
 * 인원 할당 등록/수정 폼 (투입/철수 일자 관리).
 * 클라이언트 검증: 계획 종료 ≥ 계획 시작, 실제 철수 ≥ 실제 투입(철수일 입력 시 투입일 필수),
 * 계획 기간 ⊆ 프로젝트 계획 기간. 위반 시 API를 호출하지 않는다.
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  project: { type: Object, required: true },
  assignment: { type: Object, default: null }, // null이면 생성
  users: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)
const serverErrors = reactive({})
const isEdit = computed(() => !!props.assignment)

const emptyForm = () => ({
  userId: null, roleInProject: '개발자', allocationRate: 1,
  plannedStart: props.project?.plannedStartDate || null,
  plannedEnd: props.project?.plannedEndDate || null,
  actualStart: null, actualEnd: null, note: ''
})
const form = reactive(emptyForm())

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

function checkPlannedStart(rule, value, callback) {
  if (value && props.project && value < props.project.plannedStartDate) {
    return callback(new Error(`프로젝트 계획 시작일(${props.project.plannedStartDate}) 이후여야 합니다.`))
  }
  callback()
}

function checkPlannedEnd(rule, value, callback) {
  if (value && form.plannedStart && value < form.plannedStart) {
    return callback(new Error('종료일은 시작일과 같거나 이후여야 합니다.'))
  }
  if (value && props.project && value > props.project.plannedEndDate) {
    return callback(new Error(`프로젝트 계획 종료일(${props.project.plannedEndDate}) 이전이어야 합니다.`))
  }
  callback()
}

function checkActualEnd(rule, value, callback) {
  if (value && !form.actualStart) return callback(new Error('철수일을 입력하려면 투입일이 필요합니다.'))
  if (value && form.actualStart && value < form.actualStart) {
    return callback(new Error('철수일은 투입일과 같거나 이후여야 합니다.'))
  }
  callback()
}

const rules = {
  userId: [{ required: true, message: '사용자를 선택하세요.', trigger: 'change' }],
  plannedStart: [
    { required: true, message: '계획 시작일을 입력하세요.', trigger: 'change' },
    { validator: checkPlannedStart, trigger: 'change' }
  ],
  plannedEnd: [
    { required: true, message: '계획 종료일을 입력하세요.', trigger: 'change' },
    { validator: checkPlannedEnd, trigger: 'change' }
  ],
  actualEnd: [{ validator: checkActualEnd, trigger: 'change' }]
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    for (const k of Object.keys(serverErrors)) delete serverErrors[k]
    Object.assign(form, emptyForm())
    if (props.assignment) {
      const a = props.assignment
      Object.assign(form, {
        userId: a.user.id,
        roleInProject: a.roleInProject,
        allocationRate: Number(a.allocationRate),
        plannedStart: a.plannedStart,
        plannedEnd: a.plannedEnd,
        actualStart: a.actualStart,
        actualEnd: a.actualEnd,
        note: a.note || ''
      })
    }
    formRef.value?.clearValidate()
  },
  { immediate: true }
)

async function submit() {
  for (const k of Object.keys(serverErrors)) delete serverErrors[k]
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload = {
    userId: form.userId,
    roleInProject: form.roleInProject || null,
    allocationRate: form.allocationRate,
    plannedStart: form.plannedStart,
    plannedEnd: form.plannedEnd,
    actualStart: form.actualStart || null,
    actualEnd: form.actualEnd || null,
    note: form.note || null
  }
  saving.value = true
  try {
    const saved = isEdit.value
      ? await updateAssignment(props.project.id, props.assignment.id, payload)
      : await createAssignment(props.project.id, payload)
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
  <el-dialog v-model="visible" :title="isEdit ? '할당 수정' : '인원 할당'" width="560px" append-to-body>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" data-testid="assignment-form">
      <el-form-item label="사용자" prop="userId" :error="serverErrors.userId">
        <el-select v-model="form.userId" filterable :disabled="isEdit" style="width: 100%" data-testid="assignment-user">
          <el-option v-for="u in users" :key="u.id" :label="`${u.name} (${u.username})`" :value="u.id" />
        </el-select>
      </el-form-item>
      <!-- 역할은 정해진 목록에서 선택 (기존 할당의 목록 밖 값은 그대로 표시되며 바꾸지 않으면 유지) -->
      <el-form-item label="역할" :error="serverErrors.roleInProject">
        <el-select v-model="form.roleInProject" placeholder="역할 선택" style="width: 100%" data-testid="assignment-role">
          <el-option v-for="r in PROJECT_ROLES" :key="r" :label="r" :value="r" />
        </el-select>
      </el-form-item>
      <el-form-item label="투입률" :error="serverErrors.allocationRate">
        <el-input-number v-model="form.allocationRate" :min="0.01" :max="1" :step="0.05" :precision="2" />
      </el-form-item>
      <el-form-item label="계획 시작" prop="plannedStart" :error="serverErrors.plannedStart">
        <el-date-picker v-model="form.plannedStart" type="date" value-format="YYYY-MM-DD" data-testid="assignment-planned-start" />
      </el-form-item>
      <el-form-item label="계획 종료" prop="plannedEnd" :error="serverErrors.plannedEnd">
        <el-date-picker v-model="form.plannedEnd" type="date" value-format="YYYY-MM-DD" data-testid="assignment-planned-end" />
      </el-form-item>
      <el-form-item label="실제 투입일" prop="actualStart" :error="serverErrors.actualStart">
        <el-date-picker v-model="form.actualStart" type="date" value-format="YYYY-MM-DD" clearable />
      </el-form-item>
      <el-form-item label="실제 철수일" prop="actualEnd" :error="serverErrors.actualEnd">
        <el-date-picker v-model="form.actualEnd" type="date" value-format="YYYY-MM-DD" clearable />
      </el-form-item>
      <el-form-item label="비고">
        <el-input v-model="form.note" maxlength="500" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">취소</el-button>
      <el-button type="primary" :loading="saving" data-testid="assignment-form-submit" @click="submit">저장</el-button>
    </template>
  </el-dialog>
</template>
