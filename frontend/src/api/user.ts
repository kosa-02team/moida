import { get, put } from './client';

export interface UserResponse {
  userId: number;
  loginId: string;
  realName: string;
  systemRole: string;
  status: string;
  createdAt: string;
}

export interface UserUpdateRequest {
  realName: string;
}

export interface MyClubResponse {
  clubId: number;
  name: string;
  roles: string[];
  joinedAt: string;
  visibility?: string;
  status?: string;
  category?: string;
}

/**
 * 내 정보 조회
 */
export const getMyInfo = async (): Promise<UserResponse> => {
  return get<UserResponse>('/api/users/me');
};

/**
 * 내 정보 수정
 */
export const updateMyInfo = async (request: UserUpdateRequest): Promise<UserResponse> => {
  return put<UserResponse>('/api/users/me', request);
};

/**
 * 내 모임 목록 조회
 */
export const getMyClubs = async (): Promise<MyClubResponse[]> => {
  return get<MyClubResponse[]>('/api/users/me/clubs');
};
