import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { 
  ArrowLeft, Users, Calendar, Eye, Lock, Heart, MessageCircle, Flag, Share2, UserPlus
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Card, CardContent } from '../ui/card';
import { Badge } from '../ui/badge';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { ReportDialog } from '../report/ReportDialog';
import { getClub } from '@/api/club-full';
import { get, AuthenticationError } from '@/api/client';
import { PostCardResponse } from '@/api/post';
import { joinClub } from '@/api/member';
import { getToken } from '@/api/client';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';

interface PublicGroup {
  id: string;
  name: string;
  image: string;
  description: string;
  tags: string[];
  memberCount: number;
  maxMembers: number;
  type: 'club' | 'meetup' | 'study';
  location?: string;
  ownerId?: number; // 모임 신고를 위한 owner ID
  isPostPublic: boolean;
  nextEvent?: {
    title: string;
    date: string;
    location: string;
  };
  recentPosts?: {
    id: string;
    user: string;
    image: string;
    content: string;
    likes: number;
    comments: number;
    date: string;
  }[];
}

export function ExploreGroupDetailView() {
  const { groupId } = useParams();
  const navigate = useNavigate();
  const [showReportDialog, setShowReportDialog] = useState(false);
  const [showJoinDialog, setShowJoinDialog] = useState(false);
  const [joinNickname, setJoinNickname] = useState('');
  const [isJoining, setIsJoining] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [group, setGroup] = useState<PublicGroup | null>(null);
  const [loading, setLoading] = useState(true);

  // 로그인 상태 확인
  useEffect(() => {
    const token = getToken();
    setIsLoggedIn(!!token);
  }, []);

  useEffect(() => {
    async function fetchGroup() {
      if (!groupId) {
        toast.error('모임 ID가 없습니다.');
        navigate('/explore');
        return;
      }

      try {
        setLoading(true);
        const clubId = parseInt(groupId);
        const clubData = await getClub(clubId);
        
        // 최근 게시글 조회 (게시글 공개 모임인 경우)
        let recentPosts: PublicGroup['recentPosts'] = undefined;
        if (clubData.visibility === 'PUBLIC') {
          try {
            const postsData = await get<PostCardResponse[]>(`/api/clubs/${clubId}/posts/recent?page=0&size=3`);
            recentPosts = postsData.slice(0, 3).map(post => ({
              id: post.postId.toString(),
              user: post.writerName || '익명',
              image: post.imagesUrl && post.imagesUrl.length > 0 ? post.imagesUrl[0] : '',
              content: post.content || '',
              likes: post.postLikes || 0,
              comments: post.commentCount || 0,
              date: post.createdAt ? new Date(post.createdAt).toLocaleDateString('ko-KR') : '',
            }));
          } catch (error) {
            console.error('게시글 조회 실패:', error);
          }
        }

        // 로컬 스토리지에서 이미지 가져오기
        const imageKey = `club_image_${clubData.clubId}`;
        const storedImage = localStorage.getItem(imageKey);
        console.log('=== 클럽 이미지 디버깅 정보 ===');
        console.log('클럽 ID:', clubData.clubId);
        console.log('이미지 키:', imageKey);
        console.log('이미지 존재 여부:', !!storedImage);
        console.log('이미지 값:', storedImage);
        console.log('이미지 길이:', storedImage?.length || 0);
        console.log('이미지 시작 부분:', storedImage?.substring(0, 100));
        console.log('로컬 스토리지 전체 키:', Object.keys(localStorage).filter(key => key.includes('club_image')));
        console.log('==========================');
        
        const groupData: PublicGroup = {
          id: clubData.clubId.toString(),
          name: clubData.clubName,
          image: storedImage || '',
          description: '', // 백엔드에 description 필드가 없음
          tags: [], // 백엔드에 tags 필드가 없음
          memberCount: clubData.currentMembers || 0,
          maxMembers: clubData.maxMembers || 100,
          type: clubData.type === 'OPERATION_FEE' ? 'meetup' : clubData.type === 'FAIR_SETTLEMENT' ? 'club' : 'study',
          location: '', // 백엔드에 location 필드가 없음
          isPostPublic: clubData.visibility === 'PUBLIC',
          nextEvent: undefined, // 백엔드에 nextEvent 필드가 없음
          recentPosts,
        };

        setGroup(groupData);
      } catch (error: any) {
        console.error('모임 정보 불러오기 실패:', error);
        console.error('에러 상세:', {
          status: error?.status,
          message: error?.message,
          response: error?.response,
          errorData: error?.response?.data
        });
        
        // 비공개 모임 접근 권한 없음 에러 처리
        const errorMessage = error?.message || '';
        const errorData = error?.response?.data || {};
        // 백엔드 ErrorResponse 구조: { status, code, message }
        const errorCode = errorData?.code || errorData?.errorCode || '';
        
        // AuthenticationError 체크 (client.ts에서 403 에러 시 던지는 에러)
        const isAuthenticationError = error instanceof AuthenticationError;
        
        // 403 에러 또는 CA01 에러 코드 또는 관련 메시지 확인
        const isPermissionError = error?.status === 403 || 
                                  errorCode === 'CA01' ||
                                  isAuthenticationError ||
                                  errorMessage.includes('활성 멤버가 아닙니다') ||
                                  errorMessage.includes('권한') ||
                                  errorMessage.includes('멤버가 아닙니다');
        
        if (isPermissionError) {
          toast.error('비공개 모임입니다. 해당 모임의 멤버만 상세 정보를 조회할 수 있습니다.');
          navigate('/explore');
          return;
        }
        
        toast.error('모임 정보를 불러오는데 실패했습니다.');
        navigate('/explore');
      } finally {
        setLoading(false);
      }
    }

    fetchGroup();
  }, [groupId, navigate]);

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  if (!group) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">모임을 찾을 수 없습니다.</div>
      </div>
    );
  }

  // 렌더링 시점에 이미지 상태 확인
  console.log('=== 렌더링 시점 이미지 확인 ===');
  console.log('group.image 존재:', !!group.image);
  console.log('group.image 값:', group.image);
  console.log('group.image 길이:', group.image?.length || 0);
  console.log('group.image 시작:', group.image?.substring(0, 50));
  console.log('============================');

  const handleJoinRequest = async () => {
    if (!joinNickname.trim()) {
      toast.error('닉네임을 입력해주세요.');
      return;
    }
    if (!groupId) return;
    
    try {
      setIsJoining(true);
      await joinClub(Number(groupId), {
        nickname: joinNickname.trim()
      });
      toast.success('가입 신청이 완료되었습니다. 승인을 기다려주세요.');
      setShowJoinDialog(false);
      setJoinNickname('');
      // 모임 페이지로 이동
      navigate(`/group/${groupId}`);
    } catch (error) {
      console.error('가입 신청 실패:', error);
      toast.error('가입 신청에 실패했습니다.');
    } finally {
      setIsJoining(false);
    }
  };

  const handleShare = () => {
    navigator.clipboard.writeText(window.location.href);
    toast.success('링크가 복사되었습니다');
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-32">
      {/* Header Image */}
      <div className="relative h-56 bg-stone-200 overflow-hidden">
        {/* Background Image or Placeholder */}
        {(() => {
          console.log('렌더링 시점 - group.image 값:', group.image ? '있음' : '없음', group.image?.substring(0, 50));
          return group.image && group.image.trim() ? (
            <img
              src={group.image}
              alt={group.name}
              className="w-full h-full object-cover"
              style={{ display: 'block', position: 'relative', zIndex: 0 }}
              onError={(e) => {
                console.error('=== 이미지 로드 실패 ===');
                console.error('이미지 src 시작 부분:', group.image?.substring(0, 100));
                console.error('이미지 src 길이:', group.image?.length);
                console.error('에러 이벤트:', e);
                e.currentTarget.style.display = 'none';
              }}
              onLoad={(e) => {
                console.log('=== 이미지 로드 성공 ===');
                console.log('이미지 너비:', e.currentTarget.naturalWidth);
                console.log('이미지 높이:', e.currentTarget.naturalHeight);
                console.log('이미지 src 시작 부분:', e.currentTarget.src.substring(0, 100));
              }}
            />
          ) : (
            <div className="w-full h-full bg-gradient-to-br from-orange-100 to-orange-200 flex items-center justify-center">
              <span className="text-6xl font-bold text-orange-400">
                {group.name[0]}
              </span>
            </div>
          );
        })()}
        {/* Gradient Overlay - 더 밝게 조정 */}
        <div className="absolute inset-0 bg-gradient-to-b from-black/20 to-black/50 pointer-events-none" style={{ zIndex: 1 }} />
        
        {/* Back Button */}
        <Button
          variant="ghost"
          size="icon"
          onClick={() => navigate(-1)}
          className="absolute top-4 left-4 bg-black/30 text-white hover:bg-black/50 rounded-full"
        >
          <ArrowLeft className="w-5 h-5" />
        </Button>

        {/* Actions */}
        <div className="absolute top-4 right-4 flex gap-2">
          <Button
            variant="ghost"
            size="icon"
            onClick={handleShare}
            className="bg-black/30 text-white hover:bg-black/50 rounded-full"
          >
            <Share2 className="w-5 h-5" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setShowReportDialog(true)}
            className="bg-black/30 text-white hover:bg-black/50 rounded-full"
          >
            <Flag className="w-5 h-5" />
          </Button>
        </div>

        {/* Group Info Overlay */}
        <div className="absolute bottom-0 left-0 right-0 p-4">
          <div className="flex items-center gap-2 mb-2">
            <Badge className={group.isPostPublic ? 'bg-green-500 text-white' : 'bg-stone-700 text-white'}>
              {group.isPostPublic ? (
                <><Eye className="w-3 h-3 mr-1" />게시글 공개</>
              ) : (
                <><Lock className="w-3 h-3 mr-1" />게시글 비공개</>
              )}
            </Badge>
            <Badge variant="secondary" className="bg-white/90">
              {group.type === 'club' ? '동아리' : group.type === 'study' ? '스터디' : '정모'}
            </Badge>
          </div>
          <h1 className="text-2xl font-bold text-white">{group.name}</h1>
        </div>
      </div>

      <div className="p-4 space-y-6">
        {/* Quick Info */}
        <Card>
          <CardContent className="p-4 space-y-3">
            <div className="flex items-center gap-2 text-stone-600">
              <Users className="w-5 h-5 text-orange-500" />
              <span className="font-medium">{group.memberCount}</span>
              <span className="text-stone-400">/ {group.maxMembers}명</span>
            </div>
            <p className="text-stone-700 leading-relaxed">{group.description}</p>
            <div className="flex flex-wrap gap-2 pt-2">
              {group.tags.map(tag => (
                <Badge key={tag} variant="secondary" className="bg-orange-50 text-orange-600">
                  #{tag}
                </Badge>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Next Event */}
        {group.nextEvent && (
          <Card className="border-orange-200 bg-orange-50">
            <CardContent className="p-4">
              <div className="flex items-center gap-2 text-orange-600 mb-2">
                <Calendar className="w-5 h-5" />
                <span className="font-medium">다음 일정</span>
              </div>
              <h3 className="font-bold text-stone-900">{group.nextEvent.title}</h3>
              <div className="flex items-center gap-4 mt-2 text-sm text-stone-600">
                <span>{group.nextEvent.date}</span>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Recent Posts (공개 모임만) */}
        {group.isPostPublic && group.recentPosts && group.recentPosts.length > 0 ? (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="font-bold text-lg text-stone-900">최근 게시글</h2>
              <Badge variant="secondary" className="bg-green-100 text-green-700">
                <Eye className="w-3 h-3 mr-1" />
                미리보기
              </Badge>
            </div>
            <div className="space-y-4">
              {group.recentPosts.map(post => (
                <Card key={post.id} className="overflow-hidden">
                  <div className="flex">
                    <div className="w-24 h-24 bg-stone-200 flex-shrink-0">
                      <img src={post.image} alt="" className="w-full h-full object-cover" />
                    </div>
                    <CardContent className="flex-1 p-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-stone-900">{post.user}</span>
                        <span className="text-xs text-stone-400">{post.date}</span>
                      </div>
                      <p className="text-sm text-stone-600 mt-1 line-clamp-2">{post.content}</p>
                      <div className="flex items-center gap-3 mt-2 text-xs text-stone-500">
                        <span className="flex items-center gap-1">
                          <Heart className="w-3 h-3" /> {post.likes}
                        </span>
                        <span className="flex items-center gap-1">
                          <MessageCircle className="w-3 h-3" /> {post.comments}
                        </span>
                      </div>
                    </CardContent>
                  </div>
                </Card>
              ))}
            </div>
            <p className="text-xs text-center text-stone-500">
              가입하시면 모든 게시글을 확인하실 수 있습니다.
            </p>
          </div>
        ) : !group.isPostPublic ? (
          <Card className="border-stone-200 bg-stone-100">
            <CardContent className="p-6 text-center">
              <Lock className="w-10 h-10 text-stone-400 mx-auto mb-3" />
              <h3 className="font-medium text-stone-700">게시글 비공개 모임</h3>
              <p className="text-sm text-stone-500 mt-1">
                가입 후 게시글을 확인하실 수 있습니다.
              </p>
            </CardContent>
          </Card>
        ) : null}

        {/* Login Prompt */}
        {!isLoggedIn && (
          <Card className="border-blue-200 bg-blue-50">
            <CardContent className="p-4">
              <h3 className="font-medium text-blue-800 mb-2">로그인이 필요합니다</h3>
              <p className="text-sm text-blue-700 mb-4">
                모임에 가입하려면 먼저 로그인해주세요.
              </p>
              <div className="flex gap-2">
                <Link to="/login" className="flex-1">
                  <Button className="w-full bg-blue-600 hover:bg-blue-700">
                    로그인
                  </Button>
                </Link>
                <Link to="/signup" className="flex-1">
                  <Button variant="outline" className="w-full border-blue-300 text-blue-600">
                    회원가입
                  </Button>
                </Link>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Fixed Bottom CTA */}
      {isLoggedIn ? (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4">
          <div className="max-w-[500px] mx-auto">
            <Button 
              onClick={() => setShowJoinDialog(true)}
              className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white rounded-xl text-lg"
            >
              <UserPlus className="w-5 h-5 mr-2" />
              가입 신청하기
            </Button>
          </div>
        </div>
      ) : (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4">
          <div className="max-w-[500px] mx-auto">
            <Link to="/login">
              <Button className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white rounded-xl text-lg">
                <UserPlus className="w-5 h-5 mr-2" />
                로그인하고 가입 신청하기
              </Button>
            </Link>
          </div>
        </div>
      )}

      {/* 가입 다이얼로그 */}
      <Dialog open={showJoinDialog} onOpenChange={setShowJoinDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>모임 가입 신청</DialogTitle>
            <DialogDescription>
              이 모임에 가입하시겠습니까? 가입 후 관리자 승인을 기다려주세요.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4 space-y-4">
            <div className="space-y-2">
              <Label htmlFor="nickname">모임 내 닉네임</Label>
              <Input
                id="nickname"
                placeholder="닉네임을 입력하세요 (최대 10자)"
                value={joinNickname}
                onChange={(e) => setJoinNickname(e.target.value)}
                maxLength={10}
              />
              <p className="text-xs text-stone-500">
                모임 내에서 사용할 닉네임을 입력해주세요.
              </p>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowJoinDialog(false)}>
              취소
            </Button>
            <Button 
              onClick={handleJoinRequest}
              disabled={!joinNickname.trim() || isJoining}
              className="bg-orange-500 hover:bg-orange-600"
            >
              {isJoining ? '가입 중...' : '가입 신청'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Report Dialog */}
      <ReportDialog
        open={showReportDialog}
        onOpenChange={setShowReportDialog}
        type="group"
        targetId={group.ownerId || parseInt(group.id)}
        targetName={group.name}
        clubId={parseInt(group.id)}
      />
    </div>
  );
}



