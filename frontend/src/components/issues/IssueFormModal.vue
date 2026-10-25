<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useIssueStore } from '@/stores/issue'
import { useLookupStore } from '@/stores/lookup'
import { uploadAttachment } from '@/api/attachments'
import { ISSUE_STATUS, ISSUE_PRIORITY, toOptions } from '@/utils/constants'
import AttachmentUploader from '@/components/attachments/AttachmentUploader.vue'
import AttachmentList from '@/components/attachments/AttachmentList.vue'

/**
 * 이슈 등록/수정 모달.
 * - create: 저장 → 대기열 파일 순차 업로드 → saved emit → 닫힘 (부모가 목록 재조회)
 * - edit: issue로 pre-fill, 저장 → saved emit. 첨부는 즉시 업로드 모드.
 * - 서버 400 fieldErrors는 해당 폼 항목 아래에 표시.
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' }, // 'create' | 'edit'
  issue: { type: Object, default: null },
  defaultProjectId: { type: Number, default: null }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const issueStore = useIssueStore()
const lookup = useLookupStore()

const formRef = ref()
const saving = ref(false)
const queuedFiles = ref([])
const attachments = ref([])
const serverErrors = reactive({})

const emptyForm = () => ({
  projectId: props.defaultProjectId,
  title: '',
  description: '',
  status: 'OPEN',
  priority: 'MEDIUM',
  progress: 0,
  assigneeId: null,
  dueDate: null
})
const form = reactive(emptyForm())

const rules = {
  projectId: [{ required: true, message: '프로젝트를 선택하세요.', trigger: 'change' }],
  title: [
    { required: true, whitespace: true, message: '제목을 입력하세요.', trigger: 'blur' },
    { max: 200, message: '제목은 200자 이하입니다.', trigger: 'blur' }
  ]
}

const statusOptions = toOptions(ISSUE_STATUS)
const priorityOptions = toOptions(ISSUE_PRIORITY)
const title = computed(() => (props.mode === 'edit' ? '이슈 수정' : '신규 이슈'))

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

// 열릴 때마다 폼 초기화 (edit 모드는 issue 값으로 채움)
watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    clearServerErrors()
    queuedFiles.value = []
    if (props.mode === 'edit' && props.issue) {
      Object.assign(form, {
        projectId: props.issue.projectId,
        title: props.issue.title,
        description: props.issue.description || '',
        status: props.issue.status,
        priority: props.issue.priority,
        progress: props.issue.progress,
        assigneeId: props.issue.assignee?.id ?? null,
        dueDate: props.issue.dueDate
      })
      attachments.value = props.issue.attachments || []
    } else {
      Object.assign(form, emptyForm())
      attachments.value = []
    }
    lookup.loadProjects().catch(() => {})
    lookup.loadUsers().catch(() => {})
    formRef.value?.clearValidate()
  },
  { immediate: true }
)

function clearServerErrors() {
  for (const k of Object.keys(serverErrors)) delete serverErrors[k]
}

function payload() {
  return {
    projectId: form.projectId,
    title: form.title.trim(),
    description: form.description || null,
    status: form.status,
    priority: form.priority,
    progress: form.progress,
    assigneeId: form.assigneeId || null,
    dueDate: form.dueDate || null,
    // 수정이면 불러온 버전을 같이 보낸다 → 그 사이 다른 사람이 저장했으면 서버가 409로 막음 (덮어쓰기 방지)
    version: props.mode === 'edit' ? props.issue?.version ?? null : null
  }
}

async function submit() {
  clearServerErrors()
  // 클라이언트 검증 실패 시 API 호출 안 함
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    let saved
    if (props.mode === 'edit') {
      saved = await issueStore.update(props.issue.id, payload())
    } else {
      saved = await issueStore.create(payload())
      // 대기열 파일 순차 업로드. 일부 실패해도 이슈는 유지하고 경고만 표시
      let failed = 0
      for (const file of queuedFiles.value) {
        try {
          await uploadAttachment(saved.id, file)
        } catch (e) {
          failed++
        }
      }
      if (failed > 0) {
        ElMessage.warning(`첨부파일 ${failed}개 업로드에 실패했습니다.`)
      }
    }
    ElMessage.success('저장되었습니다.')
    emit('saved', saved)
    visible.value = false
  } catch (e) {
    const fieldErrors = e.response?.data?.fieldErrors || []
    for (const fe of fieldErrors) {
      serverErrors[fe.field] = fe.message
    }
    if (e.response?.status === 400 && fieldErrors.length === 0) {
      ElMessage.error(e.response?.data?.message || '저장에 실패했습니다.')
    }
  } finally {
    saving.value = false
  }
}

function onQueued(files) {
  queuedFiles.value.push(...files)
}

function removeQueued(index) {
  queuedFiles.value.splice(index, 1)
}

function onUploaded(attachment) {
  attachments.value = [...attachments.value, attachment]
}

function onAttachmentDeleted(a) {
  attachments.value = attachments.value.filter((x) => x.id !== a.id)
}
</script>

<template>
  <el-dialog v-model="visible" :title="title" width="640px" destroy-on-close append-to-body>
    <div data-testid="issue-form-modal">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="프로젝트" prop="projectId" :error="serverErrors.projectId">
          <el-select v-model="form.projectId" placeholder="프로젝트 선택" style="width: 100%" data-testid="issue-form-project">
            <el-option v-for="p in lookup.projects" :key="p.id" :label="`${p.code} ${p.name}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="제목" prop="title" :error="serverErrors.title">
          <el-input v-model="form.title" maxlength="200" show-word-limit data-testid="issue-form-title" />
        </el-form-item>
        <el-form-item label="설명" :error="serverErrors.description">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="10000" />
        </el-form-item>
        <el-form-item label="상태">
          <el-select v-model="form.status" style="width: 160px" data-testid="issue-form-status">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="우선순위">
          <el-select v-model="form.priority" style="width: 160px" data-testid="issue-form-priority">
            <el-option v-for="o in priorityOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="진척률" :error="serverErrors.progress">
          <div style="display: flex; gap: 16px; width: 100%; align-items: center">
            <el-slider v-model="form.progress" :min="0" :max="100" :step="5" style="flex: 1" />
            <el-input-number v-model="form.progress" :min="0" :max="100" :step="1" size="small" data-testid="issue-form-progress" />
          </div>
        </el-form-item>
        <el-form-item label="담당자" :error="serverErrors.assigneeId">
          <el-select v-model="form.assigneeId" clearable filterable placeholder="미지정" style="width: 200px">
            <el-option v-for="u in lookup.users" :key="u.id" :label="`${u.name} (${u.username})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="마감일">
          <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" clearable />
        </el-form-item>
        <el-form-item label="첨부파일">
          <div style="width: 100%">
            <AttachmentUploader
              :issue-id="mode === 'edit' ? issue?.id : null"
              @queued="onQueued"
              @uploaded="onUploaded"
            />
            <!-- create 모드: 저장 시 업로드될 대기열 -->
            <div v-for="(f, i) in queuedFiles" :key="i" class="text-muted">
              {{ f.name }} <el-button link type="danger" size="small" @click="removeQueued(i)">제거</el-button>
            </div>
            <AttachmentList v-if="mode === 'edit'" :attachments="attachments" @deleted="onAttachmentDeleted" />
          </div>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="visible = false">취소</el-button>
      <el-button type="primary" :loading="saving" data-testid="issue-form-submit" @click="submit">저장</el-button>
    </template>
  </el-dialog>
</template>
