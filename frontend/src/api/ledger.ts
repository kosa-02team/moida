/**
 * 장부(Ledger) 관련 API
 */

import { get, post, patch } from './client';

export interface TransactionLogResponse {
  transactionId: number;
  clubId: number;
  scheduleId: number | null;
  accountId: number | null;
  type: string; // "DEPOSIT" | "WITHDRAW"
  amount: number;
  balanceAfter: number;
  description: string | null;
  editorId: number | null;
  createdAt: string;
  bankHistoryId: number | null;
  matchedMemberName?: string;
}

export interface ManualTransactionRequest {
  occurredAt: string; // ISO date string (YYYY-MM-DD)
  content: string;
  amount: number;
  type: 'DEPOSIT' | 'WITHDRAW';
}

export interface TransactionUpdateRequest {
  memo: string;
}

export interface BankTransactionHistory {
  historyId: number;
  clubId: number;
  bankTransactionAt: string;
  printContent: string;
  amount: number;
  isMatched: boolean;
  unmatchReason: string | null;
  inoutType: string; // "DEPOSIT" | "WITHDRAW"
}

export interface PaymentRequestResponse {
  requestId: number;
  clubId: number;
  memberId: number;
  memberName: string;
  requestType: string; // "DEPOSIT" | "SETTLEMENT" | "MEMBERSHIP_FEE"
  expectedAmount: number;
  expectedDate: string;
  status: string; // "PENDING" | "MATCHED" | "EXPIRED"
  matchedHistoryId: number | null;
  scheduleId: number | null;
}

export interface UnmatchedTransactionsResponse {
  unmatchedTransactions: BankTransactionHistory[];
  availableRequests: PaymentRequestResponse[];
}

export interface ManualMatchRequest {
  requestId: number;
  historyId: number;
}

/**
 * 모임의 장부 내역 조회
 * @param sync - true인 경우 조회 전 은행 동기화 실행 (기본값: true, 최신 거래 내역 반영)
 */
export const getLedger = async (
  clubId: number,
  startDate?: string,
  endDate?: string,
  scheduleId?: number,
  sync: boolean = true
): Promise<TransactionLogResponse[]> => {
  const params = new URLSearchParams();
  if (startDate) params.append('startDate', startDate);
  if (endDate) params.append('endDate', endDate);
  if (scheduleId) params.append('scheduleId', scheduleId.toString());
  params.append('sync', sync.toString());
  
  const queryString = params.toString();
  const url = `/api/clubs/${clubId}/ledger${queryString ? `?${queryString}` : ''}`;
  return get<TransactionLogResponse[]>(url);
};

/**
 * 수동 장부 기록 생성
 */
export const createManualTransaction = async (
  clubId: number,
  request: ManualTransactionRequest
): Promise<void> => {
  const url = `/api/clubs/${clubId}/ledger/manual`;
  return post<void>(url, request);
};

/**
 * 장부 내역 수정 (메모 수정)
 */
export const updateTransaction = async (
  clubId: number,
  transactionId: number,
  request: TransactionUpdateRequest
): Promise<void> => {
  const url = `/api/clubs/${clubId}/ledger/${transactionId}`;
  return patch<void>(url, request);
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
 * 수동 매칭 처리
 */
export const manualMatch = async (
  clubId: number,
  requestId: number,
  historyId: number,
  matchedBy: number
): Promise<void> => {
  const url = `/api/clubs/${clubId}/payment-requests/${requestId}/manual-match`;
  return post<void>(url, { historyId, matchedBy });
};

/**
 * 여러 거래를 하나의 입금 요청에 매칭 (분할 입금)
 */
export const manualMatchMultiple = async (
  clubId: number,
  requestId: number,
  historyIds: number[],
  matchedBy: number
): Promise<void> => {
  const url = `/api/clubs/${clubId}/payment-requests/${requestId}/manual-match-multiple`;
  return post<void>(url, { historyIds, matchedBy });
};
