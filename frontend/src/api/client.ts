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
 * 401, 403 에러 발생 시 사용됩니다. 이 에러가 발생하면 로그인 페이지로 리다이렉트해야 합니다.
 */
export class AuthenticationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'AuthenticationError';
  }
}

/**
 * Refresh Token을 사용하여 Access Token 갱신
 */
const refreshAccessToken = async (): Promise<string | null> => {
  try {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      return null;
    }

    const { refreshToken: refreshTokenAPI } = await import('./auth');
    const response = await refreshTokenAPI({ refreshToken });
    
    // 새 토큰 저장
    setToken(response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    
    return response.accessToken;
  } catch (error) {
    // Refresh Token도 만료된 경우
    removeToken();
    localStorage.removeItem('refreshToken');
    return null;
  }
};

/**
 * API 호출 함수
 */
export const apiClient = async <T>(
  endpoint: string,
  options: RequestOptions = {},
  retryOn401: boolean = true
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
      // 401 에러이고 retryOn401이 true인 경우 토큰 갱신 시도
      if (response.status === 401 && requiresAuth && retryOn401) {
        const newToken = await refreshAccessToken();
        if (newToken) {
          // 새 토큰으로 재시도 (무한 루프 방지를 위해 retryOn401을 false로)
          headers['Authorization'] = `Bearer ${newToken}`;
          const retryResponse = await fetch(url, {
            ...fetchOptions,
            headers,
          });
          
          if (!retryResponse.ok) {
            const retryErrorData = await retryResponse.json().catch(() => ({}));
            if (retryResponse.status === 401 || retryResponse.status === 403) {
              const errorMessage = retryErrorData.message || '인증이 필요합니다';
              throw new AuthenticationError(errorMessage);
            }
            const errorMessage = retryErrorData.message || retryErrorData.error || `HTTP error! status: ${retryResponse.status}`;
            throw new Error(errorMessage);
          }
          
          const retryData = await retryResponse.json();
          if (retryData.data !== undefined) {
            return retryData.data as T;
          }
          return retryData as T;
        } else {
          // 토큰 갱신 실패
          throw new AuthenticationError('인증이 필요합니다');
        }
      }
      
      // 에러 응답 한 번만 읽기
      const errorData = await response.json().catch(() => ({}));
      
      // 500 에러는 서버 문제이므로 조용히 처리 (개발 환경에서만 로그 출력)
      const isServerError = response.status >= 500;
      if (!isServerError) {
        // 에러 응답 상세 로깅 (500 에러 제외)
        console.error('API Error Response:', {
          status: response.status,
          statusText: response.statusText,
          url: url,
          method: fetchOptions.method,
          requiresAuth: requiresAuth,
          errorData: errorData
        });
      }
      
      // 401, 403은 인증 에러로 처리 (단, requiresAuth가 false인 경우는 일반 에러로 처리)
      if ((response.status === 401 || response.status === 403) && requiresAuth) {
        const errorMessage = errorData.message || '인증이 필요합니다';
        throw new AuthenticationError(errorMessage);
      }

      // requiresAuth가 false인 경우 (로그인 API 등)에도 백엔드 메시지 전달
      const errorMessage = errorData.message || errorData.error || `HTTP error! status: ${response.status}`;
      const error = new Error(errorMessage);
      // status 정보를 에러 객체에 추가
      (error as any).status = response.status;
      (error as any).response = { status: response.status };
      throw error;
    }

    // 응답 본문이 있는지 확인
    const contentLength = response.headers.get('content-length');
    const contentType = response.headers.get('content-type');
    
    // 본문이 없거나 빈 응답인 경우 (DELETE 성공 등)
    if (contentLength === '0' || !contentType?.includes('application/json')) {
      // void 타입이거나 빈 응답인 경우
      return undefined as T;
    }

    // 응답 본문 읽기 시도
    const text = await response.text();
    
    // 빈 문자열인 경우
    if (!text || text.trim() === '') {
      return undefined as T;
    }

    // JSON 파싱
    const data = JSON.parse(text);

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
    
    // Error 객체에서 메시지를 확인하여 500 에러인지 판단
    const errorMessage = error instanceof Error ? error.message : String(error);
    const isServerError = errorMessage.includes('500') || errorMessage.includes('Internal Server Error');
    
    // 500 에러는 서버 문제이므로 조용히 처리 (개발 환경에서만 로그 출력)
    if (!isServerError) {
      console.error('API request failed:', {
        url,
        method: fetchOptions.method,
        error: error
      });
    }
    
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
  }, true);
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
  }, true);
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
  }, true);
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
  }, true);
};

/**
 * DELETE 요청
 */
export const del = <T>(endpoint: string, requiresAuth = true): Promise<T> => {
  return apiClient<T>(endpoint, {
    method: 'DELETE',
    requiresAuth,
  }, true);
};
