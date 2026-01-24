/**
 * 은행 계좌(BankAccount) 관련 API
 */

import { get, post, put } from './client';
import { TransactionLogResponse } from './ledger';
import { PaymentRequest } from './payment-request';

// ==================== Types ====================

export interface AccountCreateRequest {
  userId: number;
  bankCode: string; // 은행 코드
  accountNumber?: string | null; // 등록할 계좌번호 (가상계좌 발급이면 null 가능)
  ownerName: string; // 소유자명
}

export interface BankAccounts {
  accountId: number;
  clubId: number;
  bankCode: string;
  userId: number;
  accountNumber: string;
  depositorName: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProcessedTransactionResponse {
  historyId: number;
  txId: string;
  occurredAt: string;
  type: 'DEPOSIT' | 'WITHDRAW';
  amount: number;
  balanceAfter: number;
  printContent: string;
  matchType: 'AUTO_MATCHED' | 'UNMATCHED' | 'CONFIRMED';
  matchedMemberName?: string | null;
  matchedMemberId?: number | null;
  matchedRequestType?: string | null;
  paymentRequestId?: number | null;
}

export interface BankTransactionHistory {
  historyId: number;
  clubId: number;
  bankTransactionAt: string;
  printContent: string;
  amount: number;
  uniqueTxKey: string;
}

export interface UnmatchedTransactionsResponse {
  unmatchedTransactions: BankTransactionHistory[];
  availableRequests: PaymentRequest[];
}

export interface RefundRequest {
  clubId: number;
  recipientUserId: number;
  recipientName: string;
  recipientBankCode: string;
  recipientAccountNum: string;
  amount: number;
  memo: string;
}

export interface RefundResponse {
  success: boolean;
  transferId: string | null;
  message: string;
  amount: number;
  recipientName: string;
  recipientAccountMasked: string;
}

// ==================== API Functions ====================

/**
 * 모임 가상계좌 조회
 */
export const getBankAccount = async (
  clubId: number
): Promise<BankAccounts> => {
  const url = `/api/clubs/${clubId}/bank/account`;
  return get<BankAccounts>(url);
};

/**
 * 모임 가상계좌 생성
 */
export const createBankAccount = async (
  clubId: number,
  request: AccountCreateRequest
): Promise<BankAccounts> => {
  const url = `/api/clubs/${clubId}/bank/accounts`;
  return post<BankAccounts>(url, request);
};

/**
 * 모임 가상계좌 변경
 */
export const changeBankAccount = async (
  clubId: number,
  request: AccountCreateRequest
): Promise<BankAccounts> => {
  const url = `/api/clubs/${clubId}/bank/account`;
  return put<BankAccounts>(url, request);
};

/**
 * 모임 가상계좌 거래내역 조회 및 동기화
 */
export const syncBankTransactions = async (
  clubId: number,
  from?: string,
  to?: string
): Promise<TransactionLogResponse[]> => {
  const params = new URLSearchParams();
  if (from) params.append('from', from);
  if (to) params.append('to', to);
  
  const queryString = params.toString();
  const url = `/api/clubs/${clubId}/bank/sync${queryString ? `?${queryString}` : ''}`;
  return post<TransactionLogResponse[]>(url);
};

/**
 * Stub 거래내역 동기화 (테스트용)
 */
export const syncBankTransactionsStub = async (
  clubId: number,
  stubId: number,
  from?: string,
  to?: string
): Promise<TransactionLogResponse[]> => {
  const params = new URLSearchParams();
  if (from) params.append('from', from);
  if (to) params.append('to', to);
  
  const queryString = params.toString();
  const url = `/api/clubs/${clubId}/bank/sync/${stubId}${queryString ? `?${queryString}` : ''}`;
  return post<TransactionLogResponse[]>(url);
};

/**
 * 처리된 거래내역 조회 (매칭 정보 포함)
 */
export const getProcessedTransactions = async (
  clubId: number,
  from: string,
  to: string
): Promise<ProcessedTransactionResponse[]> => {
  const url = `/api/clubs/${clubId}/bank/transactions/processed?from=${from}&to=${to}`;
  return get<ProcessedTransactionResponse[]>(url);
};

/**
 * 미매칭 거래내역 조회
 */
export const getUnmatchedTransactions = async (
  clubId: number
): Promise<UnmatchedTransactionsResponse> => {
  const url = `/api/clubs/${clubId}/bank/transactions/unmatched`;
  return get<UnmatchedTransactionsResponse>(url);
};

/**
 * 모임 정산 환급
 */
export const refundToMember = async (
  clubId: number,
  request: RefundRequest
): Promise<RefundResponse> => {
  const url = `/api/clubs/${clubId}/bank/refund`;
  return post<RefundResponse>(url, request);
};
