import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Bell, Users, Calendar, Trash2, MessageSquare } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { getNotifications, markAsRead, deleteNotification, getUnreadCount, type NotificationResponse } from '../../../api/notification';

export function NotificationsView() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  useEffect(() => {
    async function fetchNotifications() {
      try {
        setLoading(true);
        const [notificationsData, unreadCountData] = await Promise.all([
          getNotifications(page, 20),
          getUnreadCount()
        ]);
        // 첫 페이지면 교체, 아니면 추가
        if (page === 0) {
          setNotifications(notificationsData.content);
        } else {
          setNotifications(prev => [...prev, ...notificationsData.content]);
        }
        setUnreadCount(unreadCountData);
        setHasMore(!notificationsData.last);
      } catch (error) {
        console.error('알림 목록 불러오기 실패:', error);
        toast.error('알림 목록을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchNotifications();
  }, [page]);

  const getTypeLabel = (type: string): string => {
    switch (type) {
      case 'SCHEDULE':
        return '일정';
      case 'POST':
        return '게시글';
      case 'COMMENT':
        return '댓글';
      case 'CLUB_WELCOME':
        return '모임 가입 환영';
      case 'VOTE_DEADLINE':
        return '투표 마감 임박';
      default:
        return type;
    }
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'CLUB_WELCOME':
        return <Users className="w-5 h-5" />;
      case 'SCHEDULE':
        return <Calendar className="w-5 h-5" />;
      case 'VOTE_DEADLINE':
        return <Calendar className="w-5 h-5" />;
      case 'POST':
        return <Bell className="w-5 h-5" />;
      case 'COMMENT':
        return <MessageSquare className="w-5 h-5" />;
      default:
        return <Bell className="w-5 h-5" />;
    }
  };

  const getIconBg = (type: string) => {
    switch (type) {
      case 'CLUB_WELCOME':
        return 'bg-green-100 text-green-600';
      case 'SCHEDULE':
        return 'bg-blue-100 text-blue-600';
      case 'VOTE_DEADLINE':
        return 'bg-orange-100 text-orange-600';
      case 'POST':
        return 'bg-purple-100 text-purple-600';
      case 'COMMENT':
        return 'bg-indigo-100 text-indigo-600';
      default:
        return 'bg-stone-100 text-stone-600';
    }
  };

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return '방금 전';
    if (diffMins < 60) return `${diffMins}분 전`;
    if (diffHours < 24) return `${diffHours}시간 전`;
    if (diffDays < 7) return `${diffDays}일 전`;
    return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
  };

  const handleMarkAsRead = async (id: number) => {
    try {
      await markAsRead(id);
      setNotifications(notifications.map(n =>
        n.id === id ? { ...n, isRead: true } : n
      ));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      console.error('알림 읽음 처리 실패:', error);
      toast.error('알림 읽음 처리에 실패했습니다.');
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      const unreadNotifications = notifications.filter(n => !n.isRead);
      await Promise.all(unreadNotifications.map(n => markAsRead(n.id)));
      setNotifications(notifications.map(n => ({ ...n, isRead: true })));
      setUnreadCount(0);
      toast.success('모든 알림을 읽음 처리했습니다.');
    } catch (error) {
      console.error('모든 알림 읽음 처리 실패:', error);
      toast.error('알림 읽음 처리에 실패했습니다.');
    }
  };

  const handleDeleteNotification = async (id: number) => {
    try {
      await deleteNotification(id);
      const deletedNotification = notifications.find(n => n.id === id);
      if (deletedNotification && !deletedNotification.isRead) {
        setUnreadCount(prev => Math.max(0, prev - 1));
      }
      setNotifications(notifications.filter(n => n.id !== id));
    } catch (error) {
      console.error('알림 삭제 실패:', error);
      toast.error('알림 삭제에 실패했습니다.');
    }
  };

  const handleNotificationClick = async (notification: NotificationResponse) => {
    if (!notification.isRead) {
      handleMarkAsRead(notification.id);
    }
    
    // refId와 type, clubId에 따라 적절한 페이지로 이동
    if (!notification.refId) {
      return;
    }

    try {
      const clubId = notification.clubId || notification.refId; // CLUB_WELCOME의 경우 refId가 clubId

      switch (notification.type) {
        case 'SCHEDULE': {
          if (clubId && notification.refId) {
            navigate(`/group/${clubId}/schedule/${notification.refId}`);
          } else {
            toast.info('일정 상세 페이지로 이동하려면 일정 목록에서 선택해주세요.');
          }
          break;
        }
        case 'POST':
        case 'COMMENT': {
          if (clubId && notification.refId) {
            navigate(`/group/${clubId}/stories/${notification.refId}`);
          } else {
            toast.info('게시글 상세 페이지로 이동하려면 스토리 목록에서 선택해주세요.');
          }
          break;
        }
        case 'VOTE_DEADLINE': {
          if (clubId && notification.refId) {
            navigate(`/group/${clubId}/vote/${notification.refId}`);
          } else {
            toast.info('투표 상세 페이지로 이동하려면 투표 목록에서 선택해주세요.');
          }
          break;
        }
        case 'CLUB_WELCOME': {
          // 모임 가입 환영 알림은 모임 메인으로 이동
          // refId가 clubId인 경우
          if (notification.refId) {
            navigate(`/group/${notification.refId}`);
          }
          break;
        }
        default:
          break;
      }
    } catch (error) {
      console.error('알림 클릭 처리 실패:', error);
      toast.error('페이지 이동에 실패했습니다.');
    }
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100 backdrop-blur-sm bg-white/95">
        <div className="flex items-center justify-between px-4 py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="font-bold text-lg text-stone-800">알림</h1>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              onClick={handleMarkAllAsRead}
              className="text-orange-600 hover:text-orange-700"
            >
              모두 읽음
            </Button>
          )}
          {unreadCount === 0 && <div className="w-10" />}
        </div>
      </header>

      <div className="p-5">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <div className="text-stone-500">로딩 중...</div>
          </div>
        ) : notifications.length > 0 ? (
          <div className="space-y-3">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                onClick={() => handleNotificationClick(notification)}
                className={`bg-white rounded-xl border p-4 cursor-pointer transition-all hover:shadow-md ${
                  notification.isRead ? 'border-stone-100' : 'border-orange-200 bg-orange-50/30'
                }`}
              >
                <div className="flex items-start gap-3">
                  <div className={`p-2 rounded-lg ${getIconBg(notification.type)}`}>
                    {getIcon(notification.type)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <p className="font-semibold text-stone-900 break-words">{notification.content}</p>
                          {!notification.isRead && (
                            <span className="w-2 h-2 bg-orange-500 rounded-full flex-shrink-0" />
                          )}
                        </div>
                        <div className="flex items-center gap-2 mt-1.5">
                          {notification.type && (
                            <Badge variant="secondary" className="text-xs bg-stone-100 text-stone-600">
                              {getTypeLabel(notification.type)}
                            </Badge>
                          )}
                          <p className="text-xs text-stone-400">{formatTime(notification.createdAt)}</p>
                        </div>
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteNotification(notification.id);
                        }}
                        className="h-8 w-8 text-stone-400 hover:text-red-500 -mr-2 flex-shrink-0"
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            ))}
            {hasMore && (
              <Button
                variant="outline"
                className="w-full mt-4"
                onClick={() => setPage(prev => prev + 1)}
              >
                더 보기
              </Button>
            )}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <div className="w-16 h-16 bg-stone-100 rounded-full flex items-center justify-center mb-4">
              <Bell className="w-8 h-8 text-stone-400" />
            </div>
            <h3 className="text-lg font-semibold text-stone-700 mb-2">알림이 없습니다</h3>
            <p className="text-sm text-stone-500">새로운 알림이 오면 여기에 표시됩니다.</p>
          </div>
        )}
      </div>
    </div>
  );
}

