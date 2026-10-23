// QA 체크리스트 2 (FE): 2MB 경계 검증 함수
import { describe, it, expect } from 'vitest'
import { validateFile, MAX_FILE_SIZE, ALLOWED_EXTENSIONS, fileExtension, formatBytes } from '@/utils/file'

describe('validateFile', () => {
  it('2MB는 정확히 2,097,152 bytes', () => {
    expect(MAX_FILE_SIZE).toBe(2097152)
  })

  it('1 byte, 1KB, 정확히 2MB는 허용', () => {
    for (const size of [1, 1024, 2097152]) {
      expect(validateFile({ name: 'a.pdf', size })).toEqual({ ok: true })
    }
  })

  it('2MB + 1 byte, 3MB는 TOO_LARGE와 안내 문구', () => {
    for (const size of [2097153, 3 * 1024 * 1024]) {
      const r = validateFile({ name: 'a.pdf', size })
      expect(r.ok).toBe(false)
      expect(r.reason).toBe('TOO_LARGE')
      expect(r.message).toBe('파일 크기는 2MB를 초과할 수 없습니다.')
    }
  })

  it('크기 검사가 형식 검사보다 먼저 (3MB exe도 TOO_LARGE)', () => {
    expect(validateFile({ name: 'a.exe', size: 3 * 1024 * 1024 }).reason).toBe('TOO_LARGE')
  })

  it('빈 파일과 허용되지 않은 확장자', () => {
    expect(validateFile({ name: 'a.txt', size: 0 }).reason).toBe('EMPTY')
    expect(validateFile(null).reason).toBe('EMPTY')
    for (const name of ['a.exe', 'a.html', 'a.svg', 'README', 'a.']) {
      expect(validateFile({ name, size: 10 }).reason).toBe('TYPE')
    }
  })

  it('확장자는 대소문자 무시, 서버 allow-list와 동일', () => {
    expect(validateFile({ name: '보고서.PDF', size: 10 }).ok).toBe(true)
    expect(fileExtension('a.tar.ZIP')).toBe('zip')
    expect(ALLOWED_EXTENSIONS).toEqual([
      'pdf', 'png', 'jpg', 'jpeg', 'gif', 'txt', 'log', 'csv', 'md', 'zip',
      'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'hwp'
    ])
  })

  it('formatBytes', () => {
    expect(formatBytes(512)).toBe('512 B')
    expect(formatBytes(2048)).toBe('2.0 KB')
    expect(formatBytes(2097152)).toBe('2.00 MB')
  })
})
