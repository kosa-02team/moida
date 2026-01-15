-- V7: bank_accounts 테이블에 club_id, bank_code 추가
--
-- 주요 변경사항:
-- 1. bank_accounts 테이블에 club_id 컬럼 추가 (어느 모임의 계좌인지 추적)
-- 2. bank_accounts 테이블에 bank_code 컬럼 추가 (어느 Provider로 생성되었는지 추적)
-- 3. payment_requests 테이블이 없으면 생성
-- 4. bank_id가 NULL인 row가 있으면 먼저 채우기 (임시로 STUB 은행 1개 만든 뒤 매핑하는 방식)
-- banks 테이블에 STUB 레코드가 있다고 가정 (없으면 먼저 생성 필요)
-- 예: INSERT INTO banks(bank_code, bank_name) VALUES ('STUB','스텁은행');

SET FOREIGN_KEY_CHECKS = 0;

-- bank_accounts 테이블에 club_id 컬럼 추가
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bank_accounts'
       AND COLUMN_NAME = 'club_id') = 0,
    'ALTER TABLE bank_accounts ADD COLUMN club_id BIGINT NULL AFTER account_id',
    'SELECT "club_id column already exists" AS message'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- bank_accounts 테이블에 bank_code 컬럼 추가
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bank_accounts'
       AND COLUMN_NAME = 'bank_code') = 0,
    'ALTER TABLE bank_accounts ADD COLUMN bank_code VARCHAR(20) NULL AFTER club_id',
    'SELECT "bank_code column already exists" AS message'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- club_id에 대한 FK 제약조건 추가
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bank_accounts'
       AND CONSTRAINT_NAME = 'fk_bank_accounts_club_id') = 0,
    'ALTER TABLE bank_accounts ADD CONSTRAINT fk_bank_accounts_club_id FOREIGN KEY (club_id) REFERENCES clubs(club_id)',
    'SELECT "fk_bank_accounts_club_id constraint already exists" AS message'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- club_id와 bank_code에 인덱스 추가 (조회 성능 향상)
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bank_accounts'
       AND INDEX_NAME = 'idx_bank_accounts_club_id') = 0,
    'CREATE INDEX idx_bank_accounts_club_id ON bank_accounts(club_id)',
    'SELECT "idx_bank_accounts_club_id index already exists" AS message'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- payment_requests 테이블 생성 (입금요청 기반 매칭 시스템)

-- payment_requests 테이블이 없으면 생성
CREATE TABLE IF NOT EXISTS payment_requests (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    member_name VARCHAR(50) NOT NULL COMMENT '회원 이름 (매칭 기준)',
    request_type VARCHAR(20) NOT NULL COMMENT 'MEMBERSHIP_FEE, SETTLEMENT, DEPOSIT',
    expected_amount DECIMAL(19,2) NOT NULL COMMENT '예상 금액',
    expected_date DATE NOT NULL COMMENT '예상 날짜 (±10일 범위 허용)',
    match_days_range INT DEFAULT 10 COMMENT '매칭 날짜 범위 (±N일)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, MATCHED, EXPIRED',
    match_type VARCHAR(20) COMMENT 'AUTO_MATCHED, CONFIRMED, NULL',
    matched_history_id BIGINT COMMENT '매칭된 BankTransactionHistory ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME COMMENT '만료 시간',
    matched_at DATETIME COMMENT '매칭 완료 시간',
    matched_by BIGINT COMMENT '매칭을 수행한 사용자 (수동 매칭 시)',
    
    CONSTRAINT fk_payment_request_club FOREIGN KEY (club_id) REFERENCES clubs(club_id),
    CONSTRAINT fk_payment_request_member FOREIGN KEY (member_id) REFERENCES club_members(member_id),
    CONSTRAINT fk_payment_request_history FOREIGN KEY (matched_history_id) REFERENCES bank_transaction_history(history_id),
    
    INDEX idx_club_status (club_id, status),
    INDEX idx_member_name (member_name),
    INDEX idx_expected_date (expected_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='입금요청 (회비/정산 등)';

UPDATE bank_accounts ba
JOIN banks b ON b.bank_code = COALESCE(ba.bank_code, 'STUB')
SET ba.bank_id = b.bank_id
WHERE ba.bank_id IS NULL;

-- 2) bank_id를 NOT NULL로 변경
ALTER TABLE bank_accounts
  MODIFY bank_id BIGINT NOT NULL;

-- 3) 중복 컬럼(bank_name) 제거 (banks 테이블로부터 join해서 쓰도록)
-- 기존 데이터가 필요하면 백업 후 drop
ALTER TABLE bank_accounts
  DROP COLUMN bank_name;

-- 4) club_id, bank_code 인덱스 (이미 있으면 생략)
CREATE INDEX idx_bank_accounts_bank_code ON bank_accounts(bank_code);
SET FOREIGN_KEY_CHECKS = 1;
