-- ===== 사용자 정보에 전화번호, 영어 이름 추가 (둘 다 선택 입력) =====
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users ADD COLUMN english_name VARCHAR(100);
