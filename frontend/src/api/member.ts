/**
 * 멤버(Member) 관련 API
 */

import { get, patch, post } from './client';

// ==================== Types ====================

export interface MemberListResponse {
  memberId: number;
  userId: number;
  realName: string;
  clubNickname: string;
  roles: string[];
  status: string;
  joinedAt: string;
  createdAt: string;
}

export interface RoleUpdateRequest {
  role: string; // "ACCOUNTANT", "STAFF", "MEMBER"
}

// ==================== API Functions ====================

/**
 * 멤버 목록 조회
 */
export const getMembers = async (
  clubId: number,
  status: string = 'ACTIVE'
): Promise<MemberListResponse[]> => {
  return get<MemberListResponse[]>(`/api/clubs/${clubId}/members?status=${status}`);
};

/**
 * 역할 변경
 */
export const updateMemberRole = async (
  clubId: number,
  memberId: number,
  role: string
): Promise<MemberListResponse> => {
  return patch<MemberListResponse>(`/api/clubs/${clubId}/members/${memberId}/role`, { role });
};

/**
 * 가입 승인
 */
export const approveMember = async (
  clubId: number,
  memberId: number
): Promise<void> => {
  return patch<void>(`/api/club-member/${clubId}/members/${memberId}/approve`, {});
};

/**
 * 가입 거절
 */
export const rejectMember = async (
  clubId: number,
  memberId: number
): Promise<void> => {
  return patch<void>(`/api/club-member/${clubId}/members/${memberId}/reject`, {});
};

/**
 * 멤버 추방
 */
export const kickMember = async (
  clubId: number,
  memberId: number
): Promise<void> => {
  return patch<void>(`/api/club-member/${clubId}/members/${memberId}/kick`, {});
};

/**
 * 모임 탈퇴
 */
export const leaveClub = async (
  clubId: number
): Promise<void> => {
  return patch<void>(`/api/club-member/${clubId}/leave`, {});
};

/**
 * 모임 가입 요청
 */
export interface JoinClubRequest {
  nickname: string;
}

export interface JoinClubResponse {
  memberId: number;
  userId: number;
  realName: string;
  clubNickname: string;
  roles: string[];
  status: string;
  joinedAt: string;
  createdAt: string;
}

export const joinClub = async (
  clubId: number,
  request: JoinClubRequest
): Promise<JoinClubResponse> => {
  return post<JoinClubResponse>(`/api/club-member/${clubId}/join`, request);
};
