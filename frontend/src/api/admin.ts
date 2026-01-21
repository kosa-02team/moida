/**
 * 시스템 관리자(Admin) 관련 API
 */

import { get, post } from './client';

// ==================== Types ====================

export interface AdminDashboardResponse {
  pendingReports: number;
  bannedUsers: number;
  totalUsers: number;
  totalClubs: number;
  closedClubs: number;
}

export interface AdminReportResponse {
  reportId: number;
  clubId: number;
  clubName: string;
  reporterId: number;
  reporterName: string;
  targetId: number;
  targetName: string;
  reason: string;
  photoUrl: string | null;
  status: string;
  createdAt: string;
}

export interface AdminUserResponse {
  userId: number;
  loginId: string;
  realName: string;
  systemRole: string;
  status: string;
  createdAt: string;
  bannedAt: string | null;
}

export interface AdminClubResponse {
  clubId: number;
  name: string;
  ownerId: number;
  ownerName: string;
  status: string;
  createdAt: string;
  closedAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ==================== API Functions ====================

/**
 * 대시보드 통계 조회
 */
export const getDashboard = async (): Promise<AdminDashboardResponse> => {
  return get<AdminDashboardResponse>('/api/admin/dashboard');
};

/**
 * 신고 목록 조회
 */
export const getReports = async (
  page: number = 0,
  size: number = 10,
  status?: string
): Promise<PageResponse<AdminReportResponse>> => {
  const params = new URLSearchParams();
  params.append('page', page.toString());
  params.append('size', size.toString());
  if (status && status !== 'all') {
    params.append('status', status);
  }
  return get<PageResponse<AdminReportResponse>>(`/api/admin/reports?${params.toString()}`);
};

/**
 * 신고 상세 조회
 */
export const getReportDetail = async (reportId: number): Promise<AdminReportResponse> => {
  return get<AdminReportResponse>(`/api/admin/reports/${reportId}`);
};

/**
 * 신고 처리
 */
export const processReport = async (
  reportId: number,
  action: string
): Promise<void> => {
  return post<void>(`/api/admin/reports/${reportId}`, { action });
};

/**
 * 회원 목록 조회
 */
export const getUsers = async (
  page: number = 0,
  size: number = 10,
  keyword?: string,
  status?: string
): Promise<PageResponse<AdminUserResponse>> => {
  const params = new URLSearchParams();
  params.append('page', page.toString());
  params.append('size', size.toString());
  if (keyword) {
    params.append('keyword', keyword);
  }
  if (status && status !== 'all') {
    params.append('status', status);
  }
  return get<PageResponse<AdminUserResponse>>(`/api/admin/users?${params.toString()}`);
};

/**
 * 회원 상태 관리
 */
export const manageUser = async (
  userId: number,
  action: string
): Promise<void> => {
  return post<void>(`/api/admin/users/${userId}/status`, { action });
};

/**
 * 모임 목록 조회
 */
export const getClubs = async (
  page: number = 0,
  size: number = 10,
  keyword?: string,
  status?: string
): Promise<PageResponse<AdminClubResponse>> => {
  const params = new URLSearchParams();
  params.append('page', page.toString());
  params.append('size', size.toString());
  if (keyword) {
    params.append('keyword', keyword);
  }
  if (status && status !== 'all') {
    params.append('status', status);
  }
  return get<PageResponse<AdminClubResponse>>(`/api/admin/clubs?${params.toString()}`);
};

/**
 * 모임 상태 관리
 */
export const manageClub = async (
  clubId: number,
  action: string
): Promise<void> => {
  return post<void>(`/api/admin/clubs/${clubId}/status`, { action });
};
