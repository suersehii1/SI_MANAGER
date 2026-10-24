import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import { TOKEN_KEY } from '@/api/http'
import { applyTheme, normalizeTheme } from '@/utils/theme'

function readToken() {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch (e) {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: readToken(),
    user: null
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => !!state.user?.roles?.includes('ROLE_ADMIN'),
    roles: (state) => state.user?.roles || [],
    // 화면 스킨: 로그인 사용자에 저장된 값, 로그인 전이면 낮(LIGHT)
    theme: (state) => normalizeTheme(state.user?.theme)
  },
  actions: {
    async login(username, password) {
      const res = await authApi.login(username, password)
      this.token = res.accessToken
      this.user = res.user
      localStorage.setItem(TOKEN_KEY, res.accessToken)
      applyTheme(this.theme) // 저장된 스킨으로 바로 전환
      return res.user
    },
    async fetchMe() {
      this.user = await authApi.fetchMe()
      applyTheme(this.theme) // 새로고침 후 다시 로그인 정보를 받을 때도 스킨 유지
      return this.user
    },
    /** 스킨 변경: 화면에 먼저 적용하고 서버에 저장. 저장 실패 시 이전 스킨으로 되돌린다 */
    async setTheme(theme) {
      const before = this.theme
      this.user = { ...this.user, theme }
      applyTheme(theme)
      try {
        this.user = await authApi.updateMyTheme(theme)
      } catch (e) {
        this.user = { ...this.user, theme: before }
        applyTheme(before)
        throw e
      }
    },
    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      applyTheme('LIGHT') // 로그인 화면은 항상 낮 스킨
    }
  }
})
