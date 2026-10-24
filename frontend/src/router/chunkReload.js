// 재배포 후 예전 화면 파일(청크)을 못 불러올 때 자동 새로고침.
// 브라우저가 재배포 전 페이지를 열어 둔 채 메뉴를 누르면, 이미 지워진 예전 파일을 찾다가 화면 이동이 실패한다.
// 이때 이동하려던 주소로 한 번만 새로고침해서 최신 파일을 받게 한다. (무한 새로고침 방지: 10초 안에 다시 실패하면 멈춤)

const RELOAD_KEY = 'sipm.chunkReloadAt'

/** 동적 import(화면 파일 로딩) 실패 오류인지 (브라우저마다 메시지가 다름) */
export function isChunkLoadError(error) {
  const msg = String(error?.message || error || '')
  return /Failed to fetch dynamically imported module|error loading dynamically imported module|Importing a module script failed|Loading chunk .* failed|Unable to preload CSS/i.test(msg)
}

/**
 * router.onError 핸들러. 화면 파일 로딩 실패면 이동하려던 주소로 새로고침하고 true를 돌려준다.
 * storage/location은 테스트에서 바꿔 끼울 수 있게 인자로 받는다.
 */
export function reloadOnChunkError(error, to, { storage = sessionStorage, location = window.location, now = Date.now() } = {}) {
  if (!isChunkLoadError(error)) return false
  const last = Number(storage.getItem(RELOAD_KEY) || 0)
  if (now - last < 10000) return false // 방금 새로고침했는데 또 실패 → 더 이상 새로고침하지 않음
  storage.setItem(RELOAD_KEY, String(now))
  location.assign(to?.fullPath || location.href)
  return true
}
