<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { deleteAttachment, downloadAttachment } from '@/api/attachments'
import { formatBytes } from '@/utils/file'
import { formatDateTime } from '@/utils/format'

const props = defineProps({
  attachments: { type: Array, default: () => [] }
})
const emit = defineEmits(['deleted'])
const auth = useAuthStore()

// 삭제 버튼은 업로더 본인 또는 ADMIN에게만 표시 (서버도 403으로 검사)
function canDelete(a) {
  return auth.isAdmin || a.uploadedBy?.id === auth.user?.id
}

async function download(a) {
  try {
    await downloadAttachment(a)
  } catch (e) {
    // 공통 인터셉터가 메시지 표시
  }
}

async function remove(a) {
  try {
    await ElMessageBox.confirm(`'${a.originalFilename}' 파일을 삭제할까요?`, '첨부 삭제', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteAttachment(a.id)
  ElMessage.success('삭제되었습니다.')
  emit('deleted', a)
}
</script>

<template>
  <div class="attachment-list" data-testid="attachment-list">
    <div v-if="props.attachments.length === 0" class="text-muted">첨부파일이 없습니다.</div>
    <div v-for="a in props.attachments" :key="a.id" class="attachment-list__item">
      <el-link type="primary" @click="download(a)">{{ a.originalFilename }}</el-link>
      <span class="text-muted">{{ formatBytes(a.sizeBytes) }} · {{ a.uploadedBy?.name }} · {{ formatDateTime(a.createdAt) }}</span>
      <el-button v-if="canDelete(a)" link type="danger" size="small" @click="remove(a)">삭제</el-button>
    </div>
  </div>
</template>

<style scoped>
.attachment-list__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}
</style>
