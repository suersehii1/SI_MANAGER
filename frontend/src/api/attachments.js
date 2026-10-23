import http from './http'

// 파일 업로드. silent: 공통 에러 메시지 대신 업로더 컴포넌트가 alert로 처리
export function uploadAttachment(issueId, file) {
  const form = new FormData()
  form.append('file', file)
  return http.post(`/issues/${issueId}/attachments`, form, { silent: true }).then((r) => r.data)
}

export function listAttachments(issueId) {
  return http.get(`/issues/${issueId}/attachments`).then((r) => r.data)
}

export function deleteAttachment(id) {
  return http.delete(`/attachments/${id}`)
}

// 다운로드: blob으로 받아 임시 <a download>를 클릭한다 (Authorization 헤더가 필요하므로 단순 링크 불가)
export async function downloadAttachment(attachment) {
  const res = await http.get(`/attachments/${attachment.id}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = attachment.originalFilename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
