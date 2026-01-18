-- V10: 모임(clubs) 테이블 추가 변경사항
--
-- V1 초기 스키마에서 추가로 변경된 사항:
-- 1. visibility 컬럼 추가 (PUBLIC/PRIVATE)
-- 2. owner_id에 KEY 인덱스 추가
-- 3. main_account_id에 KEY 인덱스 추가
--
-- 엔티티 변경사항:
-- - Clubs.java: visibility 필드 추가

SET FOREIGN_KEY_CHECKS = 0;

-- 1. visibility 컬럼 추가
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

-- 2. owner_id에 KEY 인덱스 추가
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

-- 3. main_account_id에 KEY 인덱스 추가
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
