-- V8: 제약조건 추가 및 FK 개선
-- 
-- 주요 변경사항:
-- 1. votes 테이블: schedule_id에 UNIQUE 제약조건 추가 (하나의 일정에는 하나의 투표만 허용)
-- 2. transaction_log 테이블: FK에 ON DELETE SET NULL 추가 (V6에서 누락된 부분 보완)
-- 
-- V6의 문제점 보완:
-- - V6의 FK 제약조건에 ON DELETE 동작이 누락되어 있어 V8에서 보완
-- - V6의 DELETE 작업은 이미 실행되었을 가능성이 높아 V8에서 추가 처리 불필요

SET FOREIGN_KEY_CHECKS = 0;

-- 1. votes 테이블의 schedule_id에 UNIQUE 제약조건 추가
-- 하나의 일정에는 하나의 ATTENDANCE 투표만 허용 (1:1 관계 강제)
-- schedule_id가 NULL인 경우는 제외 (GENERAL 타입 투표는 schedule_id가 NULL)

-- 먼저 중복된 schedule_id가 있는지 확인
SET @duplicate_count = (
    SELECT COUNT(*) 
    FROM (
        SELECT schedule_id, COUNT(*) as cnt
        FROM votes
        WHERE schedule_id IS NOT NULL
        GROUP BY schedule_id
        HAVING cnt > 1
    ) AS duplicates
);

-- 중복이 있으면 에러 발생 (데이터 정합성 문제)
SET @sql = IF(@duplicate_count > 0, 
    CONCAT('SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Cannot add UNIQUE constraint: ', @duplicate_count, ' duplicate schedule_id found in votes table. Please clean up duplicate data first.'''),
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- UNIQUE 제약조건이 이미 존재하는지 확인
SET @uk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'votes' 
      AND CONSTRAINT_NAME = 'uk_votes_schedule_id'
      AND CONSTRAINT_TYPE = 'UNIQUE'
);

-- UNIQUE 제약조건 추가 (중복이 없고 제약조건이 없을 때만)
SET @sql = IF(@duplicate_count = 0 AND @uk_exists = 0, 
    'ALTER TABLE votes ADD CONSTRAINT uk_votes_schedule_id UNIQUE (schedule_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. transaction_log 테이블의 FK에 ON DELETE SET NULL 추가
-- 일정 삭제 시 transaction_log의 schedule_id가 자동으로 NULL로 설정됨

-- 기존 FK 제약조건이 존재하는지 확인
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'transaction_log' 
      AND CONSTRAINT_NAME = 'fk_transaction_log_schedule_id'
);

-- 기존 FK 제약조건 삭제 (재생성 위해)
SET @sql = IF(@fk_exists > 0, 
    'ALTER TABLE transaction_log DROP FOREIGN KEY fk_transaction_log_schedule_id', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ON DELETE SET NULL 포함하여 FK 제약조건 재생성
SET @sql = IF(@fk_exists > 0, 
    'ALTER TABLE transaction_log ADD CONSTRAINT fk_transaction_log_schedule_id FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id) ON DELETE SET NULL', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
