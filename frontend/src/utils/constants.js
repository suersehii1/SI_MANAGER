// enum 값 ↔ 한국어 라벨 / 태그 색상

export const PROJECT_STATUS = {
  PLANNED: { label: '계획', type: 'info' },
  IN_PROGRESS: { label: '진행 중', type: 'primary' },
  ON_HOLD: { label: '보류', type: 'warning' },
  COMPLETED: { label: '완료', type: 'success' }
}

export const ISSUE_STATUS = {
  OPEN: { label: '열림', type: 'info' },
  IN_PROGRESS: { label: '진행 중', type: 'primary' },
  RESOLVED: { label: '해결', type: 'success' },
  CLOSED: { label: '종료', type: '' }
}

export const ISSUE_PRIORITY = {
  LOW: { label: '낮음', type: 'info' },
  MEDIUM: { label: '보통', type: '' },
  HIGH: { label: '높음', type: 'warning' },
  CRITICAL: { label: '긴급', type: 'danger' }
}

export const ASSIGNMENT_STATE = {
  PLANNED: { label: '투입 예정', type: 'info' },
  ACTIVE: { label: '투입 중', type: 'success' },
  RELEASED: { label: '철수', type: '' }
}

export const WBS_STATUS = {
  NOT_STARTED: { label: '시작 전', type: 'info' },
  IN_PROGRESS: { label: '진행 중', type: 'primary' },
  DONE: { label: '완료', type: 'success' },
  ON_HOLD: { label: '보류', type: 'warning' }
}

export const ROLES = ['ROLE_ADMIN', 'ROLE_USER']

export function toOptions(map) {
  return Object.keys(map).map((value) => ({ value, label: map[value].label }))
}
