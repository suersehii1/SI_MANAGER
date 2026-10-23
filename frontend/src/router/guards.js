import { useAuthStore } from '@/stores/auth'

/**
 * 전역 라우터 가드 (specs §6.1). 단위 테스트를 위해 별도 함수로 export.
 * 1) requiresAuth 경로인데 토큰 없음 → login?redirect=원래경로
 * 2) 토큰은 있는데 사용자 정보가 없으면 /api/auth/me 조회 (실패 시 로그아웃 → login)
 * 3) matched 레코드들의 meta.roles를 합쳐 사용자 roles와 교집합이 없으면 → forbidden
 * 4) 로그인 상태에서 /login 접근 → /dashboard
 */
export async function authGuard(to) {
  const auth = useAuthStore()
  const requiresAuth = to.matched.some((r) => r.meta && r.meta.requiresAuth)

  if (to.name === 'login') {
    if (auth.token) {
      if (!auth.user) {
        try {
          await auth.fetchMe()
        } catch (e) {
          auth.logout()
          return true
        }
      }
      return { name: 'dashboard' }
    }
    return true
  }

  if (!requiresAuth) {
    return true
  }

  if (!auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch (e) {
      auth.logout()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  const requiredRoles = []
  for (const record of to.matched) {
    if (record.meta && Array.isArray(record.meta.roles)) {
      requiredRoles.push(...record.meta.roles)
    }
  }
  if (requiredRoles.length > 0) {
    const userRoles = auth.user?.roles || []
    const allowed = requiredRoles.some((role) => userRoles.includes(role))
    if (!allowed) {
      return { name: 'forbidden' }
    }
  }
  return true
}
