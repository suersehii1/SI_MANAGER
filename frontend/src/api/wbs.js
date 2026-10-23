import http from './http'

export function getWbsTree(projectId, params) {
  return http.get(`/projects/${projectId}/wbs`, { params }).then((r) => r.data)
}
export function createWbsItem(projectId, payload) {
  return http.post(`/projects/${projectId}/wbs`, payload).then((r) => r.data)
}
export function updateWbsItem(id, payload) {
  return http.put(`/wbs/${id}`, payload).then((r) => r.data)
}
export function deleteWbsItem(id) {
  return http.delete(`/wbs/${id}`)
}
export function moveWbsItem(id, payload) {
  return http.post(`/wbs/${id}/move`, payload).then((r) => r.data)
}
