-- V10: 모임(clubs) 테이블 추가 변경사항
--
-- V1 초기 스키마에서 추가로 변경된 사항:
-- 1. name → club_name 컬럼명 변경 및 길이 조정 (VARCHAR(100) → VARCHAR(20))
-- 2. club_type 컬럼 추가 (OPERATION_FEE/FAIR_SETTLEMENT)
-- 3. visibility 컬럼 추가 (PUBLIC/PRIVATE)
-- 4. status, visibility에 NOT NULL 제약조건 추가
-- 5. owner_id에 KEY 인덱스 추가
-- 6. main_account_id에 KEY 인덱스 추가
--
-- 엔티티 변경사항:
-- - Clubs.java: name → clubName(DB: club_name), type 필드, visibility 필드 추가

SET FOREIGN_KEY_CHECKS = 0;

-- 1. name → club_name 컬럼명 변경 및 길이 조정
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clubs'
      AND COLUMN_NAME = 'name'
);

SET @sql = IF(@col_exists > 0,
              'ALTER TABLE clubs CHANGE COLUMN name club_name VARCHAR(20) NOT NULL COMMENT ''모임 이름 (최대 20자)''',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. club_type 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clubs'
      AND COLUMN_NAME = 'club_type'
);

SET @sql = IF(@col_exists = 0,
              'ALTER TABLE clubs ADD COLUMN club_type VARCHAR(20) NOT NULL DEFAULT ''OPERATION_FEE'' COMMENT ''OPERATION_FEE: 운영비, FAIR_SETTLEMENT: 공정정산'' AFTER invite_code',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. max_members 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clubs'
      AND COLUMN_NAME = 'max_members'
);

SET @sql = IF(@col_exists = 0,
              'ALTER TABLE clubs ADD COLUMN max_members INT NOT NULL DEFAULT 100 COMMENT ''최대 멤버 수'' AFTER club_type',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. visibility 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clubs'
      AND COLUMN_NAME = 'visibility'
);

SET @sql = IF(@col_exists = 0,
              'ALTER TABLE clubs ADD COLUMN visibility VARCHAR(20) DEFAULT ''PUBLIC'' COMMENT ''PUBLIC: 공개, PRIVATE: 비공개''',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. status, visibility 컬럼에 NOT NULL 제약조건 추가 (기존 데이터 보호)
-- 기존 NULL 값을 기본값으로 먼저 채움
UPDATE clubs SET status = 'ACTIVE' WHERE status IS NULL;
UPDATE clubs SET visibility = 'PUBLIC' WHERE visibility IS NULL;

-- NOT NULL 제약조건 적용
ALTER TABLE clubs 
  MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE: 활성, INACTIVE: 비활성',
  MODIFY COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC: 공개, PRIVATE: 비공개';

-- 6. owner_id에 KEY 인덱스 추가
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clubs'
      AND INDEX_NAME = 'owner_id'
      AND COLUMN_NAME = 'owner_id'
);

SET @sql = IF(@idx_exists = 0,
              'ALTER TABLE clubs ADD KEY owner_id (owner_id)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7. main_account_id에 KEY 인덱스 추가
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clubs'
      AND INDEX_NAME = 'main_account_id'
      AND COLUMN_NAME = 'main_account_id'
);

SET @sql = IF(@idx_exists = 0,
              'ALTER TABLE clubs ADD KEY main_account_id (main_account_id)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
