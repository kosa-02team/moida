-- V18: NOT NULL 제약조건 추가 및 데이터 정합성 개선
--
-- 변경사항:
-- 1. clubs.invite_code에 NOT NULL 제약조건 추가
-- 2. club_members.role에 NOT NULL 제약조건 추가
-- 3. club_members.status에 NOT NULL 제약조건 추가
-- 4. FOREIGN_KEY_CHECKS 실패 시 복구 보장

-- Stored Procedure를 사용하여 SIGNAL 발생 시에도 FK 체크 복구 보장
DELIMITER $$

DROP PROCEDURE IF EXISTS add_not_null_constraints$$

CREATE PROCEDURE add_not_null_constraints()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        SET FOREIGN_KEY_CHECKS = 1;
        RESIGNAL;
    END;

    SET FOREIGN_KEY_CHECKS = 0;

    -- 1. clubs.invite_code에 NOT NULL 제약조건 추가
    -- 기존 NULL 값이 있으면 UUID로 채움
    SET @null_count = (
        SELECT COUNT(*)
        FROM clubs
        WHERE invite_code IS NULL
    );

    SET @sql = IF(@null_count > 0,
                  'UPDATE clubs SET invite_code = UUID() WHERE invite_code IS NULL',
                  'SELECT 1');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    SET @col_nullable = (
        SELECT IS_NULLABLE
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'clubs'
          AND COLUMN_NAME = 'invite_code'
    );

    SET @sql = IF(@col_nullable = 'YES',
                  'ALTER TABLE clubs MODIFY COLUMN invite_code VARCHAR(36) NOT NULL UNIQUE COMMENT ''초대 코드 (UUID 형식, 36자, 재발급 가능)''',
                  'SELECT 1');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    -- 2. club_members.role에 NOT NULL 제약조건 추가
    -- 기존 NULL 값이 있으면 기본값 'MEMBER'로 채움
    SET @null_count = (
        SELECT COUNT(*)
        FROM club_members
        WHERE role IS NULL
    );

    SET @sql = IF(@null_count > 0,
                  'UPDATE club_members SET role = ''MEMBER'' WHERE role IS NULL',
                  'SELECT 1');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    SET @col_nullable = (
        SELECT IS_NULLABLE
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'club_members'
          AND COLUMN_NAME = 'role'
    );

    SET @sql = IF(@col_nullable = 'YES',
                  'ALTER TABLE club_members MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT ''MEMBER'' COMMENT ''역할: OWNER(모임장),STAFF(운영진),ACCOUNTANT(총무),MEMBER(일반 회원)''',
                  'SELECT 1');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    -- 3. club_members.status에 NOT NULL 제약조건 추가
    -- 기존 NULL 값이 있으면 기본값 'PENDING'으로 채움
    SET @null_count = (
        SELECT COUNT(*)
        FROM club_members
        WHERE status IS NULL
    );

    SET @sql = IF(@null_count > 0,
                  'UPDATE club_members SET status = ''PENDING'' WHERE status IS NULL',
                  'SELECT 1');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    SET @col_nullable = (
        SELECT IS_NULLABLE
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'club_members'
          AND COLUMN_NAME = 'status'
    );

    SET @sql = IF(@col_nullable = 'YES',
                  'ALTER TABLE club_members MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''상태: PENDING(기본: 승인 대기),ACTIVE(활동),LEFT(탈퇴),KICKED(강퇴),REJECTED(가입 거절)''',
                  'SELECT 1');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    -- FK 체크 복구 (정상 종료 시)
    SET FOREIGN_KEY_CHECKS = 1;
END$$

DELIMITER ;

-- Stored Procedure 실행
CALL add_not_null_constraints();

-- Stored Procedure 삭제
DROP PROCEDURE IF EXISTS add_not_null_constraints;
