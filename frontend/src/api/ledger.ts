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

/**
 * 모임의 장부 내역 조회
 */
export const getLedger = async (
  clubId: number,
  startDate?: string,
  endDate?: string,
  scheduleId?: number
): Promise<TransactionLogResponse[]> => {
  const params = new URLSearchParams();
  if (startDate) params.append('startDate', startDate);
  if (endDate) params.append('endDate', endDate);
  if (scheduleId) params.append('scheduleId', scheduleId.toString());
  
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
