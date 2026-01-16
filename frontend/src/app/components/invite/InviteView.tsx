import { useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { Users, Calendar, MapPin, Check, X, Mountain } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../ui/avatar';

export function InviteView() {
  const navigate = useNavigate();
  const { inviteCode } = useParams();
  const [status, setStatus] = useState<'pending' | 'accepted' | 'declined'>('pending');
  const [isLoggedIn] = useState(false);

  // Mock data - 실제로는 inviteCode로 초대 정보 조회
  const invite = {
    code: inviteCode,
    group: {
      id: '1',
      name: '주말 등산 클럽',
      image: 'https://images.unsplash.com/photo-1551632811-561732d1e306?w=500&auto=format&fit=crop&q=60',
      description: '매주 토요일 서울 근교 산행합니다. 초보자 환영!',
      memberCount: 15,
      maxMembers: 50,
      location: '서울',
    },
    inviter: {
      name: '홍길동',
      avatar: '',
    },
    expiresAt: '2024.04.30',
  };

  const handleAccept = () => {
    if (!isLoggedIn) {
      toast.info('로그인이 필요합니다');
      navigate('/login');
      return;
    }
    
    setStatus('accepted');
    toast.success('초대를 수락했습니다! 모임에 가입되었습니다.');
    setTimeout(() => {
      navigate(`/group/${invite.group.id}`);
    }, 1500);
  };

  const handleDecline = () => {
    setStatus('declined');
    toast.info('초대를 거절했습니다');
  };

  if (status === 'accepted') {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-50 via-white to-stone-100 flex flex-col items-center justify-center p-6">
        <div className="w-20 h-20 bg-green-500 rounded-full flex items-center justify-center mb-6 animate-in zoom-in duration-300">
          <Check className="w-10 h-10 text-white" />
        </div>
        <h1 className="text-2xl font-bold text-stone-900 mb-2">환영합니다! 🎉</h1>
        <p className="text-stone-500 mb-6">"{invite.group.name}"에 가입되었습니다</p>
        <p className="text-sm text-stone-400">잠시 후 모임 페이지로 이동합니다...</p>
      </div>
    );
  }

  if (status === 'declined') {
    return (
      <div className="min-h-screen bg-stone-50 flex flex-col items-center justify-center p-6">
        <div className="w-20 h-20 bg-stone-200 rounded-full flex items-center justify-center mb-6">
          <X className="w-10 h-10 text-stone-500" />
        </div>
        <h1 className="text-xl font-bold text-stone-900 mb-2">초대를 거절했습니다</h1>
        <p className="text-stone-500 mb-6">나중에 마음이 바뀌면 초대 링크를 다시 사용할 수 있습니다.</p>
        <Link to="/">
          <Button className="bg-orange-500 hover:bg-orange-600">
            홈으로 가기
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-50 via-white to-stone-100">
      {/* Header */}
      <header className="p-4 flex justify-center">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 bg-orange-500 rounded-lg flex items-center justify-center">
            <Mountain className="w-5 h-5 text-white" />
          </div>
          <span className="font-bold text-stone-800">모임 관리</span>
        </div>
      </header>

      <div className="p-6 max-w-md mx-auto">
        {/* Invite Card */}
        <div className="bg-white rounded-3xl shadow-xl overflow-hidden border border-stone-100">
          {/* Group Image */}
          <div className="h-40 bg-stone-200 relative">
            <img
              src={invite.group.image}
              alt={invite.group.name}
              className="w-full h-full object-cover"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent" />
          </div>

          {/* Content */}
          <div className="p-6 space-y-4">
            {/* Inviter */}
            <div className="flex items-center gap-3 pb-4 border-b border-stone-100">
              <Avatar className="w-10 h-10">
                <AvatarImage src={invite.inviter.avatar} />
                <AvatarFallback>{invite.inviter.name[0]}</AvatarFallback>
              </Avatar>
              <div>
                <p className="text-sm text-stone-500">
                  <span className="font-medium text-stone-900">{invite.inviter.name}</span>님이 초대했습니다
                </p>
              </div>
            </div>

            {/* Group Info */}
            <div className="space-y-3">
              <h1 className="text-2xl font-bold text-stone-900">{invite.group.name}</h1>
              <p className="text-stone-600">{invite.group.description}</p>
              
              <div className="flex items-center gap-4 text-sm text-stone-500">
                <span className="flex items-center gap-1">
                  <MapPin className="w-4 h-4" />
                  {invite.group.location}
                </span>
                <span className="flex items-center gap-1">
                  <Users className="w-4 h-4" />
                  {invite.group.memberCount}/{invite.group.maxMembers}명
                </span>
              </div>
            </div>

            {/* Expiry */}
            <div className="bg-orange-50 rounded-xl p-3">
              <p className="text-xs text-orange-700">
                ⏰ 이 초대는 <span className="font-medium">{invite.expiresAt}</span>까지 유효합니다
              </p>
            </div>

            {/* Actions */}
            <div className="space-y-3 pt-2">
              <Button
                onClick={handleAccept}
                className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl"
              >
                초대 수락하기
              </Button>
              <Button
                variant="outline"
                onClick={handleDecline}
                className="w-full h-12 rounded-xl text-stone-600"
              >
                거절하기
              </Button>
            </div>

            {/* Login Notice */}
            {!isLoggedIn && (
              <p className="text-xs text-center text-stone-500">
                초대 수락을 위해 <Link to="/login" className="text-orange-600 underline">로그인</Link>이 필요합니다
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}



