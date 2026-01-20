-- V14: 모임(clubs) 테이블에 invite_code uuid를 저장하기에 너무 짧아서 늘림
--
-- 변경사항:
-- 1. invite_code가 UUID(36자)를 저장하기에 너무 짧습니다.

ALTER TABLE clubs MODIFY COLUMN invite_code VARCHAR(36);