<script setup>
import { ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { validateFile, TOO_LARGE_MESSAGE, TYPE_MESSAGE, EMPTY_MESSAGE, MAX_FILE_SIZE } from '@/utils/file'
import { uploadAttachment } from '@/api/attachments'

/**
 * 첨부파일 선택/업로드 컴포넌트.
 * - issueId가 null이면 "대기열 모드": 검증 통과 파일을 queued로 넘기고, 이슈 생성 후 부모가 업로드한다.
 * - issueId가 있으면 선택 즉시 업로드.
 * - 클라이언트 검증(2MB 초과/형식/빈 파일) 실패 시 alert만 띄우고 업로드 요청을 보내지 않는다.
 */
const props = defineProps({
  issueId: { type: [Number, String], default: null },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['queued', 'uploaded', 'rejected'])

const inputRef = ref()
const uploading = ref(false)

function openPicker() {
  inputRef.value?.click()
}

function alertFor(reason) {
  const message = reason === 'TOO_LARGE' ? TOO_LARGE_MESSAGE : reason === 'TYPE' ? TYPE_MESSAGE : EMPTY_MESSAGE
  return ElMessageBox.alert(message, '업로드 불가', { type: 'error' }).catch(() => {})
}

async function onChange(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = '' // 같은 파일 재선택 허용
  const accepted = []
  for (const file of files) {
    const result = validateFile(file)
    if (!result.ok) {
      // 클라이언트단 차단: 서버 요청 없이 alert 후 rejected 이벤트
      emit('rejected', result.reason)
      await alertFor(result.reason)
      continue
    }
    accepted.push(file)
  }
  if (accepted.length === 0) return

  if (props.issueId == null) {
    emit('queued', accepted)
    return
  }
  uploading.value = true
  try {
    for (const file of accepted) {
      try {
        const attachment = await uploadAttachment(props.issueId, file)
        emit('uploaded', attachment)
      } catch (e) {
        // 서버 413/400(FILE_TOO_LARGE 등)도 같은 문구로 alert
        const status = e.response?.status
        const code = e.response?.data?.code
        if (status === 413 || code === 'FILE_TOO_LARGE') {
          await alertFor('TOO_LARGE')
        } else if (code === 'FILE_TYPE_NOT_ALLOWED') {
          await alertFor('TYPE')
        } else if (code === 'FILE_EMPTY') {
          await alertFor('EMPTY')
        } else {
          ElMessage.error(e.response?.data?.message || '업로드에 실패했습니다.')
        }
      }
    }
  } finally {
    uploading.value = false
  }
}

defineExpose({ onChange, maxSize: MAX_FILE_SIZE })
</script>

<template>
  <div class="attachment-uploader">
    <input
      ref="inputRef"
      type="file"
      multiple
      style="display: none"
      data-testid="attachment-input"
      @change="onChange"
    />
    <el-button size="small" :loading="uploading" :disabled="disabled" data-testid="attachment-pick-btn" @click="openPicker">
      <el-icon><Paperclip /></el-icon>&nbsp;파일 첨부
    </el-button>
    <span class="text-muted">&nbsp;파일당 최대 2MB</span>
  </div>
</template>
