CREATE TABLE refresh_token (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,       -- @Id, Long id
                               token TEXT NOT NULL,                        -- @Column(columnDefinition = "TEXT")
                               expire_time DATETIME(6) NOT NULL,           -- LocalDateTime
                               user_id BIGINT,                             -- @ManyToOne Users user

                               CONSTRAINT fk_refresh_token_users FOREIGN KEY (user_id) REFERENCES users (user_id)
);