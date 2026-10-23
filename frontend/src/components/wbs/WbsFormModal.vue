<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useWbsStore } from '@/stores/wbs'
import { WBS_STATUS, toOptions } from '@/utils/constants'

/**
 * WBS 항목 등록/수정 모달.
 * - create: parent(null=최상위) 아래에 insertAfterId 뒤로 삽입
 * - edit: rolledUp(하위가 있는) 항목은 상태/기간/진척률이 집계값이므로 입력 비활성
 * - 클라이언트 검증: 이름 필수, 종료일 ≥ 시작일(위반 시 API 호출 안 함). 프로젝트 계획 기간 밖이면 경고만 표시.
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  item: { type: Object, default: null },
  parent: { type: Object, default: null },
  insertAfterId: { type: Number, default: null }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const store = useWbsStore()
const formRef = ref()
const saving = ref(false)
const serverErrors = reactive({})
const statusOptions = toOptions(WBS_STATUS)

const emptyForm = () => ({
  name: '', description: '', deliverable: '', assigneeId: null, status: 'NOT_STARTED', weight: 1,
  plannedStart: null, plannedEnd: null, actualStart: null, actualEnd: null, progress: 0
})
const form = reactive(emptyForm())

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
const rolledUp = computed(() => props.mode === 'edit' && !!props.item?.rolledUp)
const parentLabel = computed(() => {
  const parent = props.mode === 'edit'
    ? store.items.find((i) => i.id === props.item?.parentId)
    : props.parent
  return parent ? `${parent.wbsCode} ${parent.name}` : '(최상위)'
})

// 프로젝트 계획 기간 밖이면 경고 (서버는 리프에 대해 400 WBS_OUT_OF_PROJECT_PERIOD로 거부)
const periodWarning = computed(() => {
  const p = store.project
  if (!p || rolledUp.value) return ''
  const outside = (d) => d && (d < p.plannedStartDate || d > p.plannedEndDate)
  if (outside(form.plannedStart) || outside(form.plannedEnd)) {
    return `프로젝트 계획 기간(${p.plannedStartDate} ~ ${p.plannedEndDate})을 벗어났습니다.`
  }
  return ''
})

function endAfter(startKey, requireStart) {
  return (rule, value, callback) => {
    const start = form[startKey]
    if (value && requireStart && !start) return callback(new Error('종료일을 입력하려면 시작일이 필요합니다.'))
    if (value && start && value < start) return callback(new Error('종료일은 시작일과 같거나 이후여야 합니다.'))
    callback()
  }
}

const rules = {
  name: [
    { required: true, whitespace: true, message: '작업명을 입력하세요.', trigger: 'blur' },
    { max: 200, message: '200자 이하로 입력하세요.', trigger: 'blur' }
  ],
  plannedEnd: [{ validator: endAfter('plannedStart', false), trigger: 'change' }],
  actualEnd: [{ validator: endAfter('actualStart', true), trigger: 'change' }]
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    for (const k of Object.keys(serverErrors)) delete serverErrors[k]
    Object.assign(form, emptyForm())
    if (props.mode === 'edit' && props.item) {
      const i = props.item
      Object.assign(form, {
        name: i.name,
        description: i.description || '',
        deliverable: i.deliverable || '',
        assigneeId: i.assignee?.id ?? null,
        status: i.status,
        weight: Number(i.weight),
        plannedStart: i.plannedStart,
        plannedEnd: i.plannedEnd,
        actualStart: i.actualStart,
        actualEnd: i.actualEnd,
        progress: Math.round(Number(i.progress))
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
  const payload = {
    name: form.name.trim(),
    description: form.description || null,
    deliverable: form.deliverable || null,
    assigneeId: form.assigneeId || null,
    status: form.status,
    weight: form.weight,
    plannedStart: form.plannedStart || null,
    plannedEnd: form.plannedEnd || null,
    actualStart: form.actualStart || null,
    actualEnd: form.actualEnd || null,
    progress: form.progress
  }
  saving.value = true
  try {
    let saved
    if (props.mode === 'edit') {
      saved = await store.update(props.item.id, payload)
    } else {
      saved = await store.create({ ...payload, parentId: props.parent?.id ?? null, insertAfterId: props.insertAfterId })
    }
    ElMessage.success('저장되었습니다.')
    emit('saved', saved)
    visible.value = false
  } catch (e) {
    const fieldErrors = e.response?.data?.fieldErrors || []
    for (const fe of fieldErrors) serverErrors[fe.field] = fe.message
    if (e.response?.status === 400 && fieldErrors.length === 0) {
      ElMessage.error(e.response?.data?.message || '저장에 실패했습니다.')
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" :title="mode === 'edit' ? 'WBS 항목 수정' : 'WBS 항목 추가'" width="600px" append-to-body>
    <div data-testid="wbs-form-modal">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="상위 항목">
          <span>{{ parentLabel }}</span>
        </el-form-item>
        <el-form-item label="작업명" prop="name" :error="serverErrors.name">
          <el-input v-model="form.name" maxlength="200" data-testid="wbs-form-name" />
        </el-form-item>
        <el-form-item label="설명">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="4000" />
        </el-form-item>
        <el-form-item label="산출물">
          <el-input v-model="form.deliverable" maxlength="200" />
        </el-form-item>
        <el-form-item label="담당자" :error="serverErrors.assigneeId">
          <el-select v-model="form.assigneeId" clearable placeholder="미지정 (프로젝트 멤버)" style="width: 220px">
            <el-option v-for="m in store.members" :key="m.id" :label="`${m.name} (${m.username})`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="가중치" :error="serverErrors.weight">
          <el-input-number v-model="form.weight" :min="0.01" :max="999.99" :step="0.5" :precision="2" />
        </el-form-item>
        <el-alert v-if="rolledUp" type="info" :closable="false" show-icon title="하위 항목에서 자동 집계 — 상태/기간/진척률은 수정할 수 없습니다." style="margin-bottom: 12px" />
        <el-form-item label="상태">
          <el-select v-model="form.status" :disabled="rolledUp" style="width: 160px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="계획 시작" prop="plannedStart" :error="serverErrors.plannedStart">
          <el-date-picker v-model="form.plannedStart" type="date" value-format="YYYY-MM-DD" :disabled="rolledUp" clearable />
        </el-form-item>
        <el-form-item label="계획 종료" prop="plannedEnd" :error="serverErrors.plannedEnd">
          <el-date-picker v-model="form.plannedEnd" type="date" value-format="YYYY-MM-DD" :disabled="rolledUp" clearable />
        </el-form-item>
        <el-alert v-if="periodWarning" type="warning" :closable="false" show-icon :title="periodWarning" style="margin-bottom: 12px" />
        <el-form-item label="실제 시작" prop="actualStart" :error="serverErrors.actualStart">
          <el-date-picker v-model="form.actualStart" type="date" value-format="YYYY-MM-DD" :disabled="rolledUp" clearable />
        </el-form-item>
        <el-form-item label="실제 종료" prop="actualEnd" :error="serverErrors.actualEnd">
          <el-date-picker v-model="form.actualEnd" type="date" value-format="YYYY-MM-DD" :disabled="rolledUp" clearable />
        </el-form-item>
        <el-form-item label="진척률" :error="serverErrors.progress">
          <div style="display: flex; gap: 16px; width: 100%; align-items: center">
            <el-slider v-model="form.progress" :min="0" :max="100" :step="5" :disabled="rolledUp" style="flex: 1" />
            <el-input-number v-model="form.progress" :min="0" :max="100" size="small" :disabled="rolledUp" />
          </div>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="visible = false">취소</el-button>
      <el-button type="primary" :loading="saving" data-testid="wbs-form-submit" @click="submit">저장</el-button>
    </template>
  </el-dialog>
</template>
