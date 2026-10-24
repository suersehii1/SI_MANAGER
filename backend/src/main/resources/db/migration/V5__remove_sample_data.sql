-- ===== 화면 확인용으로 넣었던 샘플 데이터 삭제 (V2__seed_data, V4__wbs_seed) =====
-- 남기는 것: roles(ROLE_ADMIN, ROLE_USER), admin 계정 (로그인에 필요)
-- 지우는 것: 샘플 프로젝트 2개(+ 할당/이슈/첨부/WBS는 ON DELETE CASCADE로 함께 삭제), 샘플 계정 user/kim/lee
-- 이미 반영된 V2/V4 파일은 Flyway 체크섬 때문에 수정하지 않고, 이 마이그레이션으로 지운다.

-- 1) 샘플 프로젝트 (하위 할당·이슈·첨부·WBS 자동 삭제)
DELETE FROM projects WHERE code IN ('SI-2026-001', 'SI-2026-002');

-- 2) 샘플 계정
--    사용자가 직접 입력한 데이터(할당, 이슈 등록/담당, 첨부 업로드, WBS 담당)가 이 계정을 참조하고 있으면
--    지우지 않고 남겨 둔다. (사용자 데이터를 바꾸거나 지우지 않고, FK 제약으로 기동이 실패하지도 않도록)
DELETE FROM users
WHERE username IN ('user', 'kim', 'lee')
  AND NOT EXISTS (SELECT 1 FROM project_assignments pa WHERE pa.user_id = users.id)
  AND NOT EXISTS (SELECT 1 FROM issues i WHERE i.reporter_id = users.id OR i.assignee_id = users.id)
  AND NOT EXISTS (SELECT 1 FROM issue_attachments a WHERE a.uploaded_by = users.id)
  AND NOT EXISTS (SELECT 1 FROM wbs_items w WHERE w.assignee_id = users.id);
