-- V20: 일정 정산을 위한 스냅샷 필드 추가
--
-- 주요 변경사항:
-- 1. schedules 테이블에 참석 마감 시점의 트랜잭션 ID와 마감 시각 추가

SET FOREIGN_KEY_CHECKS = 0;

-- snapshot_transaction_id 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'schedules'
      AND COLUMN_NAME = 'snapshot_transaction_id'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE schedules ADD COLUMN snapshot_transaction_id BIGINT NULL COMMENT ''참석 마감 시점의 마지막 트랜잭션 ID'' AFTER vote_deadline',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- attendance_closed_at 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'schedules'
      AND COLUMN_NAME = 'attendance_closed_at'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE schedules ADD COLUMN attendance_closed_at DATETIME NULL COMMENT ''참석 실제 마감 시각'' AFTER snapshot_transaction_id',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TransactionLog에 BankTransactionHistory ID 연결 컬럼 추가
ALTER TABLE transaction_log
ADD COLUMN bank_history_id BIGINT COMMENT 'BankTransactionHistory와의 연결 ID';

-- 인덱스 추가 (조회 성능 향상)
CREATE INDEX idx_transaction_log_bank_history_id ON transaction_log(bank_history_id);

SET FOREIGN_KEY_CHECKS = 1;
