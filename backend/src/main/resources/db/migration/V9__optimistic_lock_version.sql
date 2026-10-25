-- ===== 낙관적 잠금(동시 수정 덮어쓰기 방지)용 버전 컬럼 =====
-- 저장할 때마다 1씩 증가한다. 화면에서 불러온 버전과 DB의 현재 버전이 다르면
-- "다른 사람이 먼저 수정했다"고 판단해 저장을 막는다 (409 CONCURRENT_MODIFICATION).
ALTER TABLE projects ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE issues ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE wbs_items ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE project_assignments ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
