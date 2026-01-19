-- V10: 모임(clubs) 테이블 추가 변경사항
--
-- V1 초기 스키마에서 추가로 변경된 사항:
-- 1. name → club_name 컬럼명 변경 및 길이 조정 (VARCHAR(100) → VARCHAR(20))
-- 2. main_account_id 타입 변경 (BIGINT → VARCHAR(36), UUID 사용)
-- 3. club_type 컬럼 추가 (OPERATION_FEE/FAIR_SETTLEMENT)
-- 4. visibility 컬럼 추가 (PUBLIC/PRIVATE)
-- 5. status, visibility에 NOT NULL 제약조건 추가
-- 6. owner_id, main_account_id에 KEY 인덱스 추가
--
-- 엔티티 변경사항:
-- - Clubs.java: name → clubName(DB: club_name), mainAccountId UUID 타입, type 필드, visibility 필드 추가

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

-- 2. main_account_id 타입 변경 (BIGINT → VARCHAR(36), UUID 형식)
-- FK 제약조건 확인 및 제거
SET @fk_name = NULL;
SELECT CONSTRAINT_NAME INTO @fk_name
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'clubs'
  AND COLUMN_NAME = 'main_account_id'
  AND REFERENCED_TABLE_NAME IS NOT NULL
LIMIT 1;

SET @sql = IF(@fk_name IS NOT NULL,
              CONCAT('ALTER TABLE clubs DROP FOREIGN KEY ', @fk_name),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- main_account_id 타입 변경 (기존 데이터를 UUID로 변환)
UPDATE clubs SET main_account_id = UUID() WHERE main_account_id IS NOT NULL;

ALTER TABLE clubs MODIFY COLUMN main_account_id VARCHAR(36) NOT NULL COMMENT '모임 대표 계좌 ID (UUID)';

-- 3. club_type 컬럼 추가
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

-- 4. max_members 컬럼 추가
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

-- 5. visibility 컬럼 추가
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

-- 6. status, visibility 컬럼에 NOT NULL 제약조건 추가 (기존 데이터 보호)
-- 기존 NULL 값을 기본값으로 먼저 채움
UPDATE clubs SET status = 'ACTIVE' WHERE status IS NULL;
UPDATE clubs SET visibility = 'PUBLIC' WHERE visibility IS NULL;

-- NOT NULL 제약조건 적용
ALTER TABLE clubs 
  MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE: 활성, INACTIVE: 비활성',
  MODIFY COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC: 공개, PRIVATE: 비공개';

-- 7. owner_id에 KEY 인덱스 추가
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

-- 8. main_account_id에 KEY 인덱스 추가
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
