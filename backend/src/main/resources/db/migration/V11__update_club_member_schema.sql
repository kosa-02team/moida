-- V11: 모임 멤버 테이블 추가 변경사항
--
-- V4 이후 추가로 변경된 사항:
-- 1. role 컬럼 길이: VARCHAR(20) → VARCHAR(100) 변경
-- 2. status 기본값: DEFAULT 'ACTIVE' → 'PENDING' 변경
-- 3. joined_at DEFAULT 제거
-- 4. created_at, updated_at 컬럼 추가 (BaseEntity 상속)
-- 5. FK 제약조건에 ON DELETE CASCADE 추가
--
-- 엔티티 변경사항:
-- - ClubMembers.java: BaseEntity 상속, status 기본값 PENDING, role 길이 확장

SET FOREIGN_KEY_CHECKS = 0;

-- 1. role 컬럼 길이 변경 (VARCHAR(20) → VARCHAR(100))
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_members'
      AND COLUMN_NAME = 'role'
);

SET @sql = IF(@col_exists > 0,
              'ALTER TABLE club_members MODIFY COLUMN role VARCHAR(100) DEFAULT ''MEMBER'' COMMENT ''역할: OWNER(모임장),STAFF(운영진),ACCOUNTANT(총무),MEMBER(일반 회원)''',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. status 기본값 변경 (DEFAULT 'ACTIVE' → 'PENDING')
-- 기존 데이터가 ACTIVE인 경우는 그대로 유지, 새로 추가되는 데이터만 PENDING
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_members'
      AND COLUMN_NAME = 'status'
);

SET @sql = IF(@col_exists > 0,
              'ALTER TABLE club_members MODIFY COLUMN status VARCHAR(20) DEFAULT ''PENDING'' COMMENT ''상태: PENDING(기본: 승인 대기),ACTIVE(활동),LEFT(탈퇴),KICKED(강퇴), REJECTED(가입 거절)''',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. joined_at DEFAULT 제거
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_members'
      AND COLUMN_NAME = 'joined_at'
);

SET @sql = IF(@col_exists > 0,
              'ALTER TABLE club_members MODIFY COLUMN joined_at DATETIME COMMENT ''가입 승인 시점''',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. created_at 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_members'
      AND COLUMN_NAME = 'created_at'
);

SET @sql = IF(@col_exists = 0,
              'ALTER TABLE club_members ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''레코드 생성일''',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. updated_at 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_members'
      AND COLUMN_NAME = 'updated_at'
);

SET @sql = IF(@col_exists = 0,
              'ALTER TABLE club_members ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''레코드 수정일''',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. FK 제약조건에 ON DELETE CASCADE 추가
-- 기존 FK 제약조건 확인 및 재생성

-- 6-1. fk_member_club 제약조건 재생성
SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_members'
      AND CONSTRAINT_NAME = 'fk_member_club'
);

SET @sql = IF(@fk_exists > 0,
              'ALTER TABLE club_members DROP FOREIGN KEY fk_member_club',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@fk_exists > 0,
              'ALTER TABLE club_members ADD CONSTRAINT fk_member_club FOREIGN KEY (club_id) REFERENCES clubs(club_id) ON DELETE CASCADE',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6-2. fk_member_user 제약조건 재생성
SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_members'
      AND CONSTRAINT_NAME = 'fk_member_user'
);

SET @sql = IF(@fk_exists > 0,
              'ALTER TABLE club_members DROP FOREIGN KEY fk_member_user',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@fk_exists > 0,
              'ALTER TABLE club_members ADD CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
