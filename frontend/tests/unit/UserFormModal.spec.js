// Admin 사용자 등록/수정 팝업: 직급은 정해진 8개 값 중에서 선택
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mountWith, settle, $, click } from './helpers'
import UserFormModal from '@/components/admin/UserFormModal.vue'
import { createUser } from '@/api/admin'

vi.mock('@/api/admin', () => ({ createUser: vi.fn(), updateUser: vi.fn() }))

// 직급 드롭다운을 열고 옵션 요소 목록을 돌려준다
async function openJobTitleOptions() {
  $('[data-testid="user-form-job-title"]').querySelector('.el-select__wrapper, input').click()
  await settle()
  return [...document.body.querySelectorAll('.el-select-dropdown__item')]
}

describe('UserFormModal 직급 선택', () => {
  let wrapper

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ''
  })

  it('직급 선택 목록은 사원~기타 8개', async () => {
    wrapper = mountWith(UserFormModal, { props: { modelValue: true, user: null } })
    await settle()
    const labels = (await openJobTitleOptions()).map((el) => el.textContent.trim())
    expect(labels).toEqual(['사원', '선임', '책임', '수석', '임원', '대표', '부대표', '기타'])
  })

  it('선택한 직급으로 사용자를 등록한다', async () => {
    createUser.mockResolvedValue({ id: 9, username: 'park' })
    wrapper = mountWith(UserFormModal, { props: { modelValue: true, user: null } })
    await settle()
    // 팝업 안의 입력칸 순서: 아이디, 비밀번호, 이름, 이메일
    const inputs = document.body.querySelectorAll('[data-testid="user-form"] input')
    for (const [el, value] of [[inputs[0], 'park'], [inputs[1], 'park1234'], [inputs[2], '박민수']]) {
      el.value = value
      el.dispatchEvent(new Event('input'))
    }
    await settle()
    const option = (await openJobTitleOptions()).find((el) => el.textContent.trim() === '수석')
    option.click()
    await settle()
    // 저장 버튼 (dialog footer의 primary 버튼)
    await click('.el-dialog__footer .el-button--primary')
    expect(createUser).toHaveBeenCalledWith(expect.objectContaining({ username: 'park', name: '박민수', jobTitle: '수석' }))
  })

  // 입력칸 채우기: 아이디, 비밀번호, 이름 + data-testid로 영어 이름/전화번호
  async function fillBasic(englishName, phone) {
    const inputs = document.body.querySelectorAll('[data-testid="user-form"] input')
    for (const [el, value] of [[inputs[0], 'park'], [inputs[1], 'park1234'], [inputs[2], '박민수']]) {
      el.value = value
      el.dispatchEvent(new Event('input'))
    }
    for (const [testid, value] of [['user-form-english-name', englishName], ['user-form-phone', phone]]) {
      const el = document.body.querySelector(`[data-testid="${testid}"]`)
      const input = el.tagName === 'INPUT' ? el : el.querySelector('input')
      input.value = value
      input.dispatchEvent(new Event('input'))
    }
    await settle()
  }

  it('영어 이름·전화번호를 입력하면 함께 등록된다', async () => {
    createUser.mockResolvedValue({ id: 9, username: 'park' })
    wrapper = mountWith(UserFormModal, { props: { modelValue: true, user: null } })
    await settle()
    await fillBasic('Minsu Park', '010-1234-5678')
    await click('.el-dialog__footer .el-button--primary')
    expect(createUser).toHaveBeenCalledWith(expect.objectContaining({ englishName: 'Minsu Park', phone: '010-1234-5678' }))
  })

  it('영어 이름에 한글이 있으면 저장하지 않는다', async () => {
    wrapper = mountWith(UserFormModal, { props: { modelValue: true, user: null } })
    await settle()
    await fillBasic('박민수', '')
    await click('.el-dialog__footer .el-button--primary')
    expect(createUser).not.toHaveBeenCalled()
    expect(document.body.textContent).toContain('영문자로 시작하고')
  })
})
