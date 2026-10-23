// WBS 셀 렌더러(들여쓰기/토글)와 공통 그리드 헬퍼(액션 버튼, XSS 이스케이프)
import { describe, it, expect } from 'vitest'
import { wbsNameCellRenderer, delayedRenderer, INDENT_PX } from '@/components/wbs/wbsCellRenderers'
import { actionsRenderer, clickedAction, progressRenderer } from '@/utils/grid'

function render(html) {
  const div = document.createElement('div')
  div.innerHTML = html
  return div
}

describe('wbsNameCellRenderer', () => {
  it('level × 20px 들여쓰기', () => {
    expect(INDENT_PX).toBe(20)
    for (const level of [1, 2, 3, 5]) {
      const el = render(wbsNameCellRenderer({ value: 'A', data: { id: 1, level, hasChildren: false } }))
      expect(el.querySelector('.wbs-name').style.paddingLeft).toBe(`${level * 20}px`)
    }
  })

  it('하위가 있으면 ▼(펼침) / ▶(접힘) 토글, 리프는 토글 없음', () => {
    const open = render(wbsNameCellRenderer({ value: '설계', data: { id: 2, level: 1, hasChildren: true, collapsed: false } }))
    expect(open.querySelector('[data-testid="wbs-toggle-2"]').textContent).toBe('▼')
    expect(open.querySelector('[data-action="toggle"]')).not.toBeNull()

    const closed = render(wbsNameCellRenderer({ value: '설계', data: { id: 2, level: 1, hasChildren: true, collapsed: true } }))
    expect(closed.querySelector('[data-testid="wbs-toggle-2"]').textContent).toBe('▶')

    const leaf = render(wbsNameCellRenderer({ value: 'DB 설계', data: { id: 22, level: 2, hasChildren: false } }))
    expect(leaf.querySelector('[data-action="toggle"]')).toBeNull()
  })

  it('작업명은 HTML 이스케이프 (XSS 방지)', () => {
    const el = render(wbsNameCellRenderer({ value: '<img src=x onerror=alert(1)>', data: { id: 1, level: 1 } }))
    expect(el.querySelector('img')).toBeNull()
    expect(el.textContent).toContain('<img src=x onerror=alert(1)>')
  })

  it('집계 행은 굵게, 지연은 표시', () => {
    expect(wbsNameCellRenderer({ value: 'A', data: { id: 1, level: 1, rolledUp: true } })).toContain('font-weight:600')
    expect(delayedRenderer({ value: true })).toContain('지연')
    expect(delayedRenderer({ value: false })).toBe('')
  })
})

describe('grid helpers', () => {
  it('actionsRenderer: data-testid/data-action, visible 조건', () => {
    const renderer = actionsRenderer([
      { action: 'edit', label: '수정', testid: 'issue-edit' },
      { action: 'delete', label: '삭제', testid: 'issue-delete', visible: (row) => row.mine }
    ])
    const mine = render(renderer({ data: { id: 7, mine: true } }))
    expect(mine.querySelector('[data-testid="issue-edit-7"]').getAttribute('data-action')).toBe('edit')
    expect(mine.querySelector('[data-testid="issue-delete-7"]')).not.toBeNull()
    const others = render(renderer({ data: { id: 8, mine: false } }))
    expect(others.querySelector('[data-testid="issue-delete-8"]')).toBeNull()
  })

  it('clickedAction: 버튼 안쪽 클릭이면 액션명, 아니면 null', () => {
    const el = render('<button data-action="edit"><span>수정</span></button><span class="other"></span>')
    expect(clickedAction({ event: { target: el.querySelector('button span') } })).toBe('edit')
    expect(clickedAction({ event: { target: el.querySelector('.other') } })).toBeNull()
    expect(clickedAction({})).toBeNull()
  })

  it('progressRenderer: 0..100 범위로 막대, 소수 1자리', () => {
    expect(progressRenderer({ value: 53.8 })).toContain('53.8%')
    expect(progressRenderer({ value: 40 })).toContain('40%')
    expect(progressRenderer({ value: 140 })).toContain('width:100%')
  })
})
