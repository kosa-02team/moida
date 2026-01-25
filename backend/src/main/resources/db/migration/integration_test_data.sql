-- ============================================================
-- 전제: Flyway 마이그레이션(V1~V17)으로 스키마가 적용된 DB에서만 실행.
--       단, banks 테이블은 없으면 자동 생성됨.
-- ============================================================

-- UTF-8 인코딩 설정
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

-- 기존 테스트 데이터 삭제 (중복 방지)
DELETE FROM club_members WHERE club_id IN (1, 2, 3, 8);
DELETE FROM bank_accounts WHERE club_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
DELETE FROM clubs WHERE club_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
DELETE FROM users WHERE user_id IN (2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28);

-- ============================================================
-- 1. 은행 데이터 (모든 은행 코드 포함)
-- ============================================================
-- banks 테이블이 없으면 생성 (V2 스키마와 동일)
CREATE TABLE IF NOT EXISTS banks (
    bank_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_code VARCHAR(10) NOT NULL UNIQUE COMMENT '은행 코드',
    bank_name VARCHAR(50) NOT NULL COMMENT '은행 이름',
    provider_class_name VARCHAR(255) COMMENT 'Provider 클래스명',
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    ('STUB', '오픈은행', 'back.bank.provider.test.StubBankProvider', 1)
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
(1, 'gywjd@naver.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '두효정', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(2, 'member02@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '김민준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(3, 'member03@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '이서준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(4, 'member04@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '박지훈', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(5, 'member05@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '최서연', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(6, 'member06@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '정하준', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(7, 'member07@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '한지민', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(8, 'member08@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '오세훈', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
(9, 'member09@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '윤아린', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
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
(21, 'member21@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '남도현', 'USER', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL),
-- DELETED 사용자
(22, 'deleted01@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '탈퇴한사용자1', 'USER', NULL, 'DELETED', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), NULL),
(23, 'deleted02@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '탈퇴한사용자2', 'USER', NULL, 'DELETED', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NULL),
-- BANNED 사용자
(24, 'banned01@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '정지된사용자1', 'USER', NULL, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY), NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(25, 'banned02@example.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '정지된사용자2', 'USER', NULL, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 45 DAY), NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- ADMIN 사용자
(26, 'admin@moida.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '시스템관리자', 'ADMIN', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL);

-- ============================================================
-- 3. 모임 데이터 (모든 상태, 공개설정, 타입, 카테고리 조합)
-- ============================================================
-- main_account_id는 UUID 형식이므로 UUID() 함수 사용
INSERT INTO clubs
(club_id, club_name, owner_id, main_account_id, invite_code, club_type, max_members, category, status, visibility, created_at, updated_at, closed_at)
VALUES
-- ACTIVE + PUBLIC + OPERATION_FEE + 각 카테고리
(1, '운영비스터디모임', 1, UUID(), 'MOIDA0001', 'OPERATION_FEE', 50, 'STUDY', 'ACTIVE', 'PUBLIC', NOW(), NOW(), NULL),
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
(1, 'STUB', 1, 10, '111111111', '두효정', NULL, NOW(), NOW()),
(2, 'STUB', 2, 10, '222222222', '김민준', NULL, NOW(), NOW()),
(3, 'STUB', 3, 10, '333333333', '이서준', NULL, NOW(), NOW()),
(4, 'STUB', 4, 10, '444444444', '박지훈', NULL, NOW(), NOW()),
(5, 'STUB', 5, 10, '555555555', '최서연', NULL, NOW(), NOW()),
(6, 'STUB', 6, 10, '666666666', '정하준', NULL, NOW(), NOW()),
(7, 'STUB', 7, 10, '777777777', '한지민', NULL, NOW(), NOW()),
(8, 'STUB', 8, 10, '888888888', '오세훈', NULL, NOW(), NOW()),
(9, 'STUB', 9, 10, '999999999', '윤아린', NULL, NOW(), NOW()),
(10, 'STUB', 10, 10, '101010101', '장도윤', NULL, NOW(), NOW());

-- ============================================================
-- 5. 모임 멤버십 데이터 (모든 역할과 상태 조합)
-- ============================================================
-- club_id=1 멤버들 (다양한 역할)
INSERT INTO club_members
(club_id, user_id, nickname, role, status, joined_at, created_at, updated_at)
VALUES
(1, 1, '두효정', 'OWNER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 2, '김민준', 'ACCOUNTANT', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 3, '이서준', 'STAFF', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 4, '박지훈', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 5, '최서연', 'MEMBER', 'ACTIVE', NOW(), NOW(), NOW()),
(1, 6, '정하준', 'MEMBER', 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 7, '한지민', 'MEMBER', 'REJECTED', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 8, '오세훈', 'MEMBER', 'LEFT', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
(1, 9, '윤아린', 'MEMBER', 'KICKED', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY));

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

-- ============================================================
-- 6. 일정 데이터 (모든 상태: OPEN, CLOSED, CANCELLED)
-- ============================================================
INSERT INTO schedules
(schedule_id, club_id, schedule_name, event_date, end_date, location, description, entry_fee, total_spent, refund_per_person, status, vote_deadline, closed_at, cancel_reason, created_at, updated_at)
VALUES
-- OPEN 상태 일정들
(1, 1, '1월 신년회', '2025-01-25 18:00:00', '2025-01-25 22:00:00', '강남역 맛집', '다같이 모여서 신년회 합시다!', 30000.00, 0, 0, 'OPEN', '2025-01-20 23:59:59', NULL, NULL, NOW(), NOW()),
(2, 1, '2월 정기모임', '2025-02-15 19:00:00', '2025-02-15 22:00:00', '홍대 카페', '2월 정기 모임입니다', 20000.00, 0, 0, 'OPEN', '2025-02-10 23:59:59', NULL, NULL, NOW(), NOW()),
(3, 2, '주말 등산', '2025-01-20 08:00:00', '2025-01-20 18:00:00', '북한산', '주말 등산 모임', 15000.00, 0, 0, 'OPEN', '2025-01-18 23:59:59', NULL, NULL, NOW(), NOW()),
-- CLOSED 상태 일정 (마감됨)
(4, 1, '12월 송년회', '2024-12-30 18:00:00', '2024-12-30 23:00:00', '강남 레스토랑', '송년회 모임', 50000.00, 450000.00, 45000.00, 'CLOSED', '2024-12-25 23:59:59', '2024-12-30 23:00:00', NULL, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 2, '11월 정기모임', '2024-11-20 19:00:00', '2024-11-20 22:00:00', '홍대', '11월 모임', 25000.00, 200000.00, 25000.00, 'CLOSED', '2024-11-15 23:59:59', '2024-11-20 22:00:00', NULL, DATE_SUB(NOW(), INTERVAL 50 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),
-- CANCELLED 상태 일정 (취소됨)
(6, 1, '취소된일정', '2025-01-10 18:00:00', '2025-01-10 22:00:00', '강남', '취소된 일정', 20000.00, 0, 0, 'CANCELLED', '2025-01-05 23:59:59', DATE_SUB(NOW(), INTERVAL 3 DAY), '참석 인원 부족으로 취소', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(7, 3, '취소된모임', '2025-01-15 19:00:00', '2025-01-15 22:00:00', '홍대', '취소됨', 15000.00, 0, 0, 'CANCELLED', '2025-01-10 23:59:59', DATE_SUB(NOW(), INTERVAL 2 DAY), '날씨 악화로 취소', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ============================================================
-- 7. 게시글 데이터 (모든 카테고리: GENERAL, SCHEDULE, SETTLEMENT, VOTE)
-- ============================================================
INSERT INTO posts
(post_id, club_id, writer_id, category, title, content, schedule_id, place, created_at, updated_at, deleted_at)
VALUES
-- GENERAL 카테고리
(1, 1, 1, 'GENERAL', '일반 게시글 제목1', '일반 게시글 내용입니다', NULL, NULL, NOW(), NOW(), NULL),
(2, 1, 2, 'GENERAL', '일반 게시글 제목2', '또 다른 일반 게시글', NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
-- SCHEDULE 카테고리 (일정과 연결)
(3, 1, 1, 'SCHEDULE', '1월 신년회 일정', '1월 신년회 일정 게시글입니다', 1, '강남역 맛집', NOW(), NOW(), NULL),
(4, 1, 1, 'SCHEDULE', '2월 정기모임 일정', '2월 정기모임 게시글', 2, '홍대 카페', NOW(), NOW(), NULL),
-- SETTLEMENT 카테고리
(5, 1, 2, 'SETTLEMENT', '12월 송년회 정산', '12월 송년회 정산 내역입니다', 4, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL),
-- VOTE 카테고리
(6, 1, 1, 'VOTE', '투표 게시글', '투표를 진행합니다', NULL, NULL, NOW(), NOW(), NULL),
-- 삭제된 게시글
(7, 1, 3, 'GENERAL', '삭제된 게시글', '이 게시글은 삭제되었습니다', NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));

-- ============================================================
-- 8. 댓글 데이터
-- ============================================================
INSERT INTO comments
(comment_id, post_id, writer_id, content, created_at, updated_at, deleted_at)
VALUES
(1, 1, 2, '좋은 게시글이네요!', NOW(), NOW(), NULL),
(2, 1, 3, '동의합니다', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(3, 3, 2, '참석하겠습니다!', NOW(), NOW(), NULL),
(4, 3, 4, '저도 참석합니다', NOW(), NOW(), NULL),
(5, 5, 1, '정산 확인했습니다', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
-- 삭제된 댓글
(6, 1, 4, '삭제된 댓글', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ============================================================
-- 9. 투표 데이터 (모든 타입: GENERAL, ATTENDANCE / 상태: OPEN, CLOSED)
-- ============================================================
INSERT INTO votes
(vote_id, post_id, vote_type, schedule_id, creator_id, title, description, is_anonymous, allow_multiple, status, closed_at, deadline, created_at, updated_at)
VALUES
-- GENERAL 타입 + OPEN 상태
(1, 6, 'GENERAL', NULL, 1, '일반 투표 제목', '일반 투표 설명입니다', FALSE, TRUE, 'OPEN', NULL, DATE_ADD(NOW(), INTERVAL 7 DAY), NOW(), NOW()),
-- ATTENDANCE 타입 + OPEN 상태 (일정과 연결)
(2, 3, 'ATTENDANCE', 1, 1, '1월 신년회 참석 투표', '참석 여부를 투표해주세요', FALSE, FALSE, 'OPEN', NULL, '2025-01-20 23:59:59', NOW(), NOW()),
-- GENERAL 타입 + CLOSED 상태
(3, NULL, 'GENERAL', NULL, 1, '종료된 투표', '이미 종료된 투표입니다', FALSE, FALSE, 'CLOSED', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- ATTENDANCE 타입 + CLOSED 상태
(4, NULL, 'ATTENDANCE', 4, 1, '12월 송년회 참석 투표', '종료된 참석 투표', FALSE, FALSE, 'CLOSED', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY));

-- 투표 옵션 데이터
INSERT INTO vote_options
(option_id, vote_id, option_text, created_at, updated_at)
VALUES
(1, 1, '옵션 1', NOW(), NOW()),
(2, 1, '옵션 2', NOW(), NOW()),
(3, 1, '옵션 3', NOW(), NOW()),
(4, 2, '참석', NOW(), NOW()),
(5, 2, '불참', NOW(), NOW()),
(6, 3, '옵션 A', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(7, 3, '옵션 B', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(8, 4, '참석', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),
(9, 4, '불참', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY));

-- 투표 기록 데이터
INSERT INTO vote_records
(record_id, vote_id, option_id, user_id, voted_at)
VALUES
(1, 1, 1, 2, NOW()),
(2, 1, 2, 3, NOW()),
(3, 2, 4, 2, NOW()),
(4, 2, 4, 3, NOW()),
(5, 2, 5, 4, NOW()),
(6, 3, 6, 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(7, 3, 6, 2, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(8, 4, 8, 1, DATE_SUB(NOW(), INTERVAL 28 DAY)),
(9, 4, 8, 2, DATE_SUB(NOW(), INTERVAL 27 DAY));

-- ============================================================
-- 10. 신고 데이터 (모든 상태: PENDING, REVIEWING, RESOLVED, DISMISSED)
-- ============================================================
INSERT INTO reports
(report_id, club_id, reporter_id, target_id, reason, photo_url, status, created_at, updated_at)
VALUES
-- PENDING 상태 신고
(1, 1, 2, 3, '부적절한 게시글 작성', NULL, 'PENDING', NOW(), NOW()),
(2, 1, 4, 5, '욕설 사용', NULL, 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- REVIEWING 상태 신고
(3, 1, 1, 6, '스팸 게시글', NULL, 'REVIEWING', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- RESOLVED 상태 신고
(4, 2, 2, 7, '부적절한 행동', NULL, 'RESOLVED', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- DISMISSED 상태 신고
(5, 1, 3, 8, '오신고', NULL, 'DISMISSED', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ============================================================
-- 11. 거래 내역 데이터 (DEPOSIT, WITHDRAW)
-- ============================================================
INSERT INTO transaction_log
(log_id, club_id, account_id, transaction_type, amount, description, editor_id, created_at, updated_at)
VALUES
-- DEPOSIT 거래
(1, 1, 1, 'DEPOSIT', 300000.00, '회비 입금', 1, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2, 1, 1, 'DEPOSIT', 50000.00, '참가비 입금', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 2, 2, 'DEPOSIT', 200000.00, '회비 입금', 2, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
-- WITHDRAW 거래
(4, 1, 1, 'WITHDRAW', 150000.00, '일정 비용 지출', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5, 1, 1, 'WITHDRAW', 50000.00, '모임 장소 대여비', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 2, 2, 'WITHDRAW', 100000.00, '운동 장비 구매', 2, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ============================================================
-- 12. 입금 요청 데이터 (모든 상태: PENDING, MATCHED, EXPIRED / 모든 타입: MEMBERSHIP_FEE, SETTLEMENT, DEPOSIT)
-- ============================================================
INSERT INTO payment_requests
(request_id, club_id, member_id, member_name, request_type, expected_amount, expected_date, match_days_range, status, match_type, matched_history_id, created_at, expires_at, matched_at, matched_by, schedule_id, billing_period)
VALUES
-- PENDING 상태 + MEMBERSHIP_FEE 타입
(1, 1, 4, '박지훈', 'MEMBERSHIP_FEE', 50000.00, CURDATE(), 10, 'PENDING', NULL, NULL, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), NULL, NULL, NULL, '2025-01'),
-- PENDING 상태 + SETTLEMENT 타입
(2, 1, 5, '최서연', 'SETTLEMENT', 30000.00, CURDATE(), 10, 'PENDING', NULL, NULL, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), NULL, NULL, 1, NULL),
-- MATCHED 상태 + DEPOSIT 타입
(3, 1, 2, '김민준', 'DEPOSIT', 50000.00, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 10, 'MATCHED', 'AUTO_MATCHED', 2, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, NULL, NULL),
-- MATCHED 상태 + MEMBERSHIP_FEE 타입 (수동 확인)
(4, 2, 11, '임수현', 'MEMBERSHIP_FEE', 30000.00, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 10, 'MATCHED', 'CONFIRMED', 3, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 2, NULL, '2025-01'),
-- EXPIRED 상태
(5, 1, 6, '정하준', 'MEMBERSHIP_FEE', 50000.00, DATE_SUB(CURDATE(), INTERVAL 15 DAY), 10, 'EXPIRED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 15 DAY), INTERVAL 7 DAY), NULL, NULL, NULL, '2024-12'),
-- EXPIRED 상태 + SETTLEMENT 타입
(6, 1, 7, '한지민', 'SETTLEMENT', 20000.00, DATE_SUB(CURDATE(), INTERVAL 20 DAY), 10, 'EXPIRED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 DAY), INTERVAL 7 DAY), NULL, NULL, 4, NULL);

-- ============================================================
-- 13. 일정 참석자 데이터
-- ============================================================
INSERT INTO schedule_participants
(participant_id, schedule_id, member_id, attendance_status, created_at, updated_at)
VALUES
(1, 1, 1, 'ATTENDING', NOW(), NOW()),
(2, 1, 2, 'ATTENDING', NOW(), NOW()),
(3, 1, 3, 'ATTENDING', NOW(), NOW()),
(4, 1, 4, 'NOT_ATTENDING', NOW(), NOW()),
(5, 1, 5, 'UNDECIDED', NOW(), NOW()),
(6, 2, 1, 'ATTENDING', NOW(), NOW()),
(7, 2, 2, 'ATTENDING', NOW(), NOW()),
(8, 4, 1, 'ATTENDING', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
(9, 4, 2, 'ATTENDING', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
(10, 4, 3, 'ATTENDING', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY));

-- ============================================================
-- 14. 게시글 좋아요 데이터
-- ============================================================
INSERT INTO post_likes
(like_id, post_id, user_id, created_at)
VALUES
(1, 1, 2, NOW()),
(2, 1, 3, NOW()),
(3, 1, 4, NOW()),
(4, 3, 2, NOW()),
(5, 3, 3, NOW()),
(6, 5, 1, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ============================================================
-- 15. 댓글 좋아요 데이터
-- ============================================================
INSERT INTO comment_likes
(like_id, comment_id, user_id, created_at)
VALUES
(1, 1, 3, NOW()),
(2, 1, 4, NOW()),
(3, 3, 1, NOW()),
(4, 3, 4, NOW()),
(5, 4, 2, NOW());

SET FOREIGN_KEY_CHECKS = 1;