-- 1. 모임 멤버 테이블
DROP TABLE club_members;
CREATE TABLE club_members (
                              member_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '멤버 식별자',
                              club_id BIGINT NOT NULL COMMENT '소속 모임 식별자',
                              user_id BIGINT NOT NULL COMMENT '회원 ID',
                              club_nickname VARCHAR(50) NOT NULL COMMENT '모임 내 별칭',

                              role VARCHAR(100) DEFAULT 'MEMBER' COMMENT '역할: OWNER(모임장),STAFF(운영진),ACCOUNTANT(총무),MEMBER(일반 회원)',

                              status VARCHAR(20) DEFAULT 'PENDING' COMMENT '상태: PENDING(기본: 승인 대기),ACTIVE(활동),LEFT(탈퇴),KICKED(강퇴), REJECTED(가입 거절)',

                              joined_at DATETIME COMMENT '가입 승인 시점',
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '레코드 생성일',
                              updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '레코드 수정일',

                              UNIQUE KEY uk_club_user (club_id, user_id),

                              CONSTRAINT fk_member_club FOREIGN KEY (club_id)
                                  REFERENCES clubs(club_id) ON DELETE CASCADE,
                              CONSTRAINT fk_member_user FOREIGN KEY (user_id)
                                  REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;