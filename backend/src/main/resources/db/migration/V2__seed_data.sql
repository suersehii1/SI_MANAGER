-- ===== roles =====
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');
INSERT INTO roles (name) VALUES ('ROLE_USER');

-- ===== users (BCrypt cost 10) =====
-- admin1234
INSERT INTO users (username, password_hash, name, email, job_title, enabled)
VALUES ('admin', '$2a$10$Dak9d7683Dss68Ylb/bGHus.HBLbIV.OBWHmVqeGDIItvxBmLVnF.', '관리자', NULL, 'PMO', TRUE);
-- user1234
INSERT INTO users (username, password_hash, name, email, job_title, enabled)
VALUES ('user', '$2a$10$MuKHA9p4Q7ve.uvmYuUO4Ok57q7WfywUh.n.rIJ44/OiBHj7gb97a', '일반사용자', NULL, '사원', TRUE);
INSERT INTO users (username, password_hash, name, email, job_title, enabled)
VALUES ('kim', '$2a$10$MuKHA9p4Q7ve.uvmYuUO4Ok57q7WfywUh.n.rIJ44/OiBHj7gb97a', '김철수', 'kim@example.com', '과장', TRUE);
INSERT INTO users (username, password_hash, name, email, job_title, enabled)
VALUES ('lee', '$2a$10$MuKHA9p4Q7ve.uvmYuUO4Ok57q7WfywUh.n.rIJ44/OiBHj7gb97a', '이영희', 'lee@example.com', '대리', TRUE);

-- ===== user_roles =====
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ROLE_USER';
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
