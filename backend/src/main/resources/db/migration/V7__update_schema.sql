-- V7 Migration
-- 목적
-- 1 bank_accounts에 club_id, bank_code 추가
-- 2 bank_accounts.bank_id NULL 데이터 정리 후 NOT NULL 강제
-- 3 bank_accounts.bank_name 제거 (banks 조인으로 대체)
-- 4 payment_requests 테이블 생성 (입금요청 기반 매칭)
-- 5 bank_transaction_history 스키마 정리 및 TransactionLog 연결
--
-- 실행 전 체크
-- 1 banks에 STUB 레코드가 없으면 먼저 생성
--   INSERT INTO banks(bank_code, bank_name) VALUES ('STUB','스텁은행');
-- 2 transaction_log PK는 transaction_id 사용

SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- A. bank_accounts 스키마 확장
-- =========================================================

-- A-1. club_id 컬럼 추가 (없으면)
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_accounts'
     AND COLUMN_NAME = 'club_id') = 0,
  'ALTER TABLE bank_accounts ADD COLUMN club_id BIGINT NULL AFTER account_id',
  'SELECT "club_id already exists" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- A-2. bank_code 컬럼 추가 (없으면)
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_accounts'
     AND COLUMN_NAME = 'bank_code') = 0,
  'ALTER TABLE bank_accounts ADD COLUMN bank_code VARCHAR(20) NULL AFTER club_id',
  'SELECT "bank_code already exists" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- A-3. club_id FK 추가 (없으면)
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_accounts'
     AND CONSTRAINT_NAME = 'fk_bank_accounts_club_id') = 0,
  'ALTER TABLE bank_accounts
     ADD CONSTRAINT fk_bank_accounts_club_id
     FOREIGN KEY (club_id) REFERENCES clubs(club_id)',
  'SELECT "fk_bank_accounts_club_id already exists" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- A-4. club_id 인덱스 (없으면)
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_accounts'
     AND INDEX_NAME = 'idx_bank_accounts_club_id') = 0,
  'CREATE INDEX idx_bank_accounts_club_id ON bank_accounts(club_id)',
  'SELECT "idx_bank_accounts_club_id already exists" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- A-5. bank_code 인덱스 (없으면)
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_accounts'
     AND INDEX_NAME = 'idx_bank_accounts_bank_code') = 0,
  'CREATE INDEX idx_bank_accounts_bank_code ON bank_accounts(bank_code)',
  'SELECT "idx_bank_accounts_bank_code already exists" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- =========================================================
-- B. payment_requests 생성 (입금요청 기반 매칭)
-- =========================================================

CREATE TABLE IF NOT EXISTS payment_requests (
  request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  club_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  member_name VARCHAR(50) NOT NULL COMMENT '회원 이름 (매칭 기준)',
  request_type VARCHAR(20) NOT NULL COMMENT 'MEMBERSHIP_FEE, SETTLEMENT, DEPOSIT',
  expected_amount DECIMAL(19,2) NOT NULL COMMENT '예상 금액',
  expected_date DATE NOT NULL COMMENT '예상 날짜',
  match_days_range INT DEFAULT 10 COMMENT '매칭 날짜 범위 (±N일)',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, MATCHED, EXPIRED',
  match_type VARCHAR(20) COMMENT 'AUTO_MATCHED, CONFIRMED',
  matched_history_id BIGINT COMMENT '매칭된 bank_transaction_history.history_id',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME COMMENT '만료 시간',
  matched_at DATETIME COMMENT '매칭 완료 시간',
  matched_by BIGINT COMMENT '수동 매칭 사용자',

  CONSTRAINT fk_payment_request_club
    FOREIGN KEY (club_id) REFERENCES clubs(club_id),
  CONSTRAINT fk_payment_request_member
    FOREIGN KEY (member_id) REFERENCES club_members(member_id),
  CONSTRAINT fk_payment_request_history
    FOREIGN KEY (matched_history_id) REFERENCES bank_transaction_history(history_id),

  INDEX idx_pr_club_status (club_id, status),
  INDEX idx_pr_member_name (member_name),
  INDEX idx_pr_expected_date (expected_date),
  INDEX idx_pr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='입금요청 (회비/정산 등)';

-- =========================================================
-- C. bank_accounts 데이터 정리 및 컬럼 정리
-- =========================================================

-- C-1. bank_id가 NULL이면 bank_code 기반으로 banks 매핑
-- bank_code가 NULL이면 STUB로 매핑
UPDATE bank_accounts ba
JOIN banks b ON b.bank_code = COALESCE(ba.bank_code, 'STUB')
SET ba.bank_id = b.bank_id
WHERE ba.bank_id IS NULL;

-- C-2. bank_id NOT NULL 강제 (위 UPDATE 후 실행해야 안전)
ALTER TABLE bank_accounts
  MODIFY bank_id BIGINT NOT NULL;

-- C-3. bank_name 컬럼 제거 (banks 테이블 조인으로 대체)
-- 기존 데이터 보존 필요하면 먼저 백업 후 DROP
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_accounts'
     AND COLUMN_NAME = 'bank_name') = 1,
  'ALTER TABLE bank_accounts DROP COLUMN bank_name',
  'SELECT "bank_name already dropped" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- =========================================================
-- D. bank_transaction_history 스키마 정리
-- =========================================================

-- D-1. sender_name -> print_content (적요)
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_transaction_history'
     AND COLUMN_NAME = 'sender_name') = 1,
  'ALTER TABLE bank_transaction_history
     CHANGE COLUMN sender_name print_content VARCHAR(100) NOT NULL',
  'SELECT "sender_name not found (maybe already migrated)" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- D-2. sender_account_number 제거 (존재할 때만)
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_transaction_history'
     AND COLUMN_NAME = 'sender_account_number') = 1,
  'ALTER TABLE bank_transaction_history DROP COLUMN sender_account_number',
  'SELECT "sender_account_number already dropped" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- D-3. matched_log_id 추가 (없으면)
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_transaction_history'
     AND COLUMN_NAME = 'matched_log_id') = 0,
  'ALTER TABLE bank_transaction_history
     ADD COLUMN matched_log_id BIGINT NULL AFTER is_matched',
  'SELECT "matched_log_id already exists" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- D-4. matched_at 추가 (없으면)
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_transaction_history'
     AND COLUMN_NAME = 'matched_at') = 0,
  'ALTER TABLE bank_transaction_history
     ADD COLUMN matched_at DATETIME NULL AFTER matched_log_id',
  'SELECT "matched_at already exists" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- D-5. unique_tx_key NOT NULL 강제
ALTER TABLE bank_transaction_history
  MODIFY COLUMN unique_tx_key VARCHAR(255) NOT NULL;

-- D-6. is_matched NOT NULL DEFAULT 0 강제
ALTER TABLE bank_transaction_history
  MODIFY COLUMN is_matched TINYINT(1) NOT NULL DEFAULT 0;

-- D-7. 인덱스 추가 (없으면)
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_transaction_history'
     AND INDEX_NAME = 'idx_bth_matched_log_id') = 0,
  'CREATE INDEX idx_bth_matched_log_id ON bank_transaction_history(matched_log_id)',
  'SELECT "idx_bth_matched_log_id already exists" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_transaction_history'
     AND INDEX_NAME = 'idx_bth_is_matched') = 0,
  'CREATE INDEX idx_bth_is_matched ON bank_transaction_history(is_matched)',
  'SELECT "idx_bth_is_matched already exists" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_transaction_history'
     AND INDEX_NAME = 'idx_bth_matched_at') = 0,
  'CREATE INDEX idx_bth_matched_at ON bank_transaction_history(matched_at)',
  'SELECT "idx_bth_matched_at already exists" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- D-8. FK 추가 (없으면)
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bank_transaction_history'
     AND CONSTRAINT_NAME = 'fk_bth_matched_log') = 0,
  'ALTER TABLE bank_transaction_history
     ADD CONSTRAINT fk_bth_matched_log
     FOREIGN KEY (matched_log_id) REFERENCES transaction_log (transaction_id)
     ON DELETE SET NULL
     ON UPDATE CASCADE',
  'SELECT "fk_bth_matched_log already exists" AS message'
));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET FOREIGN_KEY_CHECKS = 1;
