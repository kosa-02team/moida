-- UTF-8 인코딩 설정
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 은행 데이터 (모든 은행 코드 포함)
-- ============================================================
INSERT INTO banks (bank_code, bank_name, provider_class_name, is_active)
VALUES
    ('004', '국민은행', 'back.bank.provider.test.StubBankProvider', 1),
    ('003', '기업은행', 'back.bank.provider.test.StubBankProvider', 1),
    ('020', '우리은행', 'back.bank.provider.test.StubBankProvider', 1),
    ('088', '신한은행', 'back.bank.provider.test.StubBankProvider', 1),
    ('081', '하나은행', 'back.bank.provider.test.StubBankProvider', 1),
    ('011', 'NH농협은행', 'back.bank.provider.test.StubBankProvider', 1),
    ('089', '케이뱅크', 'back.bank.provider.test.StubBankProvider', 1),
    ('090', '카카오뱅크', 'back.bank.provider.test.StubBankProvider', 1),
    ('092', '토스뱅크', 'back.bank.provider.test.StubBankProvider', 1),
    ('STUB','오픈은행', 'back.bank.provider.test.StubBankProvider', 1)
ON DUPLICATE KEY UPDATE
                     bank_name = VALUES(bank_name),
                     provider_class_name = VALUES(provider_class_name),
                     is_active = VALUES(is_active);

-- ============================================================
-- 2. 사용자 데이터 (모든 상태 포함: ACTIVE, DELETED, BANNED)
-- ============================================================
-- 비밀번호 해시: '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy' (비밀번호: 123456789)
INSERT INTO users
(user_id, login_id, password, real_name, system_role, simple_password, status, created_at, updated_at, deleted_at, banned_at)
VALUES
    (27, 'gywjd@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '두효정', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (2, 'qoralsrms@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '백민근', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (3, 'qkrwjdals@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '박정민', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (4, 'whgksfla@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '조한림', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (5, 'rlawjdrhks@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '김정환', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (6, 'dltjgus@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '이서현', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (7, 'qkrwodn@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '박재우', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (8, 'whwmdgns@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '조승훈', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (9, 'rkddghals@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '강화민', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (10, 'rnjsgowus@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '권재현', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (11, 'rlagPwls@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '김혜진', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (12, 'dleotmd@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '이대승', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (13, 'whwoqvy@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '조재표', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (14, 'dbsthdus@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '윤소연', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (15, 'thdalswjs@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '송민선', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (16, 'dlfwoals@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '이재민', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
    (17, 'chlwlsgus@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '최진현', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL);

-- ============================================================
-- 3. 모임 데이터 (모든 상태, 공개설정, 타입, 카테고리 조합)
-- ============================================================
-- main_account_id는 UUID 형식이므로 UUID() 함수 사용
INSERT INTO clubs
(club_id, club_name, owner_id, main_account_id, invite_code, club_type, max_members, category, status, visibility, created_at, updated_at, closed_at)
VALUES
-- ACTIVE + PUBLIC
(1, '코사 러닝 모임', 27, UUID(), 'MOIDA0001', 'FAIR_SETTLEMENT', 50, 'STUDY', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
(2, '주말 운동 모임', 2, UUID(), 'MOIDA0002', 'OPERATION_FEE', 30, 'SPORTS', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
(3, '친목 모임', 3, UUID(), 'MOIDA0003', 'OPERATION_FEE', 100, 'SOCIAL', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
(4, '취미 공유 모임', 4, UUID(), 'MOIDA0004', 'OPERATION_FEE', 40, 'HOBBY', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
(5, '재테크 스터디', 5, UUID(), 'MOIDA0005', 'OPERATION_FEE', 25, 'FINANCE', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
(6, '자유 모임', 6, UUID(), 'MOIDA0006', 'OPERATION_FEE', 60, 'ETC', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),

-- ACTIVE + PRIVATE
(7, '비공개 스터디 모임', 7, UUID(), 'MOIDA0007', 'OPERATION_FEE', 20, 'STUDY', 'ACTIVE', 'PRIVATE', NOW(), NOW(), NULL),

-- ACTIVE + PUBLIC + FAIR_SETTLEMENT
(8, '공동 정산 모임', 8, UUID(), 'MOIDA0008', 'FAIR_SETTLEMENT', 50, 'SPORTS', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),

-- INACTIVE
(9, '독서 모임', 9, UUID(), 'MOIDA0009', 'OPERATION_FEE', 30, 'SOCIAL', 'INACTIVE', 'PUBLIC', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL),

-- CLOSED
(10, '배낭여행 모임', 10, UUID(), 'MOIDA0010', 'OPERATION_FEE', 20, 'HOBBY', 'ACTIVE', 'PUBLIC', DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY));

-- ============================================================
-- 4. 은행 계좌 데이터 (각 모임마다 계좌 생성)
-- ============================================================
INSERT INTO bank_accounts
(club_id, bank_code, user_id, bank_id, account_number, depositor_name, deleted_at, created_at, updated_at)
VALUES
    (1, 'STUB', 27, 10, '110-42-1000001', '두효정', NULL, NOW(), NOW()),
    (2, 'STUB', 2,  10, '110-42-1000002', '백민근', NULL, NOW(), NOW()),
    (3, 'STUB', 3,  10, '110-42-1000003', '박정민', NULL, NOW(), NOW()),
    (4, 'STUB', 4,  10, '110-42-1000004', '조한림', NULL, NOW(), NOW()),
    (5, 'STUB', 5,  10, '110-42-1000005', '김정환', NULL, NOW(), NOW()),
    (6, 'STUB', 6,  10, '110-42-1000006', '이서현', NULL, NOW(), NOW()),
    (7, 'STUB', 7,  10, '110-42-1000007', '박재우', NULL, NOW(), NOW()),
    (8, 'STUB', 8,  10, '110-42-1000008', '조승훈', NULL, NOW(), NOW()),
    (9, 'STUB', 9,  10, '110-42-1000009', '강화민', NULL, NOW(), NOW()),
    (10,'STUB',10, 10, '110-42-1000010', '권재현', NULL, NOW(), NOW());

-- ============================================================
-- 5. 모임 멤버십 데이터 (모든 역할과 상태 조합)
-- ============================================================
-- club_id=1 멤버들 (다양한 역할)
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
    (1, 27, '두효정', 'OWNER',       'ACTIVE', NOW(), NOW(), NOW()),
    (1, 2,  '백민근', 'ACCOUNTANT',  'ACTIVE', NOW(), NOW(), NOW()),
    (1, 3,  '박정민', 'STAFF',       'ACTIVE', NOW(), NOW(), NOW()),
    (1, 4,  '조한림', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW()),
    (1, 5,  '김정환', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW()),
    (1, 6,  '이서현', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW()),
    (1, 7,  '박재우', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW()),
    (1, 8,  '조승훈', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW()),
    (1, 9,  '강화민', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW()),
    (1, 10, '권재현', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW()),
    (1, 11, '김혜진', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW()),
    (1, 12, '이대승', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW()),
    (1, 13, '조재표', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW()),
    (1, 14, '윤소연', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW());
-- club 2
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
    (2, 27, '두효정', 'OWNER',       'ACTIVE', NOW(), NOW(), NOW()),
    (2, 2,  '백민근', 'ACCOUNTANT',  'ACTIVE', NOW(), NOW(), NOW()),
    (2, 3,  '박정민', 'STAFF',       'ACTIVE', NOW(), NOW(), NOW()),
    (2, 4,  '조한림', 'MEMBER',      'ACTIVE', NOW(), NOW(), NOW());

-- club 3
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
                             (3, 3,  '박정민', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (3, 4,  '조한림', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (3, 7,  '박재우', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (3, 9,  '강화민', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (3, 12, '이대승', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

-- club 4
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
                             (4, 4,  '조한림', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (4, 2,  '백민근', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (4, 6,  '이서현', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (4, 10, '권재현', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (4, 15, '송민선', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

-- club 5
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
                             (5, 5,  '김정환', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (5, 3,  '박정민', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (5, 8,  '조승훈', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (5, 13, '조재표', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (5, 16, '이재민', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

-- club 6
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
                             (6, 6,  '이서현', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (6, 2,  '백민근', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (6, 9,  '강화민', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (6, 14, '윤소연', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (6, 17, '최진현', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

-- club 7
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
                             (7, 7,  '박재우', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (7, 3,  '박정민', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (7, 5,  '김정환', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (7, 11, '김혜진', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (7, 16, '이재민', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

-- club 8
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
                             (8, 8,  '조승훈', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (8, 4,  '조한림', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (8, 6,  '이서현', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (8, 12, '이대승', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (8, 15, '송민선', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

-- club 9
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES

                             (9, 9,  '강화민', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (9, 2,  '백민근', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (9, 7,  '박재우', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (9, 13, '조재표', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (9, 17, '최진현', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

-- club 10
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
                             (10, 10, '권재현', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (10, 3,  '박정민', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (10, 5,  '김정환', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (10, 8,  '조승훈', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
                             (10, 14, '윤소연', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

-- club_id = 1 러닝 모임 스케줄 10개

UPDATE clubs
SET cover_image_url = '/uploads/images/profile/1.jpg'
WHERE club_id = 1;

UPDATE clubs
SET cover_image_url = '/uploads/images/profile/2.jpg'
WHERE club_id = 2;

UPDATE clubs
SET cover_image_url = '/uploads/images/profile/3.jpg'
WHERE club_id = 3;

UPDATE clubs
SET cover_image_url = '/uploads/images/profile/4.jpg'
WHERE club_id = 4;

UPDATE clubs
SET cover_image_url = '/uploads/images/profile/5.jpg'
WHERE club_id = 5;

UPDATE clubs
SET cover_image_url = '/uploads/images/profile/6.jpg'
WHERE club_id = 6;

UPDATE clubs
SET cover_image_url = '/uploads/images/profile/7.jpg'
WHERE club_id = 7;

UPDATE clubs
SET cover_image_url = '/uploads/images/profile/8.jpg'
WHERE club_id = 8;

UPDATE clubs
SET cover_image_url = '/uploads/images/profile/9.jpg'
WHERE club_id = 9;

UPDATE clubs
SET cover_image_url = '/uploads/images/profile/10.jpg'
WHERE club_id = 10;


INSERT INTO schedules
(club_id, schedule_name, event_date, end_date, location, description, entry_fee, vote_deadline, created_at, updated_at)
VALUES
-- 1. 오늘
(1,
 '북서울 꿈의 숲 컨디션 조절 러닝',
 DATE_SUB(CURDATE(), INTERVAL 0 DAY) + INTERVAL 19 HOUR,
 DATE_SUB(CURDATE(), INTERVAL 0 DAY) + INTERVAL 20 HOUR,
 '북서울 꿈의 숲',
 '북서울 꿈의 숲',
 30000,
 DATE_SUB(CURDATE(), INTERVAL 1 DAY),
 NOW(), NOW()),

-- 2. 7일 전
(1,
 '이촌 한강 야경 러닝',
 DATE_SUB(CURDATE(), INTERVAL 7 DAY) + INTERVAL 19 HOUR,
 DATE_SUB(CURDATE(), INTERVAL 7 DAY) + INTERVAL 20 HOUR,
 '이촌 한강공원',
 '이촌 한강 야경 러닝',
 30000,
 DATE_SUB(CURDATE(), INTERVAL 8 DAY),
 NOW(), NOW()),

-- 3. 14일 전
(1,
 '올림픽공원 스트레칭 실패 러닝',
 DATE_SUB(CURDATE(), INTERVAL 14 DAY) + INTERVAL 19 HOUR,
 DATE_SUB(CURDATE(), INTERVAL 14 DAY) + INTERVAL 20 HOUR,
 '올림픽공원',
 '올림픽공원',
 30000,
 DATE_SUB(CURDATE(), INTERVAL 15 DAY),
 NOW(), NOW()),

-- 4. 21일 전
(1,
 '경의선 숲길 수다 러닝',
 DATE_SUB(CURDATE(), INTERVAL 21 DAY) + INTERVAL 19 HOUR,
 DATE_SUB(CURDATE(), INTERVAL 21 DAY) + INTERVAL 20 HOUR,
 '경의선 숲길',
 '경의선 숲길 수다 러닝',
 30000,
 DATE_SUB(CURDATE(), INTERVAL 22 DAY),
 NOW(), NOW()),

-- 5. 28일 전
(1,
 '양재천 팀 러닝',
 DATE_SUB(CURDATE(), INTERVAL 28 DAY) + INTERVAL 19 HOUR,
 DATE_SUB(CURDATE(), INTERVAL 28 DAY) + INTERVAL 20 HOUR,
 '양재천 산책로',
 '양재천 팀 러닝',
 30000,
 DATE_SUB(CURDATE(), INTERVAL 29 DAY),
 NOW(), NOW()),

-- 6. 35일 전
(1,
 '망원 한강 러닝 & 치킨 논쟁',
 DATE_SUB(CURDATE(), INTERVAL 35 DAY) + INTERVAL 19 HOUR,
 DATE_SUB(CURDATE(), INTERVAL 35 DAY) + INTERVAL 20 HOUR,
 '망원 한강공원',
 '망원 한강공원',
 30000,
 DATE_SUB(CURDATE(), INTERVAL 36 DAY),
 NOW(), NOW()),

-- 7. 42일 전
(1,
 '석촌호수 초보자 러닝',
 DATE_SUB(CURDATE(), INTERVAL 42 DAY) + INTERVAL 19 HOUR,
 DATE_SUB(CURDATE(), INTERVAL 42 DAY) + INTERVAL 20 HOUR,
 '석촌호수',
 '석촌호수',
 30000,
 DATE_SUB(CURDATE(), INTERVAL 43 DAY),
 NOW(), NOW()),

-- 8. 49일 전
(1,
 '서울숲 페이스 조절 러닝',
 DATE_SUB(CURDATE(), INTERVAL 49 DAY) + INTERVAL 19 HOUR,
 DATE_SUB(CURDATE(), INTERVAL 49 DAY) + INTERVAL 20 HOUR,
 '서울숲 러닝 코스',
 '서울숲 러닝 코스',
 30000,
 DATE_SUB(CURDATE(), INTERVAL 50 DAY),
 NOW(), NOW()),

-- 9. 56일 전
(1,
 '반포 한강 러닝',
 DATE_SUB(CURDATE(), INTERVAL 56 DAY) + INTERVAL 19 HOUR,
 DATE_SUB(CURDATE(), INTERVAL 56 DAY) + INTERVAL 20 HOUR,
 '반포 한강공원',
 '',
 30000,
 DATE_SUB(CURDATE(), INTERVAL 57 DAY),
 NOW(), NOW()),

-- 10. 63일 전
(1,
 '여의도 한강공원 저녁 러닝',
 DATE_SUB(CURDATE(), INTERVAL 63 DAY) + INTERVAL 19 HOUR,
 DATE_SUB(CURDATE(), INTERVAL 63 DAY) + INTERVAL 20 HOUR,
 '여의도 한강공원',
 '여의도 공원 한 바퀴. 러닝 후 편의점에서 아이스크림 먹으며 수다.',
 30000,
 DATE_SUB(CURDATE(), INTERVAL 64 DAY),
 NOW(), NOW());

INSERT INTO posts
(club_id, writer_id, category, title, content)
VALUES
    (
        5,
        5, -- club_members 테이블의 PK (writer_id)
        'GENERAL',
        '삼성, 하이닉스 가봅시다',
        '다들 많이 사세요 더 오를거에요'
    );

INSERT INTO posts
(club_id, writer_id, category, title, content)
VALUES
    (
        5,
        5, -- club_members 테이블의 PK (writer_id)
        'GENERAL',
        '국내에서 제일 많이 산 회사는 구글이래요',
        '다들 뭐 사셨어요'
    );

INSERT INTO posts (club_id, writer_id, category, title, content, schedule_id, place, created_at, updated_at)
VALUES
(1, 27, 'GENERAL', '오늘은 여의도 공원 한 바퀴', '오늘은 여의도 공원 한 바퀴.\n\n초반엔 다들 말 많다가 2km 지나니까 말수 급감.\n\n끝나고 편의점에서 아이스크림 하나씩 들고 앉아있는데,\n\n“뛰는 것보다 이 시간이 더 좋다”는 말에 다들 고개 끄덕임.', 1, '여의도공원', NOW(), NOW()),

(1, 2, 'GENERAL', '러닝 전에 커피 마시면 안 된다고', '러닝 전에 커피 마시면 안 된다고 했는데\n\n결국 두 명이나 아이스 아메리카노 들고 등장.\n\n뛰다가 배 아프다며 중간에 화장실 찾느라 코스 이탈.\n\n다음부턴 커피 금지로 합의.', 2, '한강공원', NOW(), NOW()),

(1, 3, 'GENERAL', '비 온 뒤라 공기가 좋아서', '비 온 뒤라 공기가 좋아서 속도 욕심냈다가\n\n후반에 다 같이 페이스 무너짐.\n\n마지막 500m는 걷다 뛰다 반복.\n\n그래도 끝나고 사진 찍을 땐 다들 웃고 있음.', 3, '잠수교', NOW(), NOW()),

(1, 4, 'GENERAL', '오늘은 초보자도 있어서', '오늘은 초보자도 있어서 속도 낮춰서 진행.\n\n옆에서 계속 “지금 괜찮죠?” 물어보는 배려 덕분에\n\n처음 나온 사람도 끝까지 완주.\n\n러닝은 역시 같이 해야 오래 가는 듯.', 4, '올림픽공원', NOW(), NOW()),

(1, 5, 'GENERAL', '뛰고 나서 치킨 얘기만', '뛰고 나서 치킨 얘기만 10분째.\n\n“운동했으니까 괜찮다” vs “이러면 뭐 하러 뛰냐”.\n\n결국 반반 나뉘어서 치킨 팀, 귀가 팀으로 해산.', 5, '망원한강공원', NOW(), NOW()),

(1, 6, 'GENERAL', '이어폰 끼고 혼자 뛰는 사람', '이어폰 끼고 혼자 뛰는 사람,\n\n끝까지 옆에서 맞춰주는 사람,\n\n사진 담당까지 역할이 자연스럽게 나뉨.\n\n말 안 해도 굴러가는 게 이제 팀 같다.', 6, '반포한강공원', NOW(), NOW()),

(1, 7, 'GENERAL', '오늘은 러닝보다 수다', '오늘은 러닝보다 수다 비중이 더 높았던 날.\n\n속도는 느렸지만 시간은 제일 빨리 감.\n\n땀보다 웃음이 더 많이 난 러닝.', 7, '뚝섬유원지', NOW(), NOW()),

(1, 8, 'GENERAL', '출발 전에 스트레칭 대충', '출발 전에 스트레칭 대충 했다가\n\n첫 1km에서 다리 뻐근함 호소자 속출.\n\n다음 모임부터는 스트레칭 담당 지정하기로 결정.', 8, '석촌호수', NOW(), NOW()),

(1, 9, 'GENERAL', '끝나고 강변에서 야경', '끝나고 강변에서 야경 보면서 잠깐 멍 때림.\n\n누가 먼저랄 것도 없이 사진 찍고 공유.\n\n러닝이 핑계고, 사실 이 분위기가 좋은 듯.', 9, '청계천', NOW(), NOW()),

(1, 10, 'GENERAL', '오늘은 유독 컨디션', '오늘은 유독 컨디션 안 좋은 사람이 많았던 날.\n\n그래서 목표 거리 줄이고 일찍 종료.\n\n“이런 날도 있어야 오래 한다”는 말이 오늘의 명언.', 10, '서울숲', NOW(), NOW());



SET FOREIGN_KEY_CHECKS = 1;

