import http, { versionParam } from './http'

export function listIssues(params) {
  return http.get('/issues', { params }).then((r) => r.data)
}
export function getIssue(id) {
  return http.get(`/issues/${id}`).then((r) => r.data)
}
export function createIssue(payload) {
  return http.post('/issues', payload).then((r) => r.data)
}
export function updateIssue(id, payload) {
  return http.put(`/issues/${id}`, payload).then((r) => r.data)
}
// version: 화면이 불러온 버전 (다른 사람이 먼저 수정했으면 서버가 409로 삭제를 막음)
export function deleteIssue(id, version) {
  return http.delete(`/issues/${id}`, { params: versionParam(version) })
}
