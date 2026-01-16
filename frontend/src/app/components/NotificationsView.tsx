import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Bell, Users, Calendar, CreditCard, Check, Trash2 } from 'lucide-react';
import { Button } from './ui/button';
import { Badge } from './ui/badge';

interface Notification {
  id: string;
  type: 'member' | 'schedule' | 'dues' | 'general';
  title: string;
  message: string;
  time: string;
  isRead: boolean;
  groupId?: string;
  groupName?: string;
}

export function NotificationsView() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<Notification[]>([
    {
      id: '1',
      type: 'member',
      title: '새 멤버 가입 신청',
      message: '정수진님이 "주말 등산 클럽" 모임에 가입을 요청했습니다.',
      time: '10분 전',
      isRead: false,
      groupId: '1',
      groupName: '주말 등산 클럽',
    },
    {
      id: '2',
      type: 'schedule',
      title: '일정 투표 마감 임박',
      message: '"4월 정기 산행" 일정 투표가 내일 마감됩니다.',
      time: '1시간 전',
      isRead: false,
      groupId: '1',
      groupName: '주말 등산 클럽',
    },
    {
      id: '3',
      type: 'dues',
      title: '회비 납부 알림',
      message: '4월 회비 납부 마감일이 5일 남았습니다. (30,000원)',
      time: '3시간 전',
      isRead: true,
      groupId: '1',
      groupName: '주말 등산 클럽',
    },
    {
      id: '4',
      type: 'general',
      title: '새 스토리 등록',
      message: '홍길동님이 새 스토리를 등록했습니다.',
      time: '어제',
      isRead: true,
      groupId: '1',
      groupName: '주말 등산 클럽',
    },
    {
      id: '5',
      type: 'dues',
      title: '정산 요청 승인',
      message: '"3월 번개 모임 식사비" 정산이 승인되었습니다.',
      time: '2일 전',
      isRead: true,
      groupId: '1',
      groupName: '주말 등산 클럽',
    },
  ]);

  const unreadCount = notifications.filter(n => !n.isRead).length;

  const getIcon = (type: string) => {
    switch (type) {
      case 'member':
        return <Users className="w-5 h-5" />;
      case 'schedule':
        return <Calendar className="w-5 h-5" />;
      case 'dues':
        return <CreditCard className="w-5 h-5" />;
      default:
        return <Bell className="w-5 h-5" />;
    }
  };

  const getIconBg = (type: string) => {
    switch (type) {
      case 'member':
        return 'bg-green-100 text-green-600';
      case 'schedule':
        return 'bg-blue-100 text-blue-600';
      case 'dues':
        return 'bg-orange-100 text-orange-600';
      default:
        return 'bg-stone-100 text-stone-600';
    }
  };

  const markAsRead = (id: string) => {
    setNotifications(notifications.map(n =>
      n.id === id ? { ...n, isRead: true } : n
    ));
  };

  const markAllAsRead = () => {
    setNotifications(notifications.map(n => ({ ...n, isRead: true })));
  };

  const deleteNotification = (id: string) => {
    setNotifications(notifications.filter(n => n.id !== id));
  };

  const handleNotificationClick = (notification: Notification) => {
    markAsRead(notification.id);
    if (notification.groupId) {
      // Navigate based on notification type
      switch (notification.type) {
        case 'member':
          navigate(`/group/${notification.groupId}/admin/members`);
          break;
        case 'schedule':
          navigate(`/group/${notification.groupId}/schedule`);
          break;
        case 'dues':
          navigate(`/group/${notification.groupId}/dues`);
          break;
        default:
          navigate(`/group/${notification.groupId}`);
      }
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
              onClick={markAllAsRead}
              className="text-orange-600 hover:text-orange-700"
            >
              모두 읽음
            </Button>
          )}
          {unreadCount === 0 && <div className="w-10" />}
        </div>
      </header>

      <div className="p-5">
        {notifications.length > 0 ? (
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
                      <div>
                        <div className="flex items-center gap-2">
                          <p className="font-semibold text-stone-900">{notification.title}</p>
                          {!notification.isRead && (
                            <span className="w-2 h-2 bg-orange-500 rounded-full" />
                          )}
                        </div>
                        {notification.groupName && (
                          <Badge variant="secondary" className="text-xs mt-1 bg-stone-100 text-stone-600">
                            {notification.groupName}
                          </Badge>
                        )}
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={(e) => {
                          e.stopPropagation();
                          deleteNotification(notification.id);
                        }}
                        className="h-8 w-8 text-stone-400 hover:text-red-500 -mr-2"
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                    <p className="text-sm text-stone-600 mt-1">{notification.message}</p>
                    <p className="text-xs text-stone-400 mt-2">{notification.time}</p>
                  </div>
                </div>
              </div>
            ))}
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

