/**
 * API 클라이언트 설정
 * 백엔드 API와 통신하기 위한 기본 설정
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * JWT 토큰을 로컬 스토리지에서 가져오기
 */
export const getToken = (): string | null => {
  return localStorage.getItem('token');
};

/**
 * JWT 토큰을 로컬 스토리지에 저장
 */
export const setToken = (token: string): void => {
  localStorage.setItem('token', token);
};

/**
 * JWT 토큰 제거
 */
export const removeToken = (): void => {
  localStorage.removeItem('token');
};

/**
 * API 요청 기본 설정
 */
interface RequestOptions extends RequestInit {
  requiresAuth?: boolean;
}

/**
 * 인증 에러 클래스
 */
export class AuthenticationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'AuthenticationError';
  }
}

/**
 * API 호출 함수
 */
export const apiClient = async <T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> => {
  const { requiresAuth = true, ...fetchOptions } = options;

  // 헤더 설정
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...fetchOptions.headers,
  };

  // 인증이 필요한 경우 토큰 추가
  if (requiresAuth) {
    const token = getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  // URL 구성
  const url = `${API_BASE_URL}${endpoint}`;

  try {
    const response = await fetch(url, {
      ...fetchOptions,
      headers,
    });

    // 응답이 성공이 아닌 경우 에러 처리
    if (!response.ok) {
      // 401, 403은 인증 에러로 처리 (조용히 처리)
      if (response.status === 401 || response.status === 403) {
        throw new AuthenticationError('인증이 필요합니다');
      }
      
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
    }

    // 응답 데이터 파싱
    const data = await response.json();
    
    // SuccessResponse 형식인 경우 data 필드 추출
    if (data.data !== undefined) {
      return data.data as T;
    }
    
    return data as T;
  } catch (error) {
    // AuthenticationError는 그대로 전달 (조용히 처리)
    if (error instanceof AuthenticationError) {
      throw error;
    }
    console.error('API request failed:', error);
    throw error;
  }
};

/**
 * GET 요청
 */
export const get = <T>(endpoint: string, requiresAuth = true): Promise<T> => {
  return apiClient<T>(endpoint, {
    method: 'GET',
    requiresAuth,
  });
};

/**
 * POST 요청
 */
export const post = <T>(
  endpoint: string,
  body?: unknown,
  requiresAuth = true
): Promise<T> => {
  return apiClient<T>(endpoint, {
    method: 'POST',
    body: body ? JSON.stringify(body) : undefined,
    requiresAuth,
  });
};

/**
 * PUT 요청
 */
export const put = <T>(
  endpoint: string,
  body?: unknown,
  requiresAuth = true
): Promise<T> => {
  return apiClient<T>(endpoint, {
    method: 'PUT',
    body: body ? JSON.stringify(body) : undefined,
    requiresAuth,
  });
};

/**
 * PATCH 요청
 */
export const patch = <T>(
  endpoint: string,
  body?: unknown,
  requiresAuth = true
): Promise<T> => {
  return apiClient<T>(endpoint, {
    method: 'PATCH',
    body: body ? JSON.stringify(body) : undefined,
    requiresAuth,
  });
};

/**
 * DELETE 요청
 */
export const del = <T>(endpoint: string, requiresAuth = true): Promise<T> => {
  return apiClient<T>(endpoint, {
    method: 'DELETE',
    requiresAuth,
  });
};
