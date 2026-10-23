import http from './http'

// 인증 사용자용 프로젝트 lookup
export function listProjects() {
  return http.get('/projects').then((r) => r.data)
}

// 프로젝트 멤버 (WBS 담당자 선택용)
export function listMembers(projectId) {
  return http.get(`/projects/${projectId}/members`).then((r) => r.data)
}
