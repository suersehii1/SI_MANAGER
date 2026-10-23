import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import { TOKEN_KEY } from '@/api/http'

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
    roles: (state) => state.user?.roles || []
  },
  actions: {
    async login(username, password) {
      const res = await authApi.login(username, password)
      this.token = res.accessToken
      this.user = res.user
      localStorage.setItem(TOKEN_KEY, res.accessToken)
      return res.user
    },
    async fetchMe() {
      this.user = await authApi.fetchMe()
      return this.user
    },
    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
    }
  }
})
