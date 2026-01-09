-- V5: 스키마 업데이트
--
-- 주요 변경사항:
-- 1. 멤버 태그 테이블 추가
-- 2. posts에 삭제한 ID 컬럼 추가

SET FOREIGN_KEY_CHECKS = 0;
-- 1. banks 테이블 추가 (오픈 뱅킹 API 대안 구조)
CREATE TABLE IF NOT EXISTS post_member_tags (
    post_member_tag_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT ,
    member_id BIGINT,
    FOREIGN KEY (post_id) REFERENCES posts(post_id),
    FOREIGN KEY (member_id) REFERENCES club_members(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE posts
ADD COLUMN deleted_by BIGINT NULL,
ADD COLUMN place VARCHAR(30) NULL;

SET FOREIGN_KEY_CHECKS = 1;
