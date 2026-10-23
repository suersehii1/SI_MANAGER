import dayjs from 'dayjs'

// MM 수치는 서버가 이미 소수 2자리로 반올림해 준다. 표시만 2자리로 맞춘다.
export function formatMM(value) {
  if (value === null || value === undefined || value === '') return '-'
  return Number(value).toFixed(2)
}

export function formatDate(value) {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD')
}

export function formatDateTime(value) {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD HH:mm')
}

export function formatPeriod(start, end) {
  if (!start && !end) return '-'
  return `${start || '?'} ~ ${end || '진행 중'}`
}

export function today() {
  return dayjs().format('YYYY-MM-DD')
}
