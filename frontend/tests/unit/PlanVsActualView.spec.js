// 계획 vs 실제: 조회 조건 영역은 항상 표시, ▲(차트 접기) / ▼(표 접기)로 차트·표 영역 전환
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia } from 'pinia'
import { mountWith, settle, $, click } from './helpers'
import PlanVsActualView from '@/views/admin/PlanVsActualView.vue'

vi.mock('@/api/admin', () => ({
  getPlanVsActual: vi.fn().mockResolvedValue({
    from: '2026-05', to: '2026-06', asOf: '2026-10-24',
    summary: { totalPlannedMM: 2, plannedMMToDate: 2, actualMMToDate: 1.5, scheduleVarianceMM: -0.5, plannedDurationDays: 61, actualDurationDays: 50 },
    series: [
      { month: '2026-05', plannedMM: 1, actualMM: 1, cumulativePlannedMM: 1, cumulativeActualMM: 1 },
      { month: '2026-06', plannedMM: 1, actualMM: 0.5, cumulativePlannedMM: 2, cumulativeActualMM: 1.5 }
    ]
  })
}))
vi.mock('@/stores/lookup', () => ({ useLookupStore: () => ({ projects: [], loadProjects: () => Promise.resolve() }) }))
// 차트/표 컴포넌트는 렌더링 부담이 커서 이름만 남긴 대체 컴포넌트로 바꾼다
vi.mock('@/components/admin/PlanActualChart.vue', () => ({ default: { name: 'PlanActualChart', template: '<div class="stub-chart" />' } }))
vi.mock('@/components/admin/PlanActualTable.vue', () => ({ default: { name: 'PlanActualTable', template: '<div class="stub-table" />' } }))

// v-show로 숨겨진 요소는 style.display = 'none'
const shown = (selector) => $(selector) && $(selector).style.display !== 'none'

describe('PlanVsActualView 영역 접기', () => {
  let wrapper
  beforeEach(async () => {
    wrapper = mountWith(PlanVsActualView, { pinia: createPinia() })
    await settle()
  })
  afterEach(() => {
    wrapper.unmount()
    document.body.innerHTML = ''
  })

  it('처음에는 조회 조건, 차트, 표가 모두 보인다', () => {
    expect(shown('[data-testid="plan-actual-filter"]')).toBe(true)
    expect(shown('.pva__chart-body')).toBe(true)
    expect(shown('[data-testid="plan-actual-table-pane"]')).toBe(true)
  })

  it('▲: 차트를 접어도 조회 조건은 보이고 표가 남는다, ▼로 다시 원래대로', async () => {
    await click('[data-testid="pva-collapse-up"]')
    expect(shown('[data-testid="plan-actual-filter"]')).toBe(true)
    expect(shown('.pva__chart-body')).toBe(false)
    expect(shown('[data-testid="plan-actual-table-pane"]')).toBe(true)
    expect($('[data-testid="pva-collapse-up"]').disabled).toBe(true)

    await click('[data-testid="pva-collapse-down"]')
    expect(shown('.pva__chart-body')).toBe(true)
    expect(shown('[data-testid="plan-actual-table-pane"]')).toBe(true)
  })

  it('▼: 표를 접으면 차트만 남는다, ▲로 다시 원래대로', async () => {
    await click('[data-testid="pva-collapse-down"]')
    expect(shown('[data-testid="plan-actual-filter"]')).toBe(true)
    expect(shown('.pva__chart-body')).toBe(true)
    expect(shown('[data-testid="plan-actual-table-pane"]')).toBe(false)
    expect($('[data-testid="pva-collapse-down"]').disabled).toBe(true)

    await click('[data-testid="pva-collapse-up"]')
    expect(shown('.pva__chart-body')).toBe(true)
    expect(shown('[data-testid="plan-actual-table-pane"]')).toBe(true)
  })
})
