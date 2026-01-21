import { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { Users, Calendar, MapPin, Check, X, Mountain } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../ui/avatar';
import { getClubByInviteCode, type ClubDetailResponse } from '@/api/club-full';
import { getMyInfo } from '@/api/user';
import { joinClub } from '@/api/member';
import { getToken } from '@/api/client';

export function InviteView() {
  const navigate = useNavigate();
  const { inviteCode } = useParams();
  const [status, setStatus] = useState<'pending' | 'accepted' | 'declined'>('pending');
  const [loading, setLoading] = useState(true);
  const [clubData, setClubData] = useState<ClubDetailResponse | null>(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    async function fetchInviteData() {
      if (!inviteCode) {
        toast.error('초대 코드가 없습니다');
        navigate('/');
        return;
      }

      try {
        setLoading(true);
        
        // 로그인 상태 확인
        const token = getToken();
        if (token) {
          try {
            await getMyInfo();
            setIsLoggedIn(true);
          } catch (error) {
            setIsLoggedIn(false);
          }
        }

        // 초대 코드로 모임 정보 조회
        const club = await getClubByInviteCode(inviteCode);
        setClubData(club);
      } catch (error) {
        console.error('초대 정보 조회 실패:', error);
        toast.error('초대 정보를 불러오는데 실패했습니다');
        navigate('/');
      } finally {
        setLoading(false);
      }
    }

    fetchInviteData();
  }, [inviteCode, navigate]);

  const handleAccept = async () => {
    if (!isLoggedIn) {
      toast.info('로그인이 필요합니다');
      navigate('/login');
      return;
    }
    
    if (!inviteCode || !clubData) {
      toast.error('초대 정보가 없습니다');
      return;
    }
    
    try {
      setStatus('accepted');
      await joinClub(clubData.clubId, { nickname: '' });
      toast.success('초대를 수락했습니다! 모임에 가입되었습니다.');
      setTimeout(() => {
        navigate(`/group/${clubData.clubId}`);
      }, 1500);
    } catch (error: any) {
      console.error('모임 가입 실패:', error);
      toast.error(error.message || '모임 가입에 실패했습니다');
      setStatus('pending');
    }
  };

  const handleDecline = () => {
    setStatus('declined');
    toast.info('초대를 거절했습니다');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  if (!clubData) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">초대 정보를 불러올 수 없습니다</div>
      </div>
    );
  }

  if (status === 'accepted') {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-50 via-white to-stone-100 flex flex-col items-center justify-center p-6">
        <div className="w-20 h-20 bg-green-500 rounded-full flex items-center justify-center mb-6 animate-in zoom-in duration-300">
          <Check className="w-10 h-10 text-white" />
        </div>
        <h1 className="text-2xl font-bold text-stone-900 mb-2">환영합니다! 🎉</h1>
        <p className="text-stone-500 mb-6">"{clubData?.clubName}"에 가입되었습니다</p>
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
          <div className="h-40 bg-gradient-to-br from-orange-100 to-orange-50 relative flex items-center justify-center">
            {clubData && (() => {
              const clubImage = localStorage.getItem(`club_image_${clubData.clubId}`);
              if (clubImage) {
                return (
                  <>
                    <img
                      src={clubImage}
                      alt={clubData.clubName}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        const target = e.target as HTMLImageElement;
                        target.style.display = 'none';
                      }}
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent" />
                  </>
                );
              }
              return (
                <div className="text-5xl font-bold text-orange-200">
                  {clubData.clubName ? clubData.clubName[0] : '모'}
                </div>
              );
            })()}
          </div>

          {/* Content */}
          <div className="p-6 space-y-4">
            {/* Group Info */}
            <div className="space-y-3">
              <h1 className="text-2xl font-bold text-stone-900">{clubData?.clubName || '모임'}</h1>
              
              <div className="flex items-center gap-4 text-sm text-stone-500">
                <span className="flex items-center gap-1">
                  <Users className="w-4 h-4" />
                  {clubData?.currentMembers || 0}/{clubData?.maxMembers || 100}명
                </span>
              </div>
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



