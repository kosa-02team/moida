-- V9__add_club_type_and_fee_rule.sql

-- 1. 모임 유형(type) 컬럼 추가 (기본값: FAIR_SETTLEMENT)
ALTER TABLE clubs
ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'FAIR_SETTLEMENT';

-- 2. 회비 규칙(fee_rule) 컬럼 추가
ALTER TABLE clubs
ADD COLUMN fee_rule VARCHAR(255);

-- 1. 청구 기간(billing_period) 컬럼 추가 (운영비형 모임용)
ALTER TABLE payment_requests
ADD COLUMN billing_period VARCHAR(20);

-- 2. 일정 ID(schedule_id) 컬럼 추가 (공정정산형 모임용)
ALTER TABLE payment_requests
ADD COLUMN schedule_id BIGINT;

-- (선택 사항) schedule_id가 schedules 테이블의 post_id를 참조하도록 외래키 추가
-- ALTER TABLE payment_requests
-- ADD CONSTRAINT fk_payment_request_schedule
-- FOREIGN KEY (schedule_id) REFERENCES schedules(post_id);