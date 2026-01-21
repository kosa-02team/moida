/**
 * 모임(Club) 관련 API (전체)
 */

import { get, post, put, patch } from './client';

// ==================== Types ====================

export interface ClubDetailResponse {
  clubId: number;
  clubName: string;
  ownerId: number;
  mainAccountId: string;
  inviteCode: string;
  status: string;
  visibility: string;
  type?: string;
  category?: string;
  maxMembers?: number;
  currentMembers?: number;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ClubListResponse {
  clubId: number;
  name: string;
  visibility: string;
  status: string;
  memberCount: number;
  inviteCode: string;
  createdAt: string;
  // ownerName은 백엔드에 없음
}

export interface ClubCreateRequest {
  clubName: string;
  visibility?: 'PUBLIC' | 'PRIVATE';
  type?: 'OPERATION_FEE' | 'FAIR_SETTLEMENT';
  maxMembers?: number;
  category?: 'STUDY' | 'SPORTS' | 'SOCIAL' | 'HOBBY' | 'FINANCE' | 'ETC';
}

export interface ClubUpdateRequest {
  clubName?: string;
  visibility?: 'PUBLIC' | 'PRIVATE';
  category?: 'STUDY' | 'SPORTS' | 'SOCIAL' | 'HOBBY' | 'FINANCE' | 'ETC';
}

// ==================== API Functions ====================

/**
 * 모임 생성
 */
export const createClub = async (
  request: ClubCreateRequest
): Promise<ClubDetailResponse> => {
  return post<ClubDetailResponse>('/api/clubs', request);
};

/**
 * 모임 정보 조회
 */
export const getClub = async (
  clubId: number
): Promise<ClubDetailResponse> => {
  return get<ClubDetailResponse>(`/api/clubs/${clubId}`);
};

/**
 * 모임 정보 수정
 */
export const updateClub = async (
  clubId: number,
  request: ClubUpdateRequest
): Promise<ClubDetailResponse> => {
  return put<ClubDetailResponse>(`/api/clubs/${clubId}`, request);
};

/**
 * 모임 삭제/폐쇄
 */
export const closeClub = async (
  clubId: number
): Promise<void> => {
  return patch<void>(`/api/clubs/${clubId}/close`, {});
};

/**
 * 초대 코드로 모임 조회
 */
export const getClubByInviteCode = async (
  inviteCode: string
): Promise<ClubDetailResponse> => {
  return get<ClubDetailResponse>(`/api/clubs/by-invite-code/${inviteCode}`);
};

/**
 * 모든 모임 조회 (페이징)
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export const getAllClubs = async (
  page: number = 0,
  size: number = 20
): Promise<PageResponse<ClubDetailResponse>> => {
  const url = `/api/clubs?page=${page}&size=${size}`;
  return get<PageResponse<ClubDetailResponse>>(url, false);
};

/**
 * 카테고리별 모임 조회
 */
export const getClubsByCategory = async (
  category: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<ClubDetailResponse>> => {
  const url = `/api/clubs/category/${category}?page=${page}&size=${size}`;
  return get<PageResponse<ClubDetailResponse>>(url, false);
};

/**
 * 카테고리 + 상태별 모임 조회
 */
export const getClubsByCategoryAndStatus = async (
  category: string,
  status: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<ClubDetailResponse>> => {
  const url = `/api/clubs/category/${category}/status/${status}?page=${page}&size=${size}`;
  return get<PageResponse<ClubDetailResponse>>(url, false);
};

/**
 * 모임 검색 (카테고리, 이름)
 */
export const searchClubs = async (
  category?: string,
  clubName?: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<ClubDetailResponse>> => {
  const params = new URLSearchParams();
  params.append('page', page.toString());
  params.append('size', size.toString());
  if (category) params.append('category', category);
  if (clubName) params.append('clubName', clubName);
  
  const url = `/api/clubs/search?${params.toString()}`;
  return get<PageResponse<ClubDetailResponse>>(url, false);
};

/**
 * 모임 활성화
 */
export const activateClub = async (
  clubId: number
): Promise<void> => {
  return patch<void>(`/api/clubs/${clubId}/activate`, {});
};

export interface TransferOwnershipRequest {
  newOwnerMemberId: number;
}

/**
 * 모임장 위임
 */
export const transferOwnership = async (
  clubId: number,
  request: TransferOwnershipRequest
): Promise<void> => {
  return patch<void>(`/api/clubs/${clubId}/transfer-ownership`, request);
};
