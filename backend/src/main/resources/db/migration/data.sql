-- data.sql
-- 시스템 계정 (user_id = 1)
INSERT INTO users
(user_id, login_id, password, real_name, system_role, simple_password, status, created_at, updated_at, deleted_at, banned_at)
VALUES
(1, 'system@moida.com', '$2a$10$xXF72CHMb65eNebwwe1Qc.M2Mm1tRytvzEJI8aZr1GZBEIhUZEGRy', '시스템', 'SYSTEM', NULL, 'ACTIVE', NOW(), NOW(), NULL, NULL);
