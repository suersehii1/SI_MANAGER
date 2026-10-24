<script setup>
import { computed, ref, watch } from 'vue'
import { useIssueStore } from '@/stores/issue'
import { useAuthStore } from '@/stores/auth'
import StatusTag from '@/components/common/StatusTag.vue'
import PriorityTag from '@/components/common/PriorityTag.vue'
import AttachmentList from '@/components/attachments/AttachmentList.vue'
import AttachmentUploader from '@/components/attachments/AttachmentUploader.vue'
import { formatDate, formatDateTime } from '@/utils/format'

// 이슈 상세 Drawer (우측). 첨부 업로드/삭제 후 상세를 다시 읽고 changed를 알린다.
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  issueId: { type: Number, default: null }
})
const emit = defineEmits(['update:modelValue', 'edit', 'delete', 'changed'])

const issueStore = useIssueStore()
const auth = useAuthStore()
const issue = ref(null)
const loading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const canDelete = computed(() => auth.isAdmin || issue.value?.reporter?.id === auth.user?.id)

async function load() {
  if (!props.issueId) return
  loading.value = true
  try {
    issue.value = await issueStore.fetchOne(props.issueId)
  } catch (e) {
    issue.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.issueId],
  ([open]) => {
    if (open) load()
  },
  { immediate: true }
)

async function onAttachmentChanged() {
  await load()
  emit('changed')
}

defineExpose({ reload: load })
</script>

<template>
  <el-drawer v-model="visible" title="이슈 상세" size="480px" append-to-body>
    <div v-loading="loading" data-testid="issue-detail-drawer">
      <template v-if="issue">
        <h3 style="margin-top: 0">#{{ issue.id }} {{ issue.title }}</h3>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="프로젝트">{{ issue.projectCode }} {{ issue.projectName }}</el-descriptions-item>
          <el-descriptions-item label="상태"><StatusTag :value="issue.status" /></el-descriptions-item>
          <el-descriptions-item label="우선순위"><PriorityTag :value="issue.priority" /></el-descriptions-item>
          <el-descriptions-item label="진척률"><el-progress :percentage="issue.progress" /></el-descriptions-item>
          <el-descriptions-item label="담당자">{{ issue.assignee?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="보고자">{{ issue.reporter?.name }}</el-descriptions-item>
          <el-descriptions-item label="마감일">{{ formatDate(issue.dueDate) }}</el-descriptions-item>
          <el-descriptions-item label="등록일">{{ formatDateTime(issue.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="수정일">{{ formatDateTime(issue.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
        <h4>설명</h4>
        <div class="issue-description">{{ issue.description || '(없음)' }}</div>
        <h4>첨부파일</h4>
        <AttachmentUploader :issue-id="issue.id" @uploaded="onAttachmentChanged" />
        <AttachmentList :attachments="issue.attachments" @deleted="onAttachmentChanged" />
        <div style="margin-top: 20px">
          <el-button type="primary" @click="emit('edit', issue)">수정</el-button>
          <el-button v-if="canDelete" type="danger" @click="emit('delete', issue)">삭제</el-button>
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<style scoped>
.issue-description {
  white-space: pre-wrap;
  background: var(--el-fill-color-light);
  padding: 10px;
  border-radius: 4px;
}
</style>
