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
-- ACTIVE 사용자들
(27, 'gywjd@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '두효정', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(2, 'member02@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '김민준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(3, 'member03@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '이서준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(4, 'member04@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '박지훈', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(5, 'member05@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '최서연', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(6, 'member06@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '정하준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(7, 'member07@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '한지민', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(8, 'member08@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '오세훈', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(9, 'member09@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '윤아린', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(10, 'member10@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '장도윤', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(11, 'member11@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '임수현', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(12, 'member12@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '신유진', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(13, 'member13@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '조현우', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(14, 'member14@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '강태현', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(15, 'member15@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '서지안', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(16, 'member16@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '홍지수', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(17, 'member17@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '문예준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(18, 'member18@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '유채원', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(19, 'member19@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '배준호', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(20, 'member20@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '고은서', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(21, 'member21@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '남도현', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
-- DELETED 사용자
(22, 'deleted01@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '탈퇴한사용자1', 'USER', NULL, 'DELETED', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), NULL),
(23, 'deleted02@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '탈퇴한사용자2', 'USER', NULL, 'DELETED', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NULL),
-- BANNED 사용자
(24, 'banned01@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '정지된사용자1', 'USER', NULL, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY), NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(25, 'banned02@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '정지된사용자2', 'USER', NULL, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 45 DAY), NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- ADMIN 사용자
(26, 'admin@moida.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '시스템관리자', 'ADMIN', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(28, 'member28@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '이서준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL);

-- ============================================================
-- 3. 모임 데이터 (모든 상태, 공개설정, 타입, 카테고리 조합)
-- ============================================================
-- main_account_id는 UUID 형식이므로 UUID() 함수 사용
INSERT INTO clubs
(club_id, club_name, owner_id, main_account_id, invite_code, club_type, max_members, category, status, visibility, created_at, updated_at, closed_at)
VALUES
-- ACTIVE + PUBLIC + OPERATION_FEE + 각 카테고리
(1, '공정정산운동모임', 27, UUID(), 'MOIDA0001', 'FAIR_SETTLEMENT', 50, 'STUDY', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
(2, '운영비운동모임', 2, UUID(), 'MOIDA0002', 'OPERATION_FEE', 30, 'SPORTS', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
(3, '운영비친목모임', 3, UUID(), 'MOIDA0003', 'OPERATION_FEE', 100, 'SOCIAL', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
(4, '운영비취미모임', 4, UUID(), 'MOIDA0004', 'OPERATION_FEE', 40, 'HOBBY', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
(5, '운영비재테크모임', 5, UUID(), 'MOIDA0005', 'OPERATION_FEE', 25, 'FINANCE', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
(6, '운영비기타모임', 6, UUID(), 'MOIDA0006', 'OPERATION_FEE', 60, 'ETC', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
-- ACTIVE + PRIVATE + OPERATION_FEE
(7, '비공개운영비모임', 7, UUID(), 'MOIDA0007', 'OPERATION_FEE', 20, 'STUDY', 'ACTIVE', 'PRIVATE', NOW(), NOW(), NULL),
-- ACTIVE + PUBLIC + FAIR_SETTLEMENT
(8, '공정정산모임', 8, UUID(), 'MOIDA0008', 'FAIR_SETTLEMENT', 50, 'SPORTS', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
-- INACTIVE 모임
(9, '비활성모임', 9, UUID(), 'MOIDA0009', 'OPERATION_FEE', 30, 'SOCIAL', 'INACTIVE', 'PUBLIC', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL),
-- CLOSED 모임 (closed_at이 있는 경우)
(10, '폐쇄된모임', 10, UUID(), 'MOIDA0010', 'OPERATION_FEE', 20, 'HOBBY', 'ACTIVE', 'PUBLIC', DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY));

-- ============================================================
-- 4. 은행 계좌 데이터 (각 모임마다 계좌 생성)
-- ============================================================
INSERT INTO bank_accounts
(club_id, bank_code, user_id, bank_id, account_number, depositor_name, deleted_at, created_at, updated_at)
VALUES
    (1, 'STUB', 27, 10, '110-42-1000001', '두효정', NULL, NOW(), NOW()),
    (2, 'STUB', 2,  10, '110-42-1000002', '김민준', NULL, NOW(), NOW()),
    (3, 'STUB', 3,  10, '110-42-1000003', '이서준', NULL, NOW(), NOW()),
    (4, 'STUB', 4,  10, '110-42-1000004', '박지훈', NULL, NOW(), NOW()),
    (5, 'STUB', 5,  10, '110-42-1000005', '최서연', NULL, NOW(), NOW()),
    (6, 'STUB', 6,  10, '110-42-1000006', '정하준', NULL, NOW(), NOW()),
    (7, 'STUB', 7,  10, '110-42-1000007', '한지민', NULL, NOW(), NOW()),
    (8, 'STUB', 8,  10, '110-42-1000008', '오세훈', NULL, NOW(), NOW()),
    (9, 'STUB', 9,  10, '110-42-1000009', '윤아린', NULL, NOW(), NOW()),
    (10,'STUB',10, 10, '110-42-1000010', '장도윤', NULL, NOW(), NOW());

-- ============================================================
-- 5. 모임 멤버십 데이터 (모든 역할과 상태 조합)
-- ============================================================
-- club_id=1 멤버들 (다양한 역할)
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
    (1, 27, '두효정', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
    (1, 2, '김민준', 'ACCOUNTANT', 'ACTIVE', NOW(), NOW(), NOW()),
    (1, 3, '이서준', 'STAFF', 'ACTIVE', NOW(), NOW(), NOW()),
    (1, 28, '쭈니', 'STAFF', 'ACTIVE', NOW(), NOW(), NOW()),
    (1, 4, '박지훈', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
    (1, 5, '최서연', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
    (1, 6, '정하준', 'MEMBER', 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (1, 7, '한지민', 'MEMBER', 'REJECTED', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (1, 8, '오세훈', 'MEMBER', 'LEFT', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
    (1, 9, '윤아린', 'MEMBER', 'KICKED', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1, 10, '장도윤', 'MEMBER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY));

-- club_id=2 멤버들
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
    (2, 2, '김민준', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
    (2, 11, '임수현', 'STAFF', 'ACTIVE', NOW(), NOW(), NOW()),
    (2, 12, '신유진', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

-- club_id=3 멤버들
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
    (3, 3, '이서준', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
    (3, 13, '조현우', 'ACCOUNTANT', 'ACTIVE', NOW(), NOW(), NOW()),
    (3, 14, '강태현', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

-- club_id=8 (FAIR_SETTLEMENT 타입) 멤버들
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
    (8, 8, '오세훈', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
    (8, 15, '서지안', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
    (8, 16, '홍지수', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;