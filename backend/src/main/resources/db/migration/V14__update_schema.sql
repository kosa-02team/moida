-- 1. bank_transaction_history 입출금 컬럼 추가
-- 2. posts 외래키 변경

ALTER TABLE bank_transaction_history
ADD COLUMN inout_type VARCHAR(20) NOT NULL;

ALTER TABLE posts
DROP FOREIGN KEY posts_ibfk_2;
ALTER TABLE posts
ADD CONSTRAINT fk_posts_writer_id
FOREIGN KEY (writer_id)
REFERENCES club_members (member_id);