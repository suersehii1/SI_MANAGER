// AG Grid Community 공통 설정/헬퍼.
// (Enterprise 기능 — treeData, row grouping, master/detail, serverSide row model — 은 사용하지 않는다.)
import { ISSUE_STATUS, ISSUE_PRIORITY, PROJECT_STATUS, ASSIGNMENT_STATE, WBS_STATUS } from './constants'

export const GRID_THEME_CLASS = 'ag-theme-quartz'

export const defaultColDef = {
  resizable: true,
  sortable: false,
  suppressMovable: true
}

export const localeText = {
  noRowsToShow: '데이터가 없습니다.',
  loadingOoo: '불러오는 중...'
}

function escapeHtml(text) {
  return String(text ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

// enum 값을 Element Plus 스타일 태그 HTML로 렌더링 (단순 문자열 렌더러 — Vue 컴포넌트 렌더러 대신 사용)
function tagRenderer(map) {
  return (params) => {
    const v = params.value
    if (!v) return ''
    const item = map[v] || { label: v, type: '' }
    const typeClass = item.type ? `el-tag--${item.type}` : ''
    return `<span class="el-tag el-tag--small el-tag--light ${typeClass}">${escapeHtml(item.label)}</span>`
  }
}

export const issueStatusRenderer = tagRenderer(ISSUE_STATUS)
export const issuePriorityRenderer = tagRenderer(ISSUE_PRIORITY)
export const projectStatusRenderer = tagRenderer(PROJECT_STATUS)
export const assignmentStateRenderer = tagRenderer(ASSIGNMENT_STATE)
export const wbsStatusRenderer = tagRenderer(WBS_STATUS)

// 진척률 막대 (0..100)
export function progressRenderer(params) {
  const v = params.value == null ? 0 : Number(params.value)
  const pct = Math.max(0, Math.min(100, v))
  const text = Number.isInteger(v) ? `${v}%` : `${v.toFixed(1)}%`
  return `<div class="grid-progress"><div class="grid-progress__bar" style="width:${pct}%"></div>`
    + `<span class="grid-progress__text">${text}</span></div>`
}

/**
 * 액션 버튼 렌더러. buttons: [{ action, label, type, testid }]
 * 클릭 처리는 그리드의 cellClicked 이벤트에서 event.target의 data-action을 읽어 수행한다.
 */
export function actionsRenderer(buttons) {
  return (params) => {
    const id = params.data?.id
    return buttons
      .filter((b) => !b.visible || b.visible(params.data))
      .map((b) => {
        const testid = b.testid ? ` data-testid="${b.testid}-${id}"` : ''
        return `<button type="button" class="el-button el-button--small el-button--${b.type || 'default'} is-link"`
          + ` data-action="${b.action}"${testid}>${escapeHtml(b.label)}</button>`
      })
      .join('')
  }
}

/** cellClicked 이벤트에서 눌린 액션 이름을 꺼낸다. 버튼이 아니면 null. */
export function clickedAction(event) {
  const target = event?.event?.target
  if (!target || !target.closest) return null
  const btn = target.closest('[data-action]')
  return btn ? btn.getAttribute('data-action') : null
}

export { escapeHtml }
