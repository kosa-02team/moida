-- V19: 일정-장부 연동 강화를 위한 스키마 변경
--
-- 주요 변경사항:
-- 2. schedule_participants: 지각 합류 및 승인 상태 관리 컬럼 추가
-- 3. payment_requests: 일정 연결 및 납부 기간 관리 컬럼 추가
-- 4. schedule_participants: 결제 매칭 정보 컬럼 추가 (matched_transaction_id)

SET FOREIGN_KEY_CHECKS = 0;


-- 2. schedule_participants 테이블 컬럼 추가

-- approval_status
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedule_participants' 
      AND COLUMN_NAME = 'approval_status'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedule_participants ADD COLUMN approval_status VARCHAR(20) DEFAULT ''APPROVED'' COMMENT ''승인 상태 (PENDING, APPROVED, REJECTED)'' AFTER fee_status', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- participation_type
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedule_participants' 
      AND COLUMN_NAME = 'participation_type'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedule_participants ADD COLUMN participation_type VARCHAR(20) DEFAULT ''NORMAL'' COMMENT ''참여 유형 (NORMAL, LATE_JOIN)'' AFTER approval_status', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- fee_request_closed_at
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedule_participants' 
      AND COLUMN_NAME = 'fee_request_closed_at'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedule_participants ADD COLUMN fee_request_closed_at DATETIME NULL COMMENT ''참가비 요청 마감 시간'' AFTER participation_type', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- matched_transaction_id
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedule_participants' 
      AND COLUMN_NAME = 'matched_transaction_id'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedule_participants ADD COLUMN matched_transaction_id BIGINT NULL COMMENT ''매칭된 거래 내역 ID'' AFTER fee_status', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- 3. payment_requests 테이블 컬럼 추가

-- schedule_id
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'payment_requests' 
      AND COLUMN_NAME = 'schedule_id'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE payment_requests ADD COLUMN schedule_id BIGINT NULL COMMENT ''연관된 일정 ID'' AFTER matched_by', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- schedule_id FK
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'payment_requests' 
      AND CONSTRAINT_NAME = 'fk_payment_requests_schedule_id'
);

SET @sql = IF(@fk_exists = 0, 
    'ALTER TABLE payment_requests ADD CONSTRAINT fk_payment_requests_schedule_id FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- billing_period
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'payment_requests' 
      AND COLUMN_NAME = 'billing_period'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE payment_requests ADD COLUMN billing_period VARCHAR(20) NULL COMMENT ''납부 기간 (예: 2024-01)'' AFTER schedule_id', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. bank_transaction_history 테이블에 unmatch_reason 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bank_transaction_history' 
      AND COLUMN_NAME = 'unmatch_reason'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bank_transaction_history ADD COLUMN unmatch_reason VARCHAR(255) COMMENT ''매칭 해제 사유''', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- 5. post_images 테이블의 image_url 컬럼 길이 확장 (255 -> 2048)
SET @col_len = (
    SELECT CHARACTER_MAXIMUM_LENGTH 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'post_images' 
      AND COLUMN_NAME = 'image_url'
);

SET @sql = IF(@col_len < 2048, 
    'ALTER TABLE post_images MODIFY image_url VARCHAR(2048) NOT NULL', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
