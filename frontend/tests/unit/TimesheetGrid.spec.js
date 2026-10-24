// 월별 타임시트: 0.00이 아닌 월 값은 파란색(mm-nonzero), 과다 할당 월은 빨간색(mm-over) 우선
import { describe, it, expect, afterEach } from 'vitest'
import { mountWith, settle } from './helpers'
import TimesheetGrid from '@/components/admin/TimesheetGrid.vue'

const data = {
  months: ['2026-01', '2026-02', '2026-03'],
  rows: [
    {
      userId: 3, name: '김철수', username: 'kim', total: 2.3,
      overAllocatedMonths: ['2026-03'],
      cells: [{ month: '2026-01', mm: 0 }, { month: '2026-02', mm: 0.8 }, { month: '2026-03', mm: 1.5 }],
      projects: [
        {
          assignmentId: 11, projectCode: 'SI-1', projectName: 'A', roleInProject: '개발자', allocationRate: 1, total: 2.3,
          cells: [{ month: '2026-01', mm: 0 }, { month: '2026-02', mm: 0.8 }, { month: '2026-03', mm: 1.5 }]
        }
      ]
    }
  ],
  monthTotals: [{ month: '2026-01', mm: 0 }, { month: '2026-02', mm: 0.8 }, { month: '2026-03', mm: 1.5 }],
  grandTotal: 2.3
}

// row-id와 월(col-id)로 셀 요소를 찾는다 (하단 고정 합계 행은 row-id 'total')
function cell(rowId, month) {
  return document.body.querySelector(`.ag-row[row-id="${rowId}"] .ag-cell[col-id="${month}"]`)
}

describe('TimesheetGrid 월별 값 색상', () => {
  let wrapper
  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ''
  })

  it('0.00 → 색 없음, 0이 아닌 값 → 파란색, 과다 할당 월(사용자 행) → 빨간색', async () => {
    wrapper = mountWith(TimesheetGrid, { props: { data } })
    await settle(10)
    await new Promise((r) => setTimeout(r, 200))
    await settle()

    // 사용자 행
    expect(cell('u-3', '2026-01').classList.contains('mm-nonzero')).toBe(false)
    expect(cell('u-3', '2026-02').classList.contains('mm-nonzero')).toBe(true)
    expect(cell('u-3', '2026-03').classList.contains('mm-over')).toBe(true)
    expect(cell('u-3', '2026-03').classList.contains('mm-nonzero')).toBe(false)

    // 프로젝트 행: 과다 할당 표시는 없고 0이 아니면 파란색
    expect(cell('a-11', '2026-01').classList.contains('mm-nonzero')).toBe(false)
    expect(cell('a-11', '2026-02').classList.contains('mm-nonzero')).toBe(true)
    expect(cell('a-11', '2026-03').classList.contains('mm-nonzero')).toBe(true)

    // 하단 월 총합 행
    expect(cell('total', '2026-01').classList.contains('mm-nonzero')).toBe(false)
    expect(cell('total', '2026-02').classList.contains('mm-nonzero')).toBe(true)
  })
})
