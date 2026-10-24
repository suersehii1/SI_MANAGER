import http from './http'

export function login(username, password) {
  return http.post('/auth/login', { username, password }).then((r) => r.data)
}

export function fetchMe() {
  return http.get('/auth/me').then((r) => r.data)
}

// 내 화면 스킨 저장 (theme: 'LIGHT' | 'DARK'). 응답은 갱신된 내 정보
export function updateMyTheme(theme) {
  return http.put('/auth/me/theme', { theme }).then((r) => r.data)
}
