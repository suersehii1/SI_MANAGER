import http from './http'

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
export function deleteIssue(id) {
  return http.delete(`/issues/${id}`)
}
