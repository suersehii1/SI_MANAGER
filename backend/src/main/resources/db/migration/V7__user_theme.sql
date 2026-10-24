-- ===== 사용자별 화면 스킨(낮/밤) =====
-- LIGHT = 낮(해), DARK = 밤(달). 로그인할 때마다 저장된 스킨을 다시 적용하기 위해 사용자 정보에 보관한다.
ALTER TABLE users ADD COLUMN theme VARCHAR(10) DEFAULT 'LIGHT' NOT NULL;
ALTER TABLE users ADD CONSTRAINT ck_users_theme CHECK (theme IN ('LIGHT', 'DARK'));
