-- [MOIDA] 통합 회계/운영 DB (MySQL 8.x)


SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- 1. 사용자
-- =========================
CREATE TABLE users (
                       user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       login_id VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       real_name VARCHAR(50) NOT NULL,
                       system_role VARCHAR(20) DEFAULT 'USER',
                       simple_password VARCHAR(255),
                       status VARCHAR(20) DEFAULT 'ACTIVE',
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       deleted_at DATETIME,
                       banned_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 2. 은행 계좌
-- =========================
CREATE TABLE bank_accounts (
                               account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               bank_name VARCHAR(50) NOT NULL,
                               account_number VARCHAR(255) NOT NULL UNIQUE,
                               depositor_name VARCHAR(50) NOT NULL,
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               deleted_at DATETIME,
                               FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 3. 모임
-- =========================
CREATE TABLE clubs (
                       club_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       owner_id BIGINT NOT NULL,
                       main_account_id BIGINT NOT NULL,
                       invite_code VARCHAR(20) UNIQUE,
                       status VARCHAR(20) DEFAULT 'ACTIVE',
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       closed_at DATETIME,
                       FOREIGN KEY (owner_id) REFERENCES users(user_id),
                       FOREIGN KEY (main_account_id) REFERENCES bank_accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE club_members (
                              member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              club_id BIGINT NOT NULL,
                              user_id BIGINT NOT NULL,
                              club_nickname VARCHAR(50) NOT NULL,
                              role VARCHAR(20) DEFAULT 'MEMBER',
                              status VARCHAR(20) DEFAULT 'ACTIVE',
                              joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE KEY uk_club_user (club_id, user_id),
                              FOREIGN KEY (club_id) REFERENCES clubs(club_id),
                              FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fee_policies (
                              policy_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              club_id BIGINT NOT NULL,
                              amount DECIMAL(19,2) DEFAULT 0,
                              due_day INT DEFAULT 1,
                              is_active TINYINT(1) DEFAULT 1,
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              FOREIGN KEY (club_id) REFERENCES clubs(club_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 4. 게시판
-- =========================
CREATE TABLE posts (
                       post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       club_id BIGINT NOT NULL,
                       writer_id BIGINT NOT NULL,
                       category VARCHAR(30) NOT NULL,
                       title VARCHAR(200) NOT NULL,
                       content TEXT,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       deleted_at DATETIME,
                       FOREIGN KEY (club_id) REFERENCES clubs(club_id),
                       FOREIGN KEY (writer_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE comments (
                          comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          post_id BIGINT NOT NULL,
                          writer_id BIGINT NOT NULL,
                          content TEXT NOT NULL,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          deleted_at DATETIME,
                          FOREIGN KEY (post_id) REFERENCES posts(post_id),
                          FOREIGN KEY (writer_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_likes (
                            like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            post_id BIGINT NOT NULL,
                            user_id BIGINT NOT NULL,
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uk_post_like (post_id, user_id),
                            FOREIGN KEY (post_id) REFERENCES posts(post_id),
                            FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_read_logs (
                                read_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                post_id BIGINT NOT NULL,
                                user_id BIGINT NOT NULL,
                                read_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE KEY uk_post_read (post_id, user_id),
                                FOREIGN KEY (post_id) REFERENCES posts(post_id),
                                FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_images (
                             image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             post_id BIGINT NOT NULL,
                             image_url VARCHAR(255) NOT NULL,
                             created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                             FOREIGN KEY (post_id) REFERENCES posts(post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 5. 신고 / 처벌
-- =========================
CREATE TABLE reports (
                         report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         club_id BIGINT NOT NULL,
                         reporter_id BIGINT NOT NULL,
                         target_id BIGINT NOT NULL,
                         reason TEXT,
                         photo_url VARCHAR(255),
                         status VARCHAR(20) DEFAULT 'PENDING',
                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                         updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         FOREIGN KEY (club_id) REFERENCES clubs(club_id),
                         FOREIGN KEY (reporter_id) REFERENCES users(user_id),
                         FOREIGN KEY (target_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE member_action_history (
                                       action_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       club_id BIGINT NOT NULL,
                                       user_id BIGINT NOT NULL,
                                       report_id BIGINT,
                                       action_type VARCHAR(30) NOT NULL,
                                       actor_id BIGINT NOT NULL,
                                       reason TEXT NOT NULL,
                                       action_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       FOREIGN KEY (club_id) REFERENCES clubs(club_id),
                                       FOREIGN KEY (user_id) REFERENCES users(user_id),
                                       FOREIGN KEY (actor_id) REFERENCES users(user_id),
                                       FOREIGN KEY (report_id) REFERENCES reports(report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 6. 투표
-- =========================
CREATE TABLE votes (
                       vote_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       post_id BIGINT NOT NULL COMMENT '연결된 게시글 (posts.category = VOTE)',
                       creator_id BIGINT NOT NULL COMMENT '투표 생성자',
                       title VARCHAR(200) NOT NULL,
                       description TEXT,
                       is_anonymous TINYINT(1) DEFAULT 0 COMMENT '익명 투표 여부',
                       allow_multiple TINYINT(1) DEFAULT 0 COMMENT '복수 선택 허용 여부',
                       status VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN, CLOSED',
                       closed_at DATETIME,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       FOREIGN KEY (post_id) REFERENCES posts(post_id),
                       FOREIGN KEY (creator_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vote_options (
                              option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              vote_id BIGINT NOT NULL,
                              option_text VARCHAR(200) NOT NULL COMMENT '표시용 텍스트',
                              option_order INT DEFAULT 1,
                              event_date DATETIME COMMENT '확정 시 생성될 일정 날짜/시간',
                              location VARCHAR(255) COMMENT '장소',
                              FOREIGN KEY (vote_id) REFERENCES votes(vote_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vote_records (
                              record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              vote_id BIGINT NOT NULL,
                              option_id BIGINT NOT NULL,
                              user_id BIGINT NOT NULL,
                              voted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE KEY uk_vote_user_option (vote_id, option_id, user_id),
                              FOREIGN KEY (vote_id) REFERENCES votes(vote_id),
                              FOREIGN KEY (option_id) REFERENCES vote_options(option_id),
                              FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 7. 일정 / 정산
-- =========================
CREATE TABLE schedules (
                           post_id BIGINT PRIMARY KEY,
                           event_date DATETIME NOT NULL,
                           location VARCHAR(255),
                           entry_fee DECIMAL(19,2) DEFAULT 0,
                           total_spent DECIMAL(19,2) DEFAULT 0,
                           refund_per_person DECIMAL(19,2) DEFAULT 0,
                           status VARCHAR(20) DEFAULT 'OPEN',
                           closed_at DATETIME,
                           FOREIGN KEY (post_id) REFERENCES posts(post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE schedule_participants (
                                       participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       schedule_id BIGINT NOT NULL,
                                       user_id BIGINT NOT NULL,
                                       attendance_status VARCHAR(20) DEFAULT 'UNDECIDED',
                                       is_refunded TINYINT(1) DEFAULT 0,
                                       UNIQUE KEY uk_schedule_user (schedule_id, user_id),
                                       FOREIGN KEY (schedule_id) REFERENCES schedules(post_id),
                                       FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE settlement_requests (
                                     settlement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     post_id BIGINT NOT NULL,
                                     total_amount DECIMAL(19,2) NOT NULL,
                                     status VARCHAR(20) DEFAULT 'PENDING',
                                     created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     FOREIGN KEY (post_id) REFERENCES posts(post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE settlement_items (
                                  item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  settlement_id BIGINT NOT NULL,
                                  item_name VARCHAR(100) NOT NULL,
                                  amount DECIMAL(19,2) NOT NULL,
                                  receipt_url VARCHAR(255),
                                  description TEXT,
                                  FOREIGN KEY (settlement_id) REFERENCES settlement_requests(settlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 8. 회계 / 감사
-- =========================
CREATE TABLE bank_transaction_history (
                                          history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          club_id BIGINT NOT NULL,
                                          bank_transaction_at DATETIME NOT NULL,
                                          sender_account_number VARCHAR(255) NOT NULL,
                                          sender_name VARCHAR(50) NOT NULL,
                                          amount DECIMAL(19,2) NOT NULL,
                                          is_matched TINYINT(1) DEFAULT 0,
                                          unique_tx_key VARCHAR(255) UNIQUE,
                                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                          FOREIGN KEY (club_id) REFERENCES clubs(club_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE transaction_log (
                                 transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 club_id BIGINT NOT NULL,
                                 account_id BIGINT NOT NULL,
                                 type VARCHAR(30) NOT NULL,
                                 amount DECIMAL(19,2) NOT NULL,
                                 balance_after DECIMAL(19,2) NOT NULL,
                                 description TEXT,
                                 editor_id BIGINT,
                                 created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY (club_id) REFERENCES clubs(club_id),
                                 FOREIGN KEY (account_id) REFERENCES bank_accounts(account_id),
                                 FOREIGN KEY (editor_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
                            audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            transaction_id BIGINT NOT NULL,
                            actor_id BIGINT NOT NULL,
                            before_description TEXT,
                            after_description TEXT,
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (transaction_id) REFERENCES transaction_log(transaction_id),
                            FOREIGN KEY (actor_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 9. 회비 / 알림 / 메시지
-- =========================
CREATE TABLE monthly_fee_log (
                                 fee_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 club_id BIGINT NOT NULL,
                                 user_id BIGINT NOT NULL,
                                 fee_month DATE NOT NULL,
                                 is_paid TINYINT(1) DEFAULT 0,
                                 paid_at DATETIME,
                                 UNIQUE KEY uk_fee (club_id, user_id, fee_month),
                                 FOREIGN KEY (club_id) REFERENCES clubs(club_id),
                                 FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
                               noti_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               content TEXT NOT NULL,
                               ref_id BIGINT,
                               ref_type VARCHAR(30),
                               is_read TINYINT(1) DEFAULT 0,
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE messages (
                          message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          sender_id BIGINT NOT NULL,
                          receiver_id BIGINT NOT NULL,
                          content TEXT NOT NULL,
                          is_read TINYINT(1) DEFAULT 0,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          deleted_at DATETIME,
                          FOREIGN KEY (sender_id) REFERENCES users(user_id),
                          FOREIGN KEY (receiver_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
