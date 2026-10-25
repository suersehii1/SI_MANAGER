import axios from 'axios'
import { ElMessage } from 'element-plus'

// 공용 axios 인스턴스. 모든 API는 /api 아래 (nginx 또는 Vite dev proxy가 백엔드로 전달)
const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export const TOKEN_KEY = 'sipm.token'

/** 삭제 요청에 붙일 낙관적 잠금 버전 (?version=값). 버전을 모르면 붙이지 않는다 → 서버는 검사하지 않음 */
export function versionParam(version) {
  return version === null || version === undefined ? undefined : { version }
}

/**
 * 인터셉터 등록. router/pinia를 인자로 받아 순환 import를 피하고, 테스트에서 쉽게 대체할 수 있게 한다.
 * - 요청: localStorage 토큰이 있으면 Authorization: Bearer 헤더 추가
 * - 응답 에러:
 *   401 → (로그인 요청이 아니면) 로그아웃 후 로그인 화면으로, 현재 경로를 redirect로 전달
 *   403 → GET이면 /forbidden, 변경 요청(POST/PUT/PATCH/DELETE)이면 메시지만 표시하고 현재 화면 유지
 *   413 → 2MB 초과 메시지
 *   400 + fieldErrors → 호출한 화면(폼)이 필드별로 처리하므로 공통 메시지 생략
 *   그 외 → 서버 message 또는 기본 메시지
 */
export function setupInterceptors(router, useAuthStore) {
  http.interceptors.request.use((config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  http.interceptors.response.use(
    (response) => response,
    (error) => {
      const status = error.response?.status
      const url = error.config?.url || ''
      const data = error.response?.data
      if (status === 401) {
        if (!url.includes('/auth/login')) {
          useAuthStore().logout()
          const current = router.currentRoute.value
          if (current.name !== 'login') {
            router.push({ name: 'login', query: { redirect: current.fullPath } })
          }
        }
      } else if (status === 403) {
        // 화면 데이터 조회(GET)의 403만 forbidden 화면으로 이동한다.
        // 저장/삭제 등 변경 요청(POST/PUT/PATCH/DELETE)의 403은 메시지만 띄우고 현재 화면을 유지한다
        // (예: WBS 편집 중 프로젝트 할당이 해제된 경우). GET도 config.skipForbiddenRedirect로 이동을 끌 수 있다.
        const method = (error.config?.method || 'get').toLowerCase()
        if (method === 'get' && !error.config?.skipForbiddenRedirect) {
          router.push({ name: 'forbidden' })
        } else {
          ElMessage.error(data?.message ?? '접근 권한이 없습니다.')
        }
      } else if (status === 413) {
        ElMessage.error('파일 크기는 2MB를 초과할 수 없습니다.')
      } else if (status === 400 && data?.fieldErrors?.length) {
        // 폼에서 fieldErrors를 표시한다
      } else if (!error.config?.silent) {
        ElMessage.error(data?.message ?? '요청 처리 중 오류가 발생했습니다.')
      }
      return Promise.reject(error)
    }
  )
}

export default http
