-- ===== 테스트 전용 샘플 데이터 (운영 DB에는 들어가지 않음) =====
-- 운영 마이그레이션 V5에서 샘플 데이터를 지웠으므로, 통합 테스트가 쓰는 샘플 데이터를 여기서 다시 넣는다.
-- (roles, admin 계정은 운영 마이그레이션에 남아 있으므로 제외)
-- application-test.yml의 spring.flyway.locations에 classpath:db/testdata 로 등록되어 있다.

-- ===== users (BCrypt cost 10) =====
-- user1234
INSERT INTO users (username, password_hash, name, email, job_title, enabled)
VALUES ('user', '$2a$10$MuKHA9p4Q7ve.uvmYuUO4Ok57q7WfywUh.n.rIJ44/OiBHj7gb97a', '일반사용자', NULL, '사원', TRUE);
INSERT INTO users (username, password_hash, name, email, job_title, enabled)
VALUES ('kim', '$2a$10$MuKHA9p4Q7ve.uvmYuUO4Ok57q7WfywUh.n.rIJ44/OiBHj7gb97a', '김철수', 'kim@example.com', '과장', TRUE);
INSERT INTO users (username, password_hash, name, email, job_title, enabled)
VALUES ('lee', '$2a$10$MuKHA9p4Q7ve.uvmYuUO4Ok57q7WfywUh.n.rIJ44/OiBHj7gb97a', '이영희', 'lee@example.com', '대리', TRUE);

-- ===== user_roles =====
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'user' AND r.name = 'ROLE_USER';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'kim' AND r.name = 'ROLE_USER';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'lee' AND r.name = 'ROLE_USER';

-- ===== projects =====
INSERT INTO projects (code, name, client_name, description, status,
                      planned_start_date, planned_end_date, actual_start_date, actual_end_date, created_by)
SELECT 'SI-2026-001', '차세대 ERP 구축', 'A제조', NULL, 'IN_PROGRESS',
       DATE '2026-01-01', DATE '2026-03-31', DATE '2026-01-15', NULL, u.id
FROM users u WHERE u.username = 'admin';

INSERT INTO projects (code, name, client_name, description, status,
                      planned_start_date, planned_end_date, actual_start_date, actual_end_date, created_by)
SELECT 'SI-2026-002', '물류 MES 고도화', 'B물류', NULL, 'PLANNED',
       DATE '2026-03-01', DATE '2026-06-30', NULL, NULL, u.id
FROM users u WHERE u.username = 'admin';

-- ===== project_assignments =====
-- A1: SI-2026-001 / kim / PM / 1.00 / plan 01-01~03-31 / actual 01-15~03-31
INSERT INTO project_assignments (project_id, user_id, role_in_project, allocation_rate,
                                 planned_start, planned_end, actual_start, actual_end, note)
SELECT p.id, u.id, 'PM', 1.00, DATE '2026-01-01', DATE '2026-03-31', DATE '2026-01-15', DATE '2026-03-31', NULL
FROM projects p, users u WHERE p.code = 'SI-2026-001' AND u.username = 'kim';

-- A2: SI-2026-001 / lee / DEVELOPER / 0.50 / plan 02-01~03-31 / actual 02-01~(null)
INSERT INTO project_assignments (project_id, user_id, role_in_project, allocation_rate,
                                 planned_start, planned_end, actual_start, actual_end, note)
SELECT p.id, u.id, 'DEVELOPER', 0.50, DATE '2026-02-01', DATE '2026-03-31', DATE '2026-02-01', NULL, NULL
FROM projects p, users u WHERE p.code = 'SI-2026-001' AND u.username = 'lee';

-- A3: SI-2026-002 / kim / ARCHITECT / 0.50 / plan 03-16~04-30 / actual (null)
INSERT INTO project_assignments (project_id, user_id, role_in_project, allocation_rate,
                                 planned_start, planned_end, actual_start, actual_end, note)
SELECT p.id, u.id, 'ARCHITECT', 0.50, DATE '2026-03-16', DATE '2026-04-30', NULL, NULL, NULL
FROM projects p, users u WHERE p.code = 'SI-2026-002' AND u.username = 'kim';

-- ===== issues (SI-2026-001, reporter = admin) =====
INSERT INTO issues (project_id, title, description, status, status_rank, priority, priority_rank,
                    progress, assignee_id, reporter_id, due_date)
SELECT p.id, '로그인 화면 오류', '로그인 버튼 클릭 시 간헐적으로 오류가 발생합니다.', 'OPEN', 1, 'HIGH', 3,
       0, a.id, r.id, DATE '2026-03-20'
FROM projects p, users a, users r
WHERE p.code = 'SI-2026-001' AND a.username = 'kim' AND r.username = 'admin';

INSERT INTO issues (project_id, title, description, status, status_rank, priority, priority_rank,
                    progress, assignee_id, reporter_id, due_date)
SELECT p.id, '주문 API 성능 개선', '주문 조회 API 응답 시간이 3초 이상 소요됩니다.', 'IN_PROGRESS', 2, 'MEDIUM', 2,
       40, a.id, r.id, NULL
FROM projects p, users a, users r
WHERE p.code = 'SI-2026-001' AND a.username = 'lee' AND r.username = 'admin';

INSERT INTO issues (project_id, title, description, status, status_rank, priority, priority_rank,
                    progress, assignee_id, reporter_id, due_date)
SELECT p.id, '배포 스크립트 정리', '배포 스크립트를 정리하고 문서화합니다.', 'RESOLVED', 3, 'LOW', 1,
       100, NULL, r.id, NULL
FROM projects p, users r
WHERE p.code = 'SI-2026-001' AND r.username = 'admin';

-- ===== WBS seed for SI-2026-001 (specs-wbs.md W3.1, asOf 2026-03-15 -> overall 53.8) =====
-- Assignees: 1.x/2.x = kim (first user assigned to SI-2026-001), 3.x/4 = lee (second user).
-- Parent rows store the same values the roll-up produces; responses always expose the rolled-up values.

-- Roots
INSERT INTO wbs_items (project_id, parent_id, sort_order, name, deliverable, assignee_id, status, weight,
                       planned_start, planned_end, actual_start, actual_end, progress)
SELECT p.id, NULL, 0, '분석', '요구사항 정의서', u.id, 'DONE', 1.00,
       DATE '2026-01-01', DATE '2026-01-31', DATE '2026-01-15', DATE '2026-01-31', 100
FROM projects p LEFT JOIN users u ON u.username = 'kim' WHERE p.code = 'SI-2026-001';

INSERT INTO wbs_items (project_id, parent_id, sort_order, name, deliverable, assignee_id, status, weight,
                       planned_start, planned_end, actual_start, actual_end, progress)
SELECT p.id, NULL, 1, '설계', '설계서', u.id, 'IN_PROGRESS', 1.00,
       DATE '2026-02-01', DATE '2026-02-28', DATE '2026-02-01', NULL, 90
FROM projects p LEFT JOIN users u ON u.username = 'kim' WHERE p.code = 'SI-2026-001';

INSERT INTO wbs_items (project_id, parent_id, sort_order, name, deliverable, assignee_id, status, weight,
                       planned_start, planned_end, actual_start, actual_end, progress)
SELECT p.id, NULL, 2, '개발', '소스코드', u.id, 'IN_PROGRESS', 1.00,
       DATE '2026-03-01', DATE '2026-03-25', DATE '2026-03-02', NULL, 25
FROM projects p LEFT JOIN users u ON u.username = 'lee' WHERE p.code = 'SI-2026-001';

INSERT INTO wbs_items (project_id, parent_id, sort_order, name, deliverable, assignee_id, status, weight,
                       planned_start, planned_end, actual_start, actual_end, progress)
SELECT p.id, NULL, 3, '테스트 및 이행', '테스트 결과서', u.id, 'NOT_STARTED', 1.00,
       DATE '2026-03-26', DATE '2026-03-31', NULL, NULL, 0
FROM projects p LEFT JOIN users u ON u.username = 'lee' WHERE p.code = 'SI-2026-001';

-- 1.x (분석)
INSERT INTO wbs_items (project_id, parent_id, sort_order, name, deliverable, assignee_id, status, weight,
                       planned_start, planned_end, actual_start, actual_end, progress)
SELECT p.id,
       (SELECT w.id FROM wbs_items w WHERE w.project_id = p.id AND w.name = '분석' AND w.parent_id IS NULL),
       0, '요구사항 정의', '요구사항 정의서', u.id, 'DONE', 1.00,
       DATE '2026-01-01', DATE '2026-01-15', DATE '2026-01-15', DATE '2026-01-20', 100
FROM projects p LEFT JOIN users u ON u.username = 'kim' WHERE p.code = 'SI-2026-001';

INSERT INTO wbs_items (project_id, parent_id, sort_order, name, deliverable, assignee_id, status, weight,
                       planned_start, planned_end, actual_start, actual_end, progress)
SELECT p.id,
       (SELECT w.id FROM wbs_items w WHERE w.project_id = p.id AND w.name = '분석' AND w.parent_id IS NULL),
       1, '현행 시스템 분석', '현행 분석서', u.id, 'DONE', 1.00,
       DATE '2026-01-16', DATE '2026-01-31', DATE '2026-01-21', DATE '2026-01-31', 100
FROM projects p LEFT JOIN users u ON u.username = 'kim' WHERE p.code = 'SI-2026-001';

-- 2.x (설계)
INSERT INTO wbs_items (project_id, parent_id, sort_order, name, deliverable, assignee_id, status, weight,
                       planned_start, planned_end, actual_start, actual_end, progress)
SELECT p.id,
       (SELECT w.id FROM wbs_items w WHERE w.project_id = p.id AND w.name = '설계' AND w.parent_id IS NULL),
       0, '화면 설계', '화면 설계서', u.id, 'DONE', 1.00,
       DATE '2026-02-01', DATE '2026-02-14', DATE '2026-02-01', DATE '2026-02-16', 100
FROM projects p LEFT JOIN users u ON u.username = 'kim' WHERE p.code = 'SI-2026-001';

INSERT INTO wbs_items (project_id, parent_id, sort_order, name, deliverable, assignee_id, status, weight,
                       planned_start, planned_end, actual_start, actual_end, progress)
SELECT p.id,
       (SELECT w.id FROM wbs_items w WHERE w.project_id = p.id AND w.name = '설계' AND w.parent_id IS NULL),
       1, 'DB 설계', 'ERD, 테이블 정의서', u.id, 'IN_PROGRESS', 1.00,
       DATE '2026-02-01', DATE '2026-02-28', DATE '2026-02-01', NULL, 80
FROM projects p LEFT JOIN users u ON u.username = 'kim' WHERE p.code = 'SI-2026-001';

-- 3.x (개발)
INSERT INTO wbs_items (project_id, parent_id, sort_order, name, deliverable, assignee_id, status, weight,
                       planned_start, planned_end, actual_start, actual_end, progress)
SELECT p.id,
       (SELECT w.id FROM wbs_items w WHERE w.project_id = p.id AND w.name = '개발' AND w.parent_id IS NULL),
       0, '공통 모듈 개발', '공통 모듈', u.id, 'IN_PROGRESS', 1.00,
       DATE '2026-03-01', DATE '2026-03-15', DATE '2026-03-02', NULL, 50
FROM projects p LEFT JOIN users u ON u.username = 'lee' WHERE p.code = 'SI-2026-001';

INSERT INTO wbs_items (project_id, parent_id, sort_order, name, deliverable, assignee_id, status, weight,
                       planned_start, planned_end, actual_start, actual_end, progress)
SELECT p.id,
       (SELECT w.id FROM wbs_items w WHERE w.project_id = p.id AND w.name = '개발' AND w.parent_id IS NULL),
       1, '업무 화면 개발', '업무 화면', u.id, 'NOT_STARTED', 1.00,
       DATE '2026-03-10', DATE '2026-03-25', NULL, NULL, 0
FROM projects p LEFT JOIN users u ON u.username = 'lee' WHERE p.code = 'SI-2026-001';
