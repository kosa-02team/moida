-- V13: 모임(clubs) 테이블에 category 컬럼 추가
--
-- 변경사항:
-- 1. category 컬럼 추가 (STUDY/SPORTS/SOCIAL/HOBBY/FINANCE/ETC)
-- 2. category 인덱스 추가 (검색 성능 향상)

-- category 컬럼 추가
ALTER TABLE clubs
ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'ETC'
COMMENT 'STUDY: 스터디, SPORTS: 운동, SOCIAL: 친목, HOBBY: 취미, FINANCE: 재테크, ETC: 기타'
AFTER max_members;

-- category 인덱스 추가 (필터링 성능 향상)
CREATE INDEX idx_clubs_category ON clubs(category);

-- 복합 인덱스 (category + status) - 활성 상태별 카테고리 검색용
CREATE INDEX idx_clubs_category_status ON clubs(category, status);
