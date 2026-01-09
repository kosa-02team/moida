-- V3: 일정-투표 관계 및 일정-모임 관계 추가
--
-- 주요 변경사항:
-- 1. schedules 테이블: club_id 컬럼 추가 (일정이 모임에 직접 속하도록)
-- 2. votes 테이블: 
--    - vote_type 컬럼 추가 (GENERAL/ATTENDANCE 구분)
--    - schedule_id 컬럼 추가 (ATTENDANCE 타입일 때 일정과 연결)
--    - post_id를 nullable로 변경 (ATTENDANCE 타입은 게시글과 무관할 수 있음)
--
-- 엔티티 변경사항:
-- - Schedules.java: clubId 필드 추가
-- - Votes.java: voteType, scheduleId 필드 추가, postId nullable로 변경

SET FOREIGN_KEY_CHECKS = 0;

-- 1. schedules 테이블에 club_id 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND COLUMN_NAME = 'club_id'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE schedules ADD COLUMN club_id BIGINT NULL AFTER schedule_id', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 데이터에 club_id 설정 (posts를 통해 간접적으로 가져오기)
-- posts.schedule_id가 있는 경우, 해당 post의 club_id를 schedules.club_id에 설정
UPDATE schedules s
INNER JOIN posts p ON s.schedule_id = p.schedule_id
SET s.club_id = p.club_id
WHERE s.club_id IS NULL;

-- club_id가 여전히 NULL인 경우, schedule_participants를 통해 간접적으로 가져오기
-- (일정 참석자 중 첫 번째 참석자의 모임을 찾기 위해 posts를 거쳐야 함)
-- 이 경우는 복잡하므로, 일단 NULL로 두고 애플리케이션 레벨에서 처리하거나
-- 수동으로 업데이트하도록 함

-- club_id를 NOT NULL로 변경 (기존 데이터가 모두 채워진 후)
SET @null_count = (
    SELECT COUNT(*) 
    FROM schedules 
    WHERE club_id IS NULL
);

SET @sql = IF(@null_count = 0, 
    'ALTER TABLE schedules MODIFY COLUMN club_id BIGINT NOT NULL', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- club_id에 FK 추가
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'schedules' 
      AND CONSTRAINT_NAME = 'fk_schedules_club_id'
);

SET @sql = IF(@fk_exists = 0, 
    'ALTER TABLE schedules ADD CONSTRAINT fk_schedules_club_id FOREIGN KEY (club_id) REFERENCES clubs(club_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. votes 테이블에 vote_type 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'votes' 
      AND COLUMN_NAME = 'vote_type'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE votes ADD COLUMN vote_type VARCHAR(20) DEFAULT ''GENERAL'' COMMENT ''GENERAL: 일반 투표, ATTENDANCE: 일정 참석 투표'' AFTER post_id', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. votes 테이블에 schedule_id 컬럼 추가
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'votes' 
      AND COLUMN_NAME = 'schedule_id'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE votes ADD COLUMN schedule_id BIGINT NULL AFTER vote_type', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- schedule_id에 FK 추가
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'votes' 
      AND CONSTRAINT_NAME = 'fk_votes_schedule_id'
);

SET @sql = IF(@fk_exists = 0, 
    'ALTER TABLE votes ADD CONSTRAINT fk_votes_schedule_id FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. votes 테이블의 post_id를 nullable로 변경
-- 먼저 FK 제약조건 제거
SET @fk_name = (
    SELECT CONSTRAINT_NAME 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'votes' 
      AND COLUMN_NAME = 'post_id'
    LIMIT 1
);

SET @sql = IF(@fk_name IS NOT NULL, 
    CONCAT('ALTER TABLE votes DROP FOREIGN KEY ', @fk_name), 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- post_id를 nullable로 변경
ALTER TABLE votes MODIFY COLUMN post_id BIGINT NULL COMMENT '연결된 게시글 (GENERAL 타입일 때 사용, ATTENDANCE 타입은 NULL 가능)';

-- post_id FK 재추가 (nullable이어도 FK는 가능)
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'votes' 
      AND CONSTRAINT_NAME = 'fk_votes_post_id'
);

SET @sql = IF(@fk_exists = 0, 
    'ALTER TABLE votes ADD CONSTRAINT fk_votes_post_id FOREIGN KEY (post_id) REFERENCES posts(post_id)', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. votes 테이블에 deadline 컬럼 추가 (일반 투표 기한 설정용)
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'votes' 
      AND COLUMN_NAME = 'deadline'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE votes ADD COLUMN deadline DATETIME NULL COMMENT ''투표 종료 기한 (일반 투표용, 기한이 지나면 자동 종료)'' AFTER closed_at', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
