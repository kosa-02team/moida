-- V16: STUB 은행 데이터 추가
--
-- 변경사항:
-- 1. 모임 생성 시 자동으로 계좌를 생성하기 위해 STUB 은행 데이터 추가

INSERT INTO banks (bank_code, bank_name, provider_class_name, is_active)
VALUES ('STUB', '스텁은행', 'back.bank.provider.stub.StubBankProvider', 1)
ON DUPLICATE KEY UPDATE
    bank_name = VALUES(bank_name),
    provider_class_name = VALUES(provider_class_name),
    is_active = VALUES(is_active);
