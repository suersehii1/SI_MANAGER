// ECharts custom series 기반 간트 차트 옵션 빌더 (대시보드 타임라인, 타임시트 Gantt, WBS Gantt 공용).
// 각 행(y)에 계획 막대(위, 연한색)와 실제 막대(아래, 진한색)를 그린다.
import { graphic } from 'echarts/core'
import dayjs from 'dayjs'

const DAY_MS = 24 * 60 * 60 * 1000

function toMs(date) {
  return dayjs(date).startOf('day').valueOf()
}

/**
 * @param categories  y축 라벨 배열 (위에서 아래 순서)
 * @param bars        [{ row, start, end, layer: 'plan'|'actual', color, name, progress? }]
 * @param markDate    세로 기준선 날짜(asOf/오늘), 선택
 */
export function buildGanttOption({ categories, bars, markDate }) {
  const data = bars
    .filter((b) => b.start && b.end)
    .map((b) => ({
      name: b.name,
      // 종료일 포함(inclusive) 표시를 위해 +1일
      value: [b.row, toMs(b.start), toMs(b.end) + DAY_MS, b.layer === 'actual' ? 1 : 0, b.progress ?? null],
      itemStyle: { color: b.color }
    }))

  function renderItem(params, api) {
    const row = api.value(0)
    const startPt = api.coord([api.value(1), row])
    const endPt = api.coord([api.value(2), row])
    const layer = api.value(3)
    const bandHeight = api.size([0, 1])[1] * 0.32
    // 계획(0)은 행 중심 위쪽, 실제(1)는 아래쪽
    const y = layer === 0 ? startPt[1] - bandHeight : startPt[1] + 1
    const rect = graphic.clipRectByRect(
      { x: startPt[0], y, width: Math.max(endPt[0] - startPt[0], 2), height: bandHeight },
      { x: params.coordSys.x, y: params.coordSys.y, width: params.coordSys.width, height: params.coordSys.height }
    )
    if (!rect) return null
    const children = [{ type: 'rect', shape: rect, style: api.style() }]
    // 진척률 오버레이 (실제 막대 위에 진척 비율만큼 어두운 띠)
    const progress = api.value(4)
    if (layer === 1 && progress != null && !Number.isNaN(progress)) {
      const pw = rect.width * Math.max(0, Math.min(100, progress)) / 100
      children.push({ type: 'rect', shape: { x: rect.x, y: rect.y + rect.height - 3, width: pw, height: 3 }, style: { fill: '#1d3f72' } })
    }
    return { type: 'group', children }
  }

  const series = [
    {
      type: 'custom',
      renderItem,
      encode: { x: [1, 2], y: 0 },
      data,
      tooltip: {
        formatter: (p) => {
          const v = p.value
          const kind = v[3] === 1 ? '실제' : '계획'
          const s = dayjs(v[1]).format('YYYY-MM-DD')
          const e = dayjs(v[2] - DAY_MS).format('YYYY-MM-DD')
          const prog = v[4] != null ? ` (${v[4]}%)` : ''
          return `${p.name}<br/>${kind}: ${s} ~ ${e}${prog}`
        }
      }
    }
  ]
  if (markDate) {
    // 기준일 세로선
    series.push({
      type: 'line',
      data: [],
      markLine: {
        symbol: 'none',
        silent: true,
        lineStyle: { color: '#f56c6c', type: 'dashed' },
        label: { formatter: dayjs(markDate).format('MM-DD') },
        data: [{ xAxis: toMs(markDate) }]
      }
    })
  }

  return {
    tooltip: { trigger: 'item' },
    grid: { left: 180, right: 30, top: 30, bottom: 50 },
    dataZoom: [{ type: 'slider', xAxisIndex: 0, height: 18, bottom: 10 }],
    xAxis: { type: 'time', axisLabel: { formatter: '{yyyy}-{MM}-{dd}' } },
    yAxis: {
      type: 'category',
      data: categories,
      inverse: true,
      axisLabel: { width: 170, overflow: 'truncate' }
    },
    series
  }
}

export const GANTT_COLORS = {
  plan: '#a0cfff',
  actual: '#409eff',
  actualDone: '#67c23a',
  delayed: '#f56c6c'
}
