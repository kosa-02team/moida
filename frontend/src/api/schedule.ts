/**
 * 일정(Schedule) 관련 API
 */

import { get, post, put } from './client';

export interface ScheduleResponse {
  scheduleId: number;
  scheduleName: string;
  eventDate: string;
  endDate: string;
  location?: string;
  description?: string;
  entryFee?: number;
  totalSpent?: number;
  refundPerPerson?: number;
  status: string;
  closedAt?: string;
  cancelReason?: string;
  voteDeadline?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ScheduleCreateRequest {
  scheduleName: string;
  eventDate: string;
  endDate: string;
  location?: string;
  description?: string;
  entryFee?: number;
  voteDeadline?: string;
}

export interface ScheduleUpdateRequest {
  scheduleName: string;
  eventDate: string;
  endDate: string;
  location?: string;
  description?: string;
  entryFee?: number;
}

export interface ScheduleCancelRequest {
  cancelReason?: string;
}

export interface ScheduleSettlementRequest {
  totalSpent: number;
  refundPerPerson: number;
}

export interface ScheduleParticipantResponse {
  participantId: number;
  scheduleId: number;
  userId: number;
  realName: string;
  attendanceStatus: string;
  feeStatus: string;
  isRefunded: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * 모임의 일정 목록 조회
 */
export const getSchedules = async (clubId: number): Promise<ScheduleResponse[]> => {
  return get<ScheduleResponse[]>(`/api/clubs/${clubId}/schedules`);
};

/**
 * 단일 일정 조회
 */
export const getSchedule = async (
  clubId: number,
  scheduleId: number
): Promise<ScheduleResponse> => {
  return get<ScheduleResponse>(`/api/clubs/${clubId}/schedules/${scheduleId}`);
};

/**
 * 일정 생성
 */
export const createSchedule = async (
  clubId: number,
  request: ScheduleCreateRequest
): Promise<ScheduleResponse> => {
  return post<ScheduleResponse>(`/api/clubs/${clubId}/schedules`, request);
};

/**
 * 일정 수정
 */
export const updateSchedule = async (
  clubId: number,
  scheduleId: number,
  request: ScheduleUpdateRequest
): Promise<ScheduleResponse> => {
  return put<ScheduleResponse>(`/api/clubs/${clubId}/schedules/${scheduleId}`, request);
};

/**
 * 일정 마감
 */
export const closeSchedule = async (
  clubId: number,
  scheduleId: number
): Promise<void> => {
  return post<void>(`/api/clubs/${clubId}/schedules/${scheduleId}/close`);
};

/**
 * 일정 취소
 */
export const cancelSchedule = async (
  clubId: number,
  scheduleId: number,
  request?: ScheduleCancelRequest
): Promise<void> => {
  return post<void>(`/api/clubs/${clubId}/schedules/${scheduleId}/cancel`, request);
};

/**
 * 정산 정보 수정
 */
export const updateSettlement = async (
  clubId: number,
  scheduleId: number,
  request: ScheduleSettlementRequest
): Promise<ScheduleResponse> => {
  return put<ScheduleResponse>(
    `/api/clubs/${clubId}/schedules/${scheduleId}/settlement`,
    request
  );
};

/**
 * 일정 참여자 목록 조회
 */
export const getScheduleParticipants = async (
  clubId: number,
  scheduleId: number
): Promise<ScheduleParticipantResponse[]> => {
  return get<ScheduleParticipantResponse[]>(
    `/api/clubs/${clubId}/schedules/${scheduleId}/participants`
  );
};
