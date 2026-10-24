// 화면 스킨(낮/밤) 적용 유틸.
// - Element Plus 다크 모드는 <html class="dark"> 로 켜진다 (main.js에서 dark/css-vars.css 로드)
// - AG Grid / 공통 스타일은 styles.css의 html.dark 규칙이 따라간다
// - ECharts는 App.vue에서 THEME_KEY로 아래 CHART_DARK_THEME 이름을 넘겨준다
import { registerTheme } from 'echarts/core'

export const THEMES = {
  LIGHT: { label: '낮', icon: 'Sunny' },
  DARK: { label: '밤', icon: 'Moon' }
}

export const CHART_DARK_THEME = 'sipm-dark'

/** LIGHT/DARK 이외 값(null 포함)은 LIGHT로 본다 */
export function normalizeTheme(theme) {
  return theme === 'DARK' ? 'DARK' : 'LIGHT'
}

/** 문서 전체에 스킨을 적용한다 */
export function applyTheme(theme) {
  document.documentElement.classList.toggle('dark', normalizeTheme(theme) === 'DARK')
}

// 밤 스킨용 차트 테마: 배경은 투명(카드 배경색을 그대로 사용), 글자/축/격자선만 어두운 배경에 맞춘다
const text = '#cfd3dc'
const axisLine = '#4c4d4f'
const splitLine = '#363637'
const axis = {
  axisLine: { lineStyle: { color: axisLine } },
  axisTick: { lineStyle: { color: axisLine } },
  axisLabel: { color: text },
  splitLine: { lineStyle: { color: splitLine } }
}
registerTheme(CHART_DARK_THEME, {
  backgroundColor: 'transparent',
  textStyle: { color: text },
  title: { textStyle: { color: '#e5eaf3' } },
  legend: { textStyle: { color: text } },
  tooltip: { backgroundColor: '#1d1e1f', borderColor: axisLine, textStyle: { color: '#e5eaf3' } },
  categoryAxis: axis,
  valueAxis: axis,
  timeAxis: axis,
  dataZoom: { textStyle: { color: text }, borderColor: axisLine }
})
