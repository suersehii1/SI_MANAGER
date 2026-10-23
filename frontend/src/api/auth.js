import http from './http'

export function login(username, password) {
  return http.post('/auth/login', { username, password }).then((r) => r.data)
}

export function fetchMe() {
  return http.get('/auth/me').then((r) => r.data)
}
