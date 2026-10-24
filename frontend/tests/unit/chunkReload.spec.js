// 재배포 후 예전 화면 파일 로딩 실패 → 이동하려던 주소로 한 번만 새로고침
import { describe, it, expect, vi } from 'vitest'
import { isChunkLoadError, reloadOnChunkError } from '@/router/chunkReload'

function fakeEnv() {
  const store = new Map()
  return {
    storage: { getItem: (k) => store.get(k) ?? null, setItem: (k, v) => store.set(k, v) },
    location: { href: '/dashboard', assign: vi.fn() }
  }
}

describe('chunkReload', () => {
  it('브라우저별 화면 파일 로딩 실패 메시지를 알아본다', () => {
    expect(isChunkLoadError(new TypeError('Failed to fetch dynamically imported module: /assets/IssueListView-ab12.js'))).toBe(true)
    expect(isChunkLoadError(new TypeError('error loading dynamically imported module'))).toBe(true)
    expect(isChunkLoadError(new TypeError('Importing a module script failed.'))).toBe(true)
    expect(isChunkLoadError(new Error('Request failed with status code 500'))).toBe(false)
  })

  it('로딩 실패면 이동하려던 주소로 새로고침', () => {
    const env = fakeEnv()
    const done = reloadOnChunkError(new TypeError('Failed to fetch dynamically imported module'), { fullPath: '/issues' }, { ...env, now: 100000 })
    expect(done).toBe(true)
    expect(env.location.assign).toHaveBeenCalledWith('/issues')
  })

  it('10초 안에 또 실패하면 새로고침하지 않는다 (무한 새로고침 방지)', () => {
    const env = fakeEnv()
    const err = new TypeError('Failed to fetch dynamically imported module')
    reloadOnChunkError(err, { fullPath: '/wbs' }, { ...env, now: 100000 })
    expect(reloadOnChunkError(err, { fullPath: '/wbs' }, { ...env, now: 105000 })).toBe(false)
    expect(env.location.assign).toHaveBeenCalledTimes(1)
    // 충분히 시간이 지나면(다음 재배포) 다시 동작
    expect(reloadOnChunkError(err, { fullPath: '/wbs' }, { ...env, now: 200000 })).toBe(true)
  })

  it('다른 오류는 무시', () => {
    const env = fakeEnv()
    expect(reloadOnChunkError(new Error('boom'), { fullPath: '/issues' }, env)).toBe(false)
    expect(env.location.assign).not.toHaveBeenCalled()
  })
})
