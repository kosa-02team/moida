-- 모임 커버 이미지 URL 컬럼 추가
ALTER TABLE clubs
ADD COLUMN cover_image_url VARCHAR(255) NULL;
