SET FOREIGN_KEY_CHECKS = 0;

-- 은행
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

-- 모임장 계정 (user_id = 2, pwd = 123456789)
INSERT INTO users
(user_id, login_id, password, real_name, system_role, simple_password, status, created_at, updated_at, deleted_at, banned_at)
VALUES
(1, 'gywjd@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '두효정', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL);

-- 모임 (club_id = 1)
INSERT INTO clubs
(club_id, name, owner_id, main_account_id, invite_code, status, visibility, created_at, updated_at, closed_at)
VALUES
(1, '1번 모임', 1, 1, 'MOIDA0001', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL);

-- 은행 계좌생성(club_id = 1)
INSERT INTO bank_accounts
(club_id, bank_code, user_id, bank_id, account_number, depositor_name, deleted_at, created_at, updated_at)
VALUES
(1, 'STUB', 1, 10, '123456789', '두효정', null, NOW(), NOW());

-- 모임장 멤버십 등록 (member_id는 auto_increment라 생략 권장)
INSERT INTO club_members
(club_id, user_id, club_nickname, role, status, joined_at, created_at, updated_at)
VALUES
(1, 1, '두효정', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;


-- 멤버 계정 20개 (user_id: 2 ~ 21)
-- 비밀번호 해시: '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy' 로 통일

INSERT INTO users
(user_id, login_id, password, real_name, system_role, simple_password, status, created_at, updated_at, deleted_at, banned_at)
VALUES
(2,  'member02@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '김민준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(3,  'member03@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '이서준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(4,  'member04@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '박지훈', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(5,  'member05@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '최서연', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(6,  'member06@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '정하준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(7,  'member07@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '한지민', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(8,  'member08@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '오세훈', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(9,  'member09@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '윤아린', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(10, 'member10@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '장도윤', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(11, 'member11@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '임수현', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(12, 'member12@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '신유진', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(13, 'member13@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '조현우', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(14, 'member14@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '강태현', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(15, 'member15@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '서지안', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(16, 'member16@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '홍지수', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(17, 'member17@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '문예준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(18, 'member18@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '유채원', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(19, 'member19@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '배준호', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(20, 'member20@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '고은서', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(21, 'member21@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '남도현', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL);

-- club_id=1에 멤버십 등록 (role/status는 필요에 맞게 조정)
INSERT INTO club_members
(club_id, user_id, club_nickname, role, status, joined_at, created_at, updated_at)
VALUES
(1, 2,  '김민준', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 3,  '이서준', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 4,  '박지훈', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 5,  '최서연', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 6,  '정하준', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 7,  '한지민', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 8,  '오세훈', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 9,  '윤아린', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 10, '장도윤', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 11, '임수현', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 12, '신유진', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 13, '조현우', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 14, '강태현', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 15, '서지안', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 16, '홍지수', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 17, '문예준', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 18, '유채원', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 19, '배준호', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 20, '고은서', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 21, '남도현', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

INSERT INTO schedules
(
    schedule_id, club_id, schedule_name, event_date, end_date,
    location, description, entry_fee, total_spent, refund_per_person,
    status, vote_deadline, closed_at, cancel_reason, created_at, updated_at
)
VALUES
-- 1. [진행 중] 다가오는 1월 정기 모임 (참가비 3만원)
(1, 1, '1월 신년회', '2025-01-25 18:00:00', '2025-01-25 22:00:00',
'강남역 맛집', '다같이 모여서 신년회 합시다!', 30000.00, 0, 0,
'OPEN', '2025-01-20 23:59:59', NULL, NULL, NOW(), NOW());