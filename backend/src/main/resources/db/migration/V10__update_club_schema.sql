-- V10: Clubs 스키마 업데이트
--
-- 주요 변경사항:
-- 1. name → club_name 컬럼명 변경
-- 2. club_name에 UNIQUE 제약조건 추가
-- 3. main_account_id: bigint → VARCHAR(36) 변경 (UUID 사용, FK 제약 제거)
-- 4. invite_code: VARCHAR(20) → VARCHAR(36) 변경 (UUID 길이)
-- 5. club_type 컬럼 추가 (VARCHAR(20), NOT NULL, DEFAULT 'OPERATION_FEE')
-- 6. max_members 컬럼 추가 (INT, NOT NULL, DEFAULT 100)
--
-- 엔티티 변경사항:
-- - Clubs.java: clubName, mainAccountId (UUID String), clubType, maxMembers 추가

SET FOREIGN_KEY_CHECKS = 0;

-- 1. main_account_id의 FK 제약조건 제거 (UUID String으로 변경하므로 FK 불가)
SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clubs'
      AND CONSTRAINT_NAME = 'clubs_ibfk_2'
);

SET @sql = IF(@fk_exists > 0,
    'ALTER TABLE clubs DROP FOREIGN KEY clubs_ibfk_2',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. main_account_id 컬럼 타입 변경 (bigint → VARCHAR(36))
-- 기존 데이터가 있는 경우 UUID로 변환해야 하지만, 현재는 임시값 처리
ALTER TABLE clubs
    MODIFY COLUMN main_account_id VARCHAR(36) NOT NULL COMMENT '대표 계좌 UUID (임시)';

-- 3. invite_code 컬럼 길이 변경 (VARCHAR(20) → VARCHAR(36))
ALTER TABLE clubs
    MODIFY COLUMN invite_code VARCHAR(36) UNIQUE COMMENT '초대 코드 (UUID)';

-- 4. name → club_name 컬럼명 변경
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clubs'
      AND COLUMN_NAME = 'name'
);

SET @sql = IF(@col_exists > 0,
    'ALTER TABLE clubs CHANGE COLUMN name club_name VARCHAR(100) NOT NULL COMMENT ''모임 이름''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. club_name에 UNIQUE 제약조건 추가
SET @uk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clubs'
      AND CONSTRAINT_NAME = 'uk_club_name'
      AND CONSTRAINT_TYPE = 'UNIQUE'
);

-- 중복된 club_name이 있는지 확인
SET @duplicate_count = (
    SELECT COUNT(*)
    FROM (
        SELECT club_name, COUNT(*) as cnt
        FROM clubs
        GROUP BY club_name
        HAVING cnt > 1
    ) AS duplicates
);

SET @sql = IF(@duplicate_count > 0,
    CONCAT('SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Cannot add UNIQUE constraint: ', @duplicate_count, ' duplicate club_name found. Please clean up duplicate data first.'''),
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@duplicate_count = 0 AND @uk_exists = 0,
    'ALTER TABLE clubs ADD CONSTRAINT uk_club_name UNIQUE (club_name)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. club_type 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clubs'
      AND COLUMN_NAME = 'club_type'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE clubs ADD COLUMN club_type VARCHAR(20) NOT NULL DEFAULT ''OPERATION_FEE'' COMMENT ''모임 유형: OPERATION_FEE(운영비형), FAIR_SETTLEMENT(공정정산형)'' AFTER visibility',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7. max_members 컬럼 추가
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

SET FOREIGN_KEY_CHECKS = 1;
