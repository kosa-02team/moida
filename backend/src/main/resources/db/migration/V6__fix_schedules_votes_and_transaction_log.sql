-- V6: schedules 테이블 club_id NULL 레코드 처리, votes 테이블 post_id nullable 멱등성 개선, TransactionLog에 schedule_id 추가, schedules에 cancel_reason 추가
-- 
-- 주요 변경사항:
-- 1. schedules 테이블: club_id가 NULL인 레코드 삭제 후 NOT NULL 제약조건 강제
-- 2. votes 테이블: post_id nullable 변경 시 멱등성 보장
-- 3. transaction_log 테이블: schedule_id 컬럼 추가 (일정별 지출 추적용)
-- 4. schedules 테이블: cancel_reason 컬럼 추가 (일정 취소 사유)
-- 
-- 문제점:
-- - V3에서 club_id가 NULL인 레코드가 남아있으면 엔티티(@Column nullable = false)와 DB 스키마 불일치 발생
-- - V3에서 post_id nullable 변경 시 멱등성 보장 부족 (재실행 시 실패 가능)
-- - TransactionLog에 schedule_id가 없어 일정별 지출 추적 불가능

SET FOREIGN_KEY_CHECKS = 0;

-- 1. schedules 테이블의 club_id NULL 레코드 처리
-- NULL인 일정은 모임에 속하지 않으므로 삭제 처리 (데이터 정합성 유지)
DELETE FROM schedules WHERE club_id IS NULL;

-- club_id를 NOT NULL로 변경 (NULL 레코드가 없음을 보장한 후)
SET @null_count = (
    SELECT COUNT(*) 
    FROM schedules 
    WHERE club_id IS NULL
);

SET @sql = IF(@null_count = 0, 
    'ALTER TABLE schedules MODIFY COLUMN club_id BIGINT NOT NULL', 
    CONCAT('SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Cannot convert club_id to NOT NULL: ', @null_count, ' NULL records still exist'''));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. votes 테이블의 post_id nullable 변경 멱등성 보장
-- post_id 컬럼의 현재 nullable 상태 확인
SET @is_nullable = (
    SELECT IS_NULLABLE 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'votes' 
      AND COLUMN_NAME = 'post_id'
);

-- nullable이 아니면 변경, 이미 nullable이면 스킵 (멱등성 보장)
SET @sql = IF(@is_nullable = 'NO', 
    'ALTER TABLE votes MODIFY COLUMN post_id BIGINT NULL COMMENT ''연결된 게시글 (GENERAL 타입일 때 사용, ATTENDANCE 타입은 NULL 가능)''', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. transaction_log 테이블에 schedule_id 컬럼 추가 (일정별 지출 추적용)
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'transaction_log' 
      AND COLUMN_NAME = 'schedule_id'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE transaction_log ADD COLUMN schedule_id BIGINT NULL COMMENT ''연결된 일정 ID (일정 관련 지출/환불 추적용)'' AFTER club_id', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- schedule_id에 FK 추가 (nullable이므로 FK 가능)
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'transaction_log' 
      AND CONSTRAINT_NAME = 'fk_transaction_log_schedule_id'
);

SET @sql = IF(@fk_exists = 0, 
    'ALTER TABLE transaction_log ADD CONSTRAINT fk_transaction_log_schedule_id FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. schedules 테이블에 cancel_reason 컬럼 추가 (일정 취소 사유)
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND COLUMN_NAME = 'cancel_reason'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedules ADD COLUMN cancel_reason TEXT COMMENT ''일정 취소 사유'' AFTER closed_at', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. schedules 테이블에 vote_deadline 컬럼 추가 (투표 종료 시간)
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND COLUMN_NAME = 'vote_deadline'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedules ADD COLUMN vote_deadline DATETIME NULL COMMENT ''투표 종료 시간 (투표 마감일)'' AFTER cancel_reason', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
