-- V19: LEFT/KICKED/REJECTED 상태 멤버의 닉네임을 고유하게 변경
--
-- 변경사항:
-- 1. LEFT, KICKED, REJECTED 상태의 멤버들의 닉네임을 고유한 값으로 변경
-- 2. uk_club_nickname 제약조건 위반 방지
-- 3. 재가입 시 같은 닉네임 사용 가능하도록 함
-- 4. 닉네임 길이 제한(10자) 고려

-- Stored Procedure를 사용하여 안전하게 처리
DELIMITER $$

DROP PROCEDURE IF EXISTS fix_inactive_member_nicknames$$

CREATE PROCEDURE fix_inactive_member_nicknames()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        SET FOREIGN_KEY_CHECKS = 1;
        RESIGNAL;
    END;

    SET FOREIGN_KEY_CHECKS = 0;

    -- LEFT, KICKED, REJECTED 상태인 멤버들의 닉네임 변경
    -- 형식: "_" + member_id (닉네임 길이 제한 10자 고려)
    -- 이미 "_"로 시작하는 닉네임은 건너뜀 (이미 처리된 것으로 간주)
    UPDATE club_members
    SET nickname = CASE
        WHEN LENGTH(CONCAT('_', CAST(member_id AS CHAR))) <= 10 
        THEN CONCAT('_', CAST(member_id AS CHAR))
        ELSE SUBSTRING(CONCAT('_', CAST(member_id AS CHAR)), 1, 10)
    END
    WHERE status IN ('LEFT', 'KICKED', 'REJECTED')
      AND nickname NOT LIKE '_%';

    -- 같은 클럽 내에서 중복되는 닉네임이 있는 경우 추가 처리
    -- (이론적으로 member_id는 고유하므로 발생하지 않지만, 안전을 위해)
    UPDATE club_members cm1
    INNER JOIN (
        SELECT club_id, nickname, COUNT(*) as cnt
        FROM club_members
        WHERE status IN ('LEFT', 'KICKED', 'REJECTED')
        GROUP BY club_id, nickname
        HAVING cnt > 1
    ) cm2 ON cm1.club_id = cm2.club_id AND cm1.nickname = cm2.nickname
    SET cm1.nickname = CASE
        WHEN LENGTH(CONCAT('_', CAST(cm1.member_id AS CHAR), '_', CAST(cm1.user_id AS CHAR))) <= 10
        THEN CONCAT('_', CAST(cm1.member_id AS CHAR), '_', CAST(cm1.user_id AS CHAR))
        ELSE SUBSTRING(CONCAT('_', CAST(cm1.member_id AS CHAR)), 1, 10)
    END
    WHERE cm1.status IN ('LEFT', 'KICKED', 'REJECTED');

    -- 닉네임 길이가 10자를 초과하는 경우 최종 처리
    UPDATE club_members
    SET nickname = SUBSTRING(nickname, 1, 10)
    WHERE status IN ('LEFT', 'KICKED', 'REJECTED')
      AND LENGTH(nickname) > 10;

    SET FOREIGN_KEY_CHECKS = 1;
END$$

DELIMITER ;

-- Stored Procedure 실행
CALL fix_inactive_member_nicknames();

-- Stored Procedure 삭제
DROP PROCEDURE IF EXISTS fix_inactive_member_nicknames;
