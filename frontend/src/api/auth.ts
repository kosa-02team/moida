/**
 * 인증 관련 API
 */

import { post, get } from './client';

export interface LoginRequest {
  loginId: string;
  password: string;
}

export interface SignupRequest {
  loginId: string;
  password: string;
  realName: string;
  email?: string;
}

/**
 * 로그인
 */
export const login = async (request: LoginRequest): Promise<string> => {
  const response = await post<{ data: string }>('/api/auth/login', request, false);
  return response;
};

/**
 * 회원가입
 */
export const signup = async (request: SignupRequest): Promise<number> => {
  const response = await post<{ data: number }>('/api/auth/signup', request, false);
  return response;
};
