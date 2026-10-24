-- ===== 같은 사람을 같은 프로젝트에 다시 할당할 수 있도록 변경 =====
-- 기존: (project_id, user_id) 유니크 → 한 프로젝트에 같은 사람은 한 번만 할당 가능
-- 변경: 계획 투입 기간이 겹치지 않으면 여러 번 할당 가능 (겹침 여부는 AssignmentService에서 검사)
ALTER TABLE project_assignments DROP CONSTRAINT uk_assignments_project_user;

-- 유니크 제약이 하던 (프로젝트, 사용자) 조회용 인덱스 역할은 일반 인덱스로 유지
CREATE INDEX ix_assignments_project_user ON project_assignments (project_id, user_id);
