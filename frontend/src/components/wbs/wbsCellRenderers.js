// WBS Grid용 셀 렌더러 (AG Grid Community, 순수 함수 → DOM/HTML).
import { escapeHtml } from '@/utils/grid'

export const INDENT_PX = 20

/**
 * WbsNameCellRenderer: level × 20px 들여쓰기 + 하위가 있으면 ▶(접힘)/▼(펼침) 토글.
 * 토글 클릭은 그리드 cellClicked에서 data-action="toggle"로 감지해 store.toggle(id)을 호출한다.
 * 접힘 여부는 행 데이터의 collapsed 플래그(WbsGrid가 채움)로 판단한다.
 */
export function wbsNameCellRenderer(params) {
  const item = params.data
  if (!item) return ''
  const collapsed = !!item.collapsed
  const indent = (item.level || 1) * INDENT_PX
  const toggle = item.hasChildren
    ? `<span class="wbs-toggle" data-action="toggle" data-testid="wbs-toggle-${item.id}">${collapsed ? '▶' : '▼'}</span>`
    : '<span class="wbs-toggle wbs-toggle--leaf"></span>'
  const weight = item.rolledUp ? ' style="font-weight:600"' : ''
  return `<span class="wbs-name" style="padding-left:${indent}px">${toggle}<span${weight}>${escapeHtml(params.value)}</span></span>`
}

// 지연 여부: 빨간 아이콘
export function delayedRenderer(params) {
  return params.value ? '<span class="wbs-delayed" title="지연">● 지연</span>' : ''
}
