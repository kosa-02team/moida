-- V2: 스키마 업데이트
--
-- 주요 변경사항:
-- 1. banks 테이블 추가: 오픈 뱅킹 API 대안 구조 (은행 코드, 이름, Provider 클래스명 관리)
-- 2. bank_accounts 테이블 수정: bank_id FK 추가 (bank_name 컬럼 제거, banks 테이블과 연결)
-- 3. clubs 테이블 수정: visibility 컬럼 추가 (PUBLIC/PRIVATE 설정)
-- 4. posts 테이블 수정: schedule_id FK 추가 (nullable, 일정과 연결 가능)
-- 5. schedules 테이블 구조 변경:
--    - post_id PK 제거 → schedule_id (새 PK) 추가
--    - schedule_name, end_date, description 컬럼 추가
--    - 1:1 관계 → 1:N 관계로 변경 (하나의 일정에 여러 게시글 가능)
--    - created_at, updated_at 컬럼 추가
-- 6. schedule_participants 테이블 수정:
--    - fee_status 컬럼 추가 (PENDING/PAID/EXPIRED)
--    - matched_transaction_id 컬럼 추가 (은행 거래 내역 매칭)
--    - fee_request_closed_at 컬럼 추가 (회비 요청 종료 시점)
--    - created_at, updated_at 컬럼 추가
-- 7. settlement_requests → fee_collection_requests로 테이블명 변경
--    - post_id → schedule_id로 컬럼명 변경
--    - status 값 의미 변경 (PENDING → OPEN/CLOSED/PENDING)
-- 8. settlement_items 테이블 삭제 (영수증 첨부 기능 제거)
--
-- 엔티티 변경사항:
-- - Banks.java: 새로 생성
-- - BankAccounts.java: bank_id FK 추가, bank_name 필드 제거
-- - Clubs.java: visibility 필드 추가
-- - Posts.java: schedule_id 필드 추가, @OneToOne 관계 제거
-- - Schedules.java: post_id 제거, schedule_id (PK) 추가, BaseEntity 상속
-- - ScheduleParticipants.java: fee 관련 필드 추가, BaseEntity 상속
-- - SettlementRequests.java 삭제 → FeeCollectionRequests.java 생성
-- - SettlementItems.java 삭제
--

SET FOREIGN_KEY_CHECKS = 0;

-- 1. banks 테이블 추가 (오픈 뱅킹 API 대안 구조)
CREATE TABLE IF NOT EXISTS banks (
    bank_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_code VARCHAR(10) NOT NULL UNIQUE COMMENT '은행 코드',
    bank_name VARCHAR(50) NOT NULL COMMENT '은행 이름',
    provider_class_name VARCHAR(255) COMMENT 'Provider 클래스명',
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. bank_accounts 테이블 수정 (bank_id FK 추가)
-- 컬럼 존재 확인 후 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bank_accounts' 
      AND COLUMN_NAME = 'bank_id'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bank_accounts ADD COLUMN bank_id BIGINT NULL AFTER user_id', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- FK 제약조건 존재 확인 후 추가
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bank_accounts' 
      AND CONSTRAINT_NAME = 'fk_bank_accounts_bank_id'
);

SET @sql = IF(@fk_exists = 0, 
    'ALTER TABLE bank_accounts ADD CONSTRAINT fk_bank_accounts_bank_id FOREIGN KEY (bank_id) REFERENCES banks(bank_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. clubs 테이블 수정 (visibility 컬럼 추가)
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'clubs' 
      AND COLUMN_NAME = 'visibility'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE clubs ADD COLUMN visibility VARCHAR(20) DEFAULT ''PUBLIC'' COMMENT ''PUBLIC: 공개, PRIVATE: 비공개'' AFTER status', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. posts 테이블 수정 (schedule_id FK 추가)
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'posts' 
      AND COLUMN_NAME = 'schedule_id'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE posts ADD COLUMN schedule_id BIGINT NULL COMMENT ''일정 연결 (optional)'' AFTER category', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. schedules 테이블 구조 변경 (1:1 → 1:N 관계로 변경)

-- 기존 FK 제거 (schedule_participants -> schedules)
SET @fk_name = NULL;
SELECT CONSTRAINT_NAME INTO @fk_name 
FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'schedule_participants' 
  AND REFERENCED_TABLE_NAME = 'schedules' 
LIMIT 1;

SET @sql = IF(@fk_name IS NOT NULL, 
    CONCAT('ALTER TABLE schedule_participants DROP FOREIGN KEY ', @fk_name), 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 FK 제거 (schedules -> posts)
SET @fk_name = NULL;
SELECT CONSTRAINT_NAME INTO @fk_name 
FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'schedules' 
  AND REFERENCED_TABLE_NAME = 'posts' 
LIMIT 1;

SET @sql = IF(@fk_name IS NOT NULL, 
    CONCAT('ALTER TABLE schedules DROP FOREIGN KEY ', @fk_name), 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- schedule_id 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND COLUMN_NAME = 'schedule_id'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedules ADD COLUMN schedule_id BIGINT NULL FIRST', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- schedule_name 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND COLUMN_NAME = 'schedule_name'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedules ADD COLUMN schedule_name VARCHAR(200) NULL AFTER schedule_id', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- end_date 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND COLUMN_NAME = 'end_date'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedules ADD COLUMN end_date DATETIME NULL AFTER event_date', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- description 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND COLUMN_NAME = 'description'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedules ADD COLUMN description TEXT COMMENT ''일정 상세 설명 (준비물, 공지사항 등)'' AFTER location', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 데이터에 기본값 설정
UPDATE schedules SET 
    schedule_id = post_id,
    schedule_name = COALESCE(schedule_name, CONCAT('일정_', post_id)),
    end_date = COALESCE(end_date, DATE_ADD(event_date, INTERVAL 2 HOUR))
WHERE schedule_id IS NULL OR schedule_id = 0;

-- 기존 PK 제거 전에 UNIQUE 인덱스 확인
SET @idx_exists = (
    SELECT COUNT(*) 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND INDEX_NAME = 'idx_schedules_schedule_id'
);

SET @sql = IF(@idx_exists = 0, 
    'CREATE UNIQUE INDEX idx_schedules_schedule_id ON schedules(schedule_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 PK 제거
ALTER TABLE schedules DROP PRIMARY KEY;

-- 새 PK 설정 및 NOT NULL 제약조건 추가
ALTER TABLE schedules 
MODIFY COLUMN schedule_id BIGINT AUTO_INCREMENT,
MODIFY COLUMN schedule_name VARCHAR(200) NOT NULL,
MODIFY COLUMN end_date DATETIME NOT NULL,
ADD PRIMARY KEY (schedule_id);

-- created_at, updated_at 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND COLUMN_NAME = 'created_at'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedules ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP AFTER closed_at', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND COLUMN_NAME = 'updated_at'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedules ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- post_id 컬럼 제거
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND COLUMN_NAME = 'post_id'
);

SET @sql = IF(@col_exists > 0, 
    'ALTER TABLE schedules DROP COLUMN post_id', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. posts 테이블에 schedule_id FK 추가 (schedules 변경 후)
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'posts' 
      AND CONSTRAINT_NAME = 'fk_posts_schedule_id'
);

SET @sql = IF(@fk_exists = 0, 
    'ALTER TABLE posts ADD CONSTRAINT fk_posts_schedule_id FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7. schedule_participants 테이블 수정 (fee 관련 컬럼 추가)

-- fee_status 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedule_participants' 
      AND COLUMN_NAME = 'fee_status'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedule_participants ADD COLUMN fee_status VARCHAR(20) DEFAULT ''PENDING'' COMMENT ''PENDING: 대기, PAID: 입금확인, EXPIRED: 기간만료'' AFTER is_refunded', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- matched_transaction_id 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedule_participants' 
      AND COLUMN_NAME = 'matched_transaction_id'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedule_participants ADD COLUMN matched_transaction_id BIGINT NULL COMMENT ''bank_transaction_history와 매칭'' AFTER fee_status', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- fee_request_closed_at 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedule_participants' 
      AND COLUMN_NAME = 'fee_request_closed_at'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedule_participants ADD COLUMN fee_request_closed_at DATETIME NULL COMMENT ''회비 요청 종료 시점'' AFTER matched_transaction_id', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- created_at, updated_at 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedule_participants' 
      AND COLUMN_NAME = 'created_at'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedule_participants ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP AFTER fee_request_closed_at', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedule_participants' 
      AND COLUMN_NAME = 'updated_at'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedule_participants ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- schedule_id FK 재추가
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedule_participants' 
      AND CONSTRAINT_NAME = 'fk_schedule_participants_schedule_id'
);

SET @sql = IF(@fk_exists = 0, 
    'ALTER TABLE schedule_participants ADD CONSTRAINT fk_schedule_participants_schedule_id FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- matched_transaction_id FK 추가
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedule_participants' 
      AND CONSTRAINT_NAME = 'fk_schedule_participants_transaction_id'
);

SET @sql = IF(@fk_exists = 0, 
    'ALTER TABLE schedule_participants ADD CONSTRAINT fk_schedule_participants_transaction_id FOREIGN KEY (matched_transaction_id) REFERENCES bank_transaction_history(history_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 8. settlement_requests → fee_collection_requests로 변경

-- settlement_items 테이블 삭제 (먼저 FK 제거 필요)
SET @fk_name = NULL;
SELECT CONSTRAINT_NAME INTO @fk_name 
FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'settlement_items' 
  AND REFERENCED_TABLE_NAME = 'settlement_requests' 
LIMIT 1;

SET @sql = IF(@fk_name IS NOT NULL, 
    CONCAT('ALTER TABLE settlement_items DROP FOREIGN KEY ', @fk_name), 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TABLE IF EXISTS settlement_items;

-- settlement_requests 테이블명 변경
SET @table_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLES 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'settlement_requests'
);

SET @sql = IF(@table_exists > 0, 
    'RENAME TABLE settlement_requests TO fee_collection_requests', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 컬럼명 변경: post_id → schedule_id
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'fee_collection_requests' 
      AND COLUMN_NAME = 'post_id'
);

SET @sql = IF(@col_exists > 0, 
    'ALTER TABLE fee_collection_requests CHANGE COLUMN post_id schedule_id BIGINT NOT NULL', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 FK 제거 (posts 참조)
SET @fk_name = NULL;
SELECT CONSTRAINT_NAME INTO @fk_name 
FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'fee_collection_requests' 
  AND REFERENCED_TABLE_NAME = 'posts' 
LIMIT 1;

SET @sql = IF(@fk_name IS NOT NULL, 
    CONCAT('ALTER TABLE fee_collection_requests DROP FOREIGN KEY ', @fk_name), 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 새 FK 추가
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'fee_collection_requests' 
      AND CONSTRAINT_NAME = 'fk_fee_collection_requests_schedule_id'
);

SET @sql = IF(@fk_exists = 0, 
    'ALTER TABLE fee_collection_requests ADD CONSTRAINT fk_fee_collection_requests_schedule_id FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- status 컬럼 주석 업데이트
ALTER TABLE fee_collection_requests 
MODIFY COLUMN status VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN: 요청중, CLOSED: 종료, PENDING: 대기';

SET FOREIGN_KEY_CHECKS = 1;