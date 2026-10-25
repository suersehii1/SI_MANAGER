import http, { versionParam } from './http'

export function getWbsTree(projectId, params) {
  return http.get(`/projects/${projectId}/wbs`, { params }).then((r) => r.data)
}
export function createWbsItem(projectId, payload) {
  return http.post(`/projects/${projectId}/wbs`, payload).then((r) => r.data)
}
export function updateWbsItem(id, payload) {
  return http.put(`/wbs/${id}`, payload).then((r) => r.data)
}
// version: 화면이 불러온 버전 (다른 사람이 먼저 수정했으면 서버가 409로 삭제를 막음)
export function deleteWbsItem(id, version) {
  return http.delete(`/wbs/${id}`, { params: versionParam(version) })
}
export function moveWbsItem(id, payload) {
  return http.post(`/wbs/${id}/move`, payload).then((r) => r.data)
}
