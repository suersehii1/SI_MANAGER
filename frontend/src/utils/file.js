// 클라이언트단 첨부파일 검증 (specs §6.2, §7.1 ①).
// 서버(Spring multipart 2MB, 서비스 검증)와 동일한 기준을 먼저 적용해 불필요한 업로드 요청을 막는다.

// 2MB = 2,097,152 bytes. 정확히 2,097,152 bytes는 허용, 1 byte라도 넘으면 거부.
export const MAX_FILE_SIZE = 2 * 1024 * 1024

// 서버 allow-list(§7.3)와 동일
export const ALLOWED_EXTENSIONS = [
  'pdf', 'png', 'jpg', 'jpeg', 'gif', 'txt', 'log', 'csv', 'md', 'zip',
  'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'hwp'
]

export const TOO_LARGE_MESSAGE = '파일 크기는 2MB를 초과할 수 없습니다.'
export const TYPE_MESSAGE = '허용되지 않는 파일 형식입니다.'
export const EMPTY_MESSAGE = '빈 파일은 업로드할 수 없습니다.'

export function fileExtension(name) {
  if (!name) return ''
  const dot = name.lastIndexOf('.')
  return dot >= 0 && dot < name.length - 1 ? name.substring(dot + 1).toLowerCase() : ''
}

/**
 * 파일 검증. 성공: { ok: true } / 실패: { ok: false, reason: 'TOO_LARGE'|'EMPTY'|'TYPE', message }
 */
export function validateFile(file) {
  if (!file || file.size === 0) {
    return { ok: false, reason: 'EMPTY', message: EMPTY_MESSAGE }
  }
  if (file.size > MAX_FILE_SIZE) {
    return { ok: false, reason: 'TOO_LARGE', message: TOO_LARGE_MESSAGE }
  }
  if (!ALLOWED_EXTENSIONS.includes(fileExtension(file.name))) {
    return { ok: false, reason: 'TYPE', message: TYPE_MESSAGE }
  }
  return { ok: true }
}

export function formatBytes(bytes) {
  if (bytes == null) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}
