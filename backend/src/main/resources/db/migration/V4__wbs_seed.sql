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
