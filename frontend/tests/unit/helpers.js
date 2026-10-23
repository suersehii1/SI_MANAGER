// 컴포넌트 테스트 공통 헬퍼: Element Plus + Pinia를 등록해 mount 하고, 비동기 검증/렌더링을 기다린다.
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import * as Icons from '@element-plus/icons-vue'

export function mountWith(component, { props = {}, pinia, router, attachTo = document.body } = {}) {
  const plugins = [ElementPlus]
  if (pinia) plugins.push(pinia)
  if (router) plugins.push(router)
  return mount(component, {
    props,
    attachTo,
    global: { plugins, components: Icons }
  })
}

// el-form 검증(async-validator)과 API 목 Promise가 모두 끝날 때까지 여러 번 비운다
export async function settle(times = 5) {
  for (let i = 0; i < times; i++) {
    await flushPromises()
  }
}

// el-dialog는 body로 teleport 되므로 document 기준으로 찾는다
export function $(selector) {
  return document.body.querySelector(selector)
}

// el-input은 data-testid 같은 속성을 내부 <input>에 그대로 붙인다
export function inputOf(testid) {
  const el = $(`[data-testid="${testid}"]`)
  return el && el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA' ? el.querySelector('input, textarea') : el
}

export async function typeInto(testid, value) {
  const input = inputOf(testid)
  input.value = value
  input.dispatchEvent(new Event('input'))
  input.dispatchEvent(new Event('change'))
  await settle()
}

// el-form-item 에러 문구는 100ms 디바운스 후 그려지므로 잠시 기다린다
export async function click(selector) {
  $(selector).click()
  await settle()
  await new Promise((resolve) => setTimeout(resolve, 150))
  await settle()
}

export function fakeFile(name, size) {
  const file = new File(['x'], name)
  Object.defineProperty(file, 'size', { value: size })
  return file
}
