/**
 * 입금 요청(PaymentRequest) 관련 API
 */

import { get, post, patch } from './client';

// ==================== Types ====================

export interface PaymentRequestCreateRequest {
  requests: PaymentRequestItem[];
}

export interface PaymentRequestItem {
  memberId: number;
  memberName: string;
  requestType: 'MEMBERSHIP_FEE' | 'SETTLEMENT' | 'DEPOSIT';
  expectedAmount: number;
  expectedDate: string; // ISO date string (YYYY-MM-DD)
  matchDaysRange?: number; // ±N일 (선택, 기본 10일)
  expiresInDays?: number; // N일 후 만료 (선택)
  scheduleId?: number | null; // 일정 관련 요청일 경우
  billingPeriod?: string | null; // 회비 관련 요청일 경우 (예: "2024-02")
}

export interface PaymentRequest {
  requestId: number;
  clubId: number;
  memberId: number;
  memberName: string;
  requestType: string; // "MEMBERSHIP_FEE" | "SETTLEMENT" | "DEPOSIT"
  expectedAmount: number;
  expectedDate: string;
  matchDaysRange: number;
  status: string; // "PENDING" | "MATCHED" | "EXPIRED"
  matchType?: string | null; // "AUTO_MATCHED" | "CONFIRMED"
  matchedHistoryId?: number | null;
  createdAt: string;
  expiresAt?: string | null;
  matchedAt?: string | null;
  matchedBy?: number | null;
  scheduleId?: number | null;
  billingPeriod?: string | null;
}

// ==================== API Functions ====================

/**
 * 입금 요청 목록 조회
 */
export const getPaymentRequests = async (
  clubId: number
): Promise<PaymentRequest[]> => {
  const url = `/api/clubs/${clubId}/payment-requests`;
  return get<PaymentRequest[]>(url);
};

/**
 * 수동 입금 요청 생성
 */
export const createPaymentRequests = async (
  clubId: number,
  request: PaymentRequestCreateRequest
): Promise<PaymentRequest[]> => {
  const url = `/api/clubs/${clubId}/payment-requests`;
  return post<PaymentRequest[]>(url, request);
};

/**
 * 입금 확인 (관리자 수동 처리)
 */
export const confirmPaymentRequest = async (
  clubId: number,
  requestId: number
): Promise<void> => {
  const url = `/api/clubs/${clubId}/payment-requests/${requestId}/confirm`;
  return patch<void>(url, {});
};

/**
 * 참가비 일괄 걷기 요청
 */
export const collectScheduleFee = async (
  clubId: number,
  scheduleId: number
): Promise<void> => {
  const url = `/api/clubs/${clubId}/schedules/${scheduleId}/collect-fee`;
  return post<void>(url);
};

/**
 * 정산 및 잔액 환급 실행
 */
export const settleSchedule = async (
  clubId: number,
  scheduleId: number
): Promise<void> => {
  const url = `/api/clubs/${clubId}/schedules/${scheduleId}/settle`;
  return post<void>(url);
};
