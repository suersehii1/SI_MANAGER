// axios 인터셉터: 401 → 로그아웃+로그인 화면, 403 → (GET) forbidden / (변경 요청) 메시지만, 413 → 2MB 문구, 토큰 헤더 부착
import { describe, it, expect, vi, beforeAll, beforeEach } from 'vitest'
import http, { setupInterceptors, TOKEN_KEY } from '@/api/http'
import { ElMessage } from 'element-plus'

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, ElMessage: { ...actual.ElMessage, error: vi.fn() } }
})

const auth = { logout: vi.fn() }
const router = { push: vi.fn(), currentRoute: { value: { name: 'issues', fullPath: '/issues?page=2' } } }

// 등록된 인터셉터 핸들러를 직접 호출한다 (네트워크 없이)
const requestHandler = () => http.interceptors.request.handlers[0].fulfilled
const errorHandler = () => http.interceptors.response.handlers[0].rejected

function fail(status, url = '/issues', data = {}, config = {}) {
  return errorHandler()({ response: { status, data }, config: { url, ...config } }).catch((e) => e)
}

describe('http interceptors', () => {
  beforeAll(() => setupInterceptors(router, () => auth))
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('토큰이 있으면 Authorization: Bearer 헤더', () => {
    localStorage.setItem(TOKEN_KEY, 'abc')
    expect(requestHandler()({ headers: {} }).headers.Authorization).toBe('Bearer abc')
    localStorage.clear()
    expect(requestHandler()({ headers: {} }).headers.Authorization).toBeUndefined()
  })

  it('401 → 로그아웃 후 현재 경로를 redirect로 로그인 화면', async () => {
    const err = await fail(401)
    expect(err.response.status).toBe(401) // 항상 reject
    expect(auth.logout).toHaveBeenCalled()
    expect(router.push).toHaveBeenCalledWith({ name: 'login', query: { redirect: '/issues?page=2' } })
  })

  it('로그인 요청의 401(비밀번호 오류)은 리다이렉트하지 않음', async () => {
    await fail(401, '/auth/login')
    expect(auth.logout).not.toHaveBeenCalled()
    expect(router.push).not.toHaveBeenCalled()
  })

  it('GET 403 → forbidden 화면', async () => {
    await fail(403, '/admin/projects', {}, { method: 'get' })
    expect(router.push).toHaveBeenCalledWith({ name: 'forbidden' })
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('method 미지정(axios 기본 GET) 403 → forbidden 화면', async () => {
    await fail(403, '/admin/projects')
    expect(router.push).toHaveBeenCalledWith({ name: 'forbidden' })
  })

  it('O-03: 변경 요청(POST/PUT/PATCH/DELETE) 403 → 서버 메시지만 표시, 화면 유지', async () => {
    for (const method of ['post', 'put', 'patch', 'delete', 'PUT']) {
      vi.clearAllMocks()
      const err = await fail(403, '/wbs/1', { message: '접근 권한이 없습니다.' }, { method })
      expect(err.response.status).toBe(403) // 항상 reject
      expect(router.push).not.toHaveBeenCalled()
      expect(ElMessage.error).toHaveBeenCalledWith('접근 권한이 없습니다.')
    }
  })

  it('O-03: 서버 메시지가 없으면 기본 문구, GET도 skipForbiddenRedirect면 화면 유지', async () => {
    await fail(403, '/wbs/1', {}, { method: 'delete' })
    expect(ElMessage.error).toHaveBeenCalledWith('접근 권한이 없습니다.')
    vi.clearAllMocks()
    await fail(403, '/projects/1/wbs', { message: '권한 없음' }, { method: 'get', skipForbiddenRedirect: true })
    expect(router.push).not.toHaveBeenCalled()
    expect(ElMessage.error).toHaveBeenCalledWith('권한 없음')
  })

  it('413 → 2MB 안내 메시지', async () => {
    await fail(413, '/issues/1/attachments')
    expect(ElMessage.error).toHaveBeenCalledWith('파일 크기는 2MB를 초과할 수 없습니다.')
  })

  it('400 fieldErrors는 폼이 처리하므로 공통 메시지 생략, 그 외는 서버 메시지', async () => {
    await fail(400, '/issues', { fieldErrors: [{ field: 'title' }] })
    expect(ElMessage.error).not.toHaveBeenCalled()
    await fail(409, '/admin/projects', { message: '이미 사용 중인 프로젝트 코드입니다.' })
    expect(ElMessage.error).toHaveBeenCalledWith('이미 사용 중인 프로젝트 코드입니다.')
  })
})
