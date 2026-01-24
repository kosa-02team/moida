-- V20: 모임 삭제 요청 및 운영진 동의 기능 추가
--
-- 변경사항:
-- 1. clubs 테이블에 deletion_request_status, deletion_requested_at 컬럼 추가
-- 2. club_members 테이블에 deletion_approval 컬럼 추가
-- 3. 모임장이 삭제 요청을 시작하고, 모든 운영진이 동의하면 삭제 가능

-- Stored Procedure를 사용하여 안전하게 처리
DELIMITER $$

DROP PROCEDURE IF EXISTS add_deletion_approval_columns$$

CREATE PROCEDURE add_deletion_approval_columns()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        SET FOREIGN_KEY_CHECKS = 1;
        RESIGNAL;
    END;

    SET FOREIGN_KEY_CHECKS = 0;

    -- 1. clubs 테이블에 deletion_request_status 컬럼 추가
    SET @col_exists = (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'clubs'
          AND COLUMN_NAME = 'deletion_request_status'
    );

    SET @sql = IF(@col_exists = 0,
                  'ALTER TABLE clubs ADD COLUMN deletion_request_status VARCHAR(20) DEFAULT NULL COMMENT ''삭제 요청 상태: NONE(없음), PENDING(진행중), APPROVED(동의완료)''',
                  'SELECT 1');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    -- 2. clubs 테이블에 deletion_requested_at 컬럼 추가
    SET @col_exists = (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'clubs'
          AND COLUMN_NAME = 'deletion_requested_at'
    );

    SET @sql = IF(@col_exists = 0,
                  'ALTER TABLE clubs ADD COLUMN deletion_requested_at DATETIME DEFAULT NULL COMMENT ''삭제 요청 시작 시간''',
                  'SELECT 1');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    -- 3. club_members 테이블에 deletion_approval 컬럼 추가
    SET @col_exists = (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'club_members'
          AND COLUMN_NAME = 'deletion_approval'
    );

    SET @sql = IF(@col_exists = 0,
                  'ALTER TABLE club_members ADD COLUMN deletion_approval BOOLEAN DEFAULT NULL COMMENT ''삭제 동의 여부: NULL(미응답), TRUE(동의), FALSE(거부)''',
                  'SELECT 1');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    SET FOREIGN_KEY_CHECKS = 1;
END$$

DELIMITER ;

-- Stored Procedure 실행
CALL add_deletion_approval_columns();

-- Stored Procedure 삭제
DROP PROCEDURE IF EXISTS add_deletion_approval_columns;
