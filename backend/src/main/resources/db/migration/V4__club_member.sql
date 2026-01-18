-- V4: 모임 멤버 테이블
--
-- V1 초기 스키마에서 변경된 사항:
-- 1. club_nickname → nickname 컬럼명 변경
-- 2. nickname 길이: VARCHAR(50) → VARCHAR(10) 변경
-- 3. uk_club_nickname 제약조건 추가 (club_id, nickname)
--
-- 엔티티 변경사항:
-- - ClubMembers.java: clubNickname → nickname 변경, 길이 제한 10자

SET FOREIGN_KEY_CHECKS = 0;

-- 1. club_nickname → nickname 컬럼명 변경 및 길이 수정
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_members'
      AND COLUMN_NAME = 'club_nickname'
);

SET @sql = IF(@col_exists > 0,
              'ALTER TABLE club_members CHANGE COLUMN club_nickname nickname VARCHAR(10) NOT NULL COMMENT ''모임 내 별칭 (최대 10자)''',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. uk_club_nickname 제약조건 추가 (club_id, nickname)
SET @uk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_members'
      AND CONSTRAINT_NAME = 'uk_club_nickname'
      AND CONSTRAINT_TYPE = 'UNIQUE'
);
-- 중복된 (club_id, nickname) 조합이 있는지 확인
SET @duplicate_count = (
    SELECT COUNT(*)
    FROM (
             SELECT club_id, nickname, COUNT(*) as cnt
             FROM club_members
             GROUP BY club_id, nickname
             HAVING cnt > 1
         ) AS duplicates
);

SET @sql = IF(@duplicate_count > 0,
              CONCAT('SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Cannot add UNIQUE constraint: ', @duplicate_count, ' duplicate (club_id, nickname) found. Please clean up duplicate data first.'''),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@duplicate_count = 0 AND @uk_exists = 0,
              'ALTER TABLE club_members ADD CONSTRAINT uk_club_nickname UNIQUE (club_id, nickname)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;