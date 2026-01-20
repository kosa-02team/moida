/**
 * 알림(Notification) 관련 API
 */

import { get, put, del } from './client';

// ==================== Types ====================

export interface NotificationResponse {
  id: number;
  content: string;
  refId: number | null;
  type: string;
  isRead: boolean;
  createdAt: string;
  clubId?: number | null; // 알림과 관련된 모임 ID (선택사항)
}

export interface NotificationPageResponse {
  content: NotificationResponse[];
  pageable?: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  empty?: boolean;
  numberOfElements?: number;
}

/**
 * SSE 이벤트 리스너 타입
 */
export type NotificationEventListener = (notification: NotificationResponse) => void;

/**
 * SSE 연결 설정 옵션
 */
export interface NotificationSubscribeOptions {
  onMessage?: NotificationEventListener;
  onError?: (error: Event) => void;
  onOpen?: () => void;
  onClose?: () => void;
}

/**
 * 알림 구독 (Server-Sent Events)
 * @param options 이벤트 리스너 옵션
 * @returns EventSource 인스턴스 (연결 해제 시 close() 호출 필요)
 */
export const subscribeNotifications = (
  options?: NotificationSubscribeOptions
): EventSource => {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  const token = localStorage.getItem('token');
  
  // SSE는 EventSource를 사용하므로, 토큰을 쿼리 파라미터로 전달
  // 백엔드에서 @AuthenticationPrincipal로 인증을 처리하므로,
  // Authorization 헤더는 EventSource에서 직접 지원하지 않으므로
  // 쿠키나 쿼리 파라미터를 통해 토큰을 전달해야 할 수 있습니다.
  // 현재는 withCredentials로 쿠키를 포함시키고, 백엔드에서 쿠키로 인증 처리
  
  let url = `${API_BASE_URL}/api/notifications/subscribe`;
  if (token) {
    url += `?token=${encodeURIComponent(token)}`;
  }
  
  const eventSource = new EventSource(url, {
    withCredentials: true, // 쿠키 포함
  });

  // 'notification' 이벤트 타입으로 메시지 수신
  if (options?.onMessage) {
    eventSource.addEventListener('notification', (event: MessageEvent) => {
      try {
        const notification: NotificationResponse = JSON.parse(event.data);
        options.onMessage!(notification);
      } catch (error) {
        console.error('Failed to parse notification:', error);
      }
    });
    
    // 'test' 이벤트도 처리 (연결 확인용)
    eventSource.addEventListener('test', (event: MessageEvent) => {
      console.log('SSE connection established:', event.data);
    });
  }

  if (options?.onError) {
    eventSource.onerror = options.onError;
  }

  if (options?.onOpen) {
    eventSource.onopen = options.onOpen;
  }

  // EventSource는 onclose 이벤트를 직접 지원하지 않으므로,
  // error 이벤트에서 readyState를 확인하여 처리할 수 있습니다.
  if (options?.onClose) {
    const originalOnError = eventSource.onerror;
    eventSource.onerror = (error) => {
      if (eventSource.readyState === EventSource.CLOSED) {
        options.onClose!();
      }
      if (originalOnError) {
        originalOnError.call(eventSource, error);
      }
    };
  }

  return eventSource;
};

/**
 * 알림 목록 조회 (페이징)
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기
 * @param isRead 읽음 여부 필터 (선택사항)
 */
export const getNotifications = async (
  page: number = 0,
  size: number = 20,
  isRead?: boolean
): Promise<NotificationPageResponse> => {
  let url = `/api/notifications?page=${page}&size=${size}`;
  if (isRead !== undefined) {
    url += `&isRead=${isRead}`;
  }
  return get<NotificationPageResponse>(url);
};

/**
 * 읽지 않은 알림 개수 조회
 */
export const getUnreadCount = async (): Promise<number> => {
  return get<number>('/api/notifications/unread-count');
};

/**
 * 알림 읽음 처리
 * @param notificationId 알림 ID
 */
export const markAsRead = async (notificationId: number): Promise<void> => {
  return put<void>(`/api/notifications/${notificationId}/read`);
};

/**
 * 알림 삭제
 * @param notificationId 알림 ID
 */
export const deleteNotification = async (notificationId: number): Promise<void> => {
  return del<void>(`/api/notifications/${notificationId}`);
};
