import http, { versionParam } from './http'

// ----- 프로젝트 -----
export function listAdminProjects(params) {
  return http.get('/admin/projects', { params }).then((r) => r.data)
}
export function getAdminProject(id) {
  return http.get(`/admin/projects/${id}`).then((r) => r.data)
}
export function createProject(payload) {
  return http.post('/admin/projects', payload).then((r) => r.data)
}
export function updateProject(id, payload) {
  return http.put(`/admin/projects/${id}`, payload).then((r) => r.data)
}
// version: 화면이 불러온 버전 (다른 사람이 먼저 수정했으면 서버가 409로 삭제를 막음)
export function deleteProject(id, version) {
  return http.delete(`/admin/projects/${id}`, { params: versionParam(version) })
}

// ----- 할당 -----
export function listAssignments(projectId) {
  return http.get(`/admin/projects/${projectId}/assignments`).then((r) => r.data)
}
export function createAssignment(projectId, payload) {
  return http.post(`/admin/projects/${projectId}/assignments`, payload).then((r) => r.data)
}
export function updateAssignment(projectId, assignmentId, payload) {
  return http.put(`/admin/projects/${projectId}/assignments/${assignmentId}`, payload).then((r) => r.data)
}
export function deleteAssignment(projectId, assignmentId, version) {
  return http.delete(`/admin/projects/${projectId}/assignments/${assignmentId}`, { params: versionParam(version) })
}

// ----- 사용자 -----
export function listAdminUsers(params) {
  return http.get('/admin/users', { params }).then((r) => r.data)
}
export function createUser(payload) {
  return http.post('/admin/users', payload).then((r) => r.data)
}
export function updateUser(id, payload) {
  return http.put(`/admin/users/${id}`, payload).then((r) => r.data)
}

// ----- 타임시트 / 계획 vs 실제 -----
export function getTimesheet(params) {
  return http.get('/admin/timesheet', { params }).then((r) => r.data)
}
export function getPlanVsActual(params) {
  return http.get('/admin/plan-vs-actual', { params }).then((r) => r.data)
}
