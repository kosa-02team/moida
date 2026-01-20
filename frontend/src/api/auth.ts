/**
 * 인증 관련 API
 */

import { post, get } from './client';

export interface LoginRequest {
  loginId: string;
  password: string;
}

export interface SignupRequest {
  loginId: string;  // 이메일 형식
  password: string;
  realName: string; // 3글자 한글 이름
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
}

/**
 * 로그인
 */
export const login = async (request: LoginRequest): Promise<RefreshTokenResponse> => {
  const response = await post<RefreshTokenResponse>('/api/auth/login', request, false);
  return response;
};

/**
 * 회원가입
 */
export const signup = async (request: SignupRequest): Promise<number> => {
  return post<number>('/api/auth/signup', request, false);
};

/**
 * 토큰 갱신
 */
export interface RefreshTokenRequest {
  refreshToken: string;
}

export const refreshToken = async (request: RefreshTokenRequest): Promise<RefreshTokenResponse> => {
  return post<RefreshTokenResponse>('/api/auth/refresh', request, false);
};
