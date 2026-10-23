import http from './http'

export function getSummary(params) {
  return http.get('/dashboard/summary', { params }).then((r) => r.data)
}

export function getProjects(params) {
  return http.get('/dashboard/projects', { params }).then((r) => r.data)
}
