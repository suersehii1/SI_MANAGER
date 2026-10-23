import http from './http'

// 활성 사용자 lookup (담당자 선택용)
export function listUsers(keyword) {
  return http.get('/users', { params: { keyword: keyword || undefined } }).then((r) => r.data)
}
