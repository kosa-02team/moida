import { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { ArrowLeft, MapPin, Users, Calendar, Share2, Heart, MessageCircle, Lock, Eye, Flag } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../ui/avatar';
import { Card, CardContent } from '../ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import { Textarea } from '../ui/textarea';
import { Label } from '../ui/label';
import { ReportDialog } from '../report/ReportDialog';
import { getClub } from '@/api/club-full';
import { getRecentPosts, PostCardResponse } from '@/api/post';
import { get } from '@/api/client';

export function GroupPreviewView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const [isLiked, setIsLiked] = useState(false);
  const [showJoinDialog, setShowJoinDialog] = useState(false);
  const [showReportDialog, setShowReportDialog] = useState(false);
  const [joinMessage, setJoinMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [group, setGroup] = useState<{
    id: string | undefined;
    name: string;
    image: string;
    description: string;
    tags: string[];
    memberCount: number;
    maxMembers: number;
    type: 'club' | 'meetup' | 'study';
    location: string;
    createdAt: string;
    ownerId?: number; // 모임 신고를 위한 owner ID
    leader: { name: string; avatar: string };
    recentMembers: Array<{ id: string; name: string; avatar: string }>;
    upcomingEvents: Array<{ id: string; title: string; date: string; location: string }>;
    privacySettings: { showPostsToNonMembers: boolean; showMembersToNonMembers: boolean };
    publicPosts: Array<{ id: string; title: string; content: string; image: string; author: string; date: string; likes: number; comments: number }>;
    gallery: string[];
  } | null>(null);
  const [loading, setLoading] = useState(true);

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
        let publicPosts: Array<{ id: string; title: string; content: string; image: string; author: string; date: string; likes: number; comments: number }> = [];
        if (clubData.visibility === 'PUBLIC') {
          try {
            const postsData = await getRecentPosts(clubId, 0, 3);
            publicPosts = postsData.slice(0, 3).map(post => ({
              id: post.postId.toString(),
              title: post.title || '',
              content: post.content || '',
              image: post.imagesUrl && post.imagesUrl.length > 0 ? post.imagesUrl[0] : 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=400&h=300&fit=crop',
              author: post.writerName || '익명',
              date: post.createdAt ? new Date(post.createdAt).toLocaleDateString('ko-KR') : '',
              likes: post.postLikes || 0,
              comments: post.commentCount || 0,
            }));
          } catch (error) {
            console.error('게시글 조회 실패:', error);
          }
        }

        const isPrivateGroup = clubData.visibility === 'PRIVATE';

        setGroup({
          id: groupId,
          name: clubData.clubName,
          image: '',
          description: '', // 백엔드에 description 필드가 없음
          tags: [], // 백엔드에 tags 필드가 없음
          memberCount: clubData.currentMembers || 0,
          maxMembers: clubData.maxMembers || 100,
          type: clubData.type === 'OPERATION_FEE' ? 'meetup' : clubData.type === 'FAIR_SETTLEMENT' ? 'club' : 'study',
          location: '', // 백엔드에 location 필드가 없음
          createdAt: clubData.createdAt ? new Date(clubData.createdAt).toLocaleDateString('ko-KR') : '',
          leader: {
            name: '', // 백엔드에 ownerName이 없을 수 있음
            avatar: '',
          },
          recentMembers: [], // 백엔드에 멤버 목록 API가 필요
          upcomingEvents: [], // 백엔드에 일정 목록 API가 필요
          privacySettings: {
            showPostsToNonMembers: !isPrivateGroup,
            showMembersToNonMembers: !isPrivateGroup,
          },
          publicPosts,
          gallery: [], // 백엔드에 갤러리 API가 필요
        });
      } catch (error) {
        console.error('모임 정보 불러오기 실패:', error);
        toast.error('모임 정보를 불러오는데 실패했습니다.');
        navigate('/explore');
      } finally {
        setLoading(false);
      }
    }

    fetchGroup();
  }, [groupId, navigate]);

  if (loading || !group) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  const handleShare = () => {
    navigator.clipboard.writeText(window.location.href);
    toast.success('링크가 복사되었습니다');
  };

  const handleJoinRequest = () => {
    setIsSubmitting(true);
    setTimeout(() => {
      setIsSubmitting(false);
      setShowJoinDialog(false);
      toast.success('가입 신청이 완료되었습니다. 관리자 승인을 기다려주세요.');
    }, 1000);
  };

  const handlePostClick = (postId: string) => {
    if (group.privacySettings.showPostsToNonMembers) {
      // 게시글 공개: 상세 페이지로 이동
      navigate(`/group/${groupId}/stories/${postId}?preview=true`);
    } else {
      // 게시글 비공개: 권한 없음 안내
      toast.error('게시글을 보려면 모임에 가입해주세요.');
    }
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-24">
      {/* Header Image */}
      <div className="relative h-56 bg-stone-300">
        <img
          src={group.image}
          alt={group.name}
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
        
        {/* Top Navigation */}
        <div className="absolute top-0 left-0 right-0 p-4 flex items-center justify-between">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate(-1)}
            className="bg-black/20 hover:bg-black/40 text-white rounded-full"
          >
            <ArrowLeft className="w-6 h-6" />
          </Button>
          <div className="flex gap-2">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setIsLiked(!isLiked)}
              className="bg-black/20 hover:bg-black/40 text-white rounded-full"
            >
              <Heart className={`w-6 h-6 ${isLiked ? 'fill-red-500 text-red-500' : ''}`} />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={handleShare}
              className="bg-black/20 hover:bg-black/40 text-white rounded-full"
            >
              <Share2 className="w-6 h-6" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setShowReportDialog(true)}
              className="bg-black/20 hover:bg-black/40 text-white rounded-full"
            >
              <Flag className="w-6 h-6" />
            </Button>
          </div>
        </div>

        {/* Group Name on Image */}
        <div className="absolute bottom-4 left-4 right-4">
          <Badge className="bg-white/90 text-stone-800 mb-2">
            {group.type === 'club' ? '동아리' : group.type === 'study' ? '스터디' : '정모'}
          </Badge>
          <h1 className="text-2xl font-bold text-white">{group.name}</h1>
        </div>
      </div>

      {/* Content */}
      <div className="p-5 space-y-6">
        {/* Info */}
        <div className="flex items-center gap-4 text-sm text-stone-600">
          <span className="flex items-center gap-1">
            <MapPin className="w-4 h-4" />
            {group.location}
          </span>
          <span className="flex items-center gap-1">
            <Users className="w-4 h-4" />
            {group.memberCount}/{group.maxMembers}명
          </span>
          <span className="flex items-center gap-1">
            <Calendar className="w-4 h-4" />
            {group.createdAt} 개설
          </span>
        </div>

        {/* Tags */}
        <div className="flex flex-wrap gap-2">
          {group.tags.map(tag => (
            <Badge key={tag} variant="secondary" className="bg-orange-100 text-orange-700">
              #{tag}
            </Badge>
          ))}
        </div>

        {/* Description */}
        <div className="bg-white rounded-xl p-4 border border-stone-100">
          <h3 className="font-bold text-stone-900 mb-2">소개</h3>
          <p className="text-sm text-stone-600 leading-relaxed">{group.description}</p>
        </div>

        {/* Leader */}
        <div className="bg-white rounded-xl p-4 border border-stone-100">
          <h3 className="font-bold text-stone-900 mb-3">모임장</h3>
          <div className="flex items-center gap-3">
            <Avatar className="w-12 h-12">
              <AvatarImage src={group.leader.avatar} />
              <AvatarFallback>{group.leader.name[0]}</AvatarFallback>
            </Avatar>
            <div>
              <p className="font-medium text-stone-900">{group.leader.name}</p>
              <p className="text-xs text-stone-500">모임장</p>
            </div>
          </div>
        </div>

        {/* Members Preview */}
        {group.privacySettings.showMembersToNonMembers ? (
          <div className="bg-white rounded-xl p-4 border border-stone-100">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold text-stone-900">멤버</h3>
              <span className="text-sm text-stone-500">{group.memberCount}명</span>
            </div>
            <div className="flex -space-x-2">
              {group.recentMembers.map(member => (
                <Avatar key={member.id} className="w-10 h-10 border-2 border-white">
                  <AvatarImage src={member.avatar} />
                  <AvatarFallback>{member.name[0]}</AvatarFallback>
                </Avatar>
              ))}
              {group.memberCount > 3 && (
                <div className="w-10 h-10 rounded-full bg-stone-100 border-2 border-white flex items-center justify-center text-xs text-stone-600">
                  +{group.memberCount - 3}
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="bg-stone-100 rounded-xl p-4 border border-stone-200">
            <div className="flex items-center gap-3 text-stone-500">
              <Lock className="w-5 h-5" />
              <div>
                <p className="font-medium">멤버 목록 비공개</p>
                <p className="text-xs">가입 후 확인할 수 있습니다</p>
              </div>
            </div>
          </div>
        )}

        {/* Upcoming Events */}
        {group.upcomingEvents.length > 0 && (
          <div className="bg-white rounded-xl p-4 border border-stone-100">
            <h3 className="font-bold text-stone-900 mb-3">다가오는 일정</h3>
            <div className="space-y-3">
              {group.upcomingEvents.map(event => (
                <div key={event.id} className="flex items-start gap-3">
                  <div className="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center text-orange-600">
                    <Calendar className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="font-medium text-stone-900">{event.title}</p>
                    <p className="text-xs text-stone-500">{event.date}</p>
                    <p className="text-xs text-stone-400">{event.location}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Public Posts - 게시글 공개/비공개 */}
        <div className="bg-white rounded-xl p-4 border border-stone-100">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-bold text-stone-900">게시글</h3>
            {group.privacySettings.showPostsToNonMembers ? (
              <Badge variant="secondary" className="bg-green-100 text-green-700">
                <Eye className="w-3 h-3 mr-1" />
                공개
              </Badge>
            ) : (
              <Badge variant="secondary" className="bg-stone-100 text-stone-600">
                <Lock className="w-3 h-3 mr-1" />
                회원 전용
              </Badge>
            )}
          </div>

          {group.privacySettings.showPostsToNonMembers ? (
            // 게시글 공개: 게시글 미리보기
            <div className="space-y-3">
              {group.publicPosts.map(post => (
                <Card 
                  key={post.id} 
                  className="overflow-hidden cursor-pointer hover:shadow-md transition-shadow"
                  onClick={() => handlePostClick(post.id)}
                >
                  <div className="flex">
                    <div className="w-24 h-24 flex-shrink-0">
                      <img src={post.image} alt="" className="w-full h-full object-cover" />
                    </div>
                    <CardContent className="p-3 flex-1">
                      <h4 className="font-medium text-stone-900 text-sm line-clamp-1">{post.title}</h4>
                      <p className="text-xs text-stone-500 line-clamp-2 mt-1">{post.content}</p>
                      <div className="flex items-center gap-3 mt-2 text-xs text-stone-400">
                        <span>{post.author}</span>
                        <span>좋아요 {post.likes}</span>
                        <span>댓글 {post.comments}</span>
                      </div>
                    </CardContent>
                  </div>
                </Card>
              ))}
              <p className="text-xs text-center text-stone-400 pt-2">
                가입하면 더 많은 게시글을 볼 수 있습니다
              </p>
            </div>
          ) : (
            // 게시글 비공개: 권한 없음 안내
            <div className="py-8 text-center">
              <div className="w-16 h-16 bg-stone-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Lock className="w-8 h-8 text-stone-400" />
              </div>
              <h4 className="font-medium text-stone-700 mb-1">게시글 보기 권한이 없습니다</h4>
              <p className="text-sm text-stone-500 mb-4">
                게시글은 모임 회원만 볼 수 있습니다.
              </p>
              <Button 
                onClick={() => setShowJoinDialog(true)}
                className="bg-orange-500 hover:bg-orange-600"
              >
                가입 신청하기
              </Button>
            </div>
          )}
        </div>

        {/* 회비 현황 - 회원 전용 */}
        <div className="bg-white rounded-xl p-4 border border-stone-100">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-bold text-stone-900">회비 현황</h3>
            <Badge variant="secondary" className="bg-stone-100 text-stone-600">
              <Lock className="w-3 h-3 mr-1" />
              회원 전용
            </Badge>
          </div>
          <div className="py-6 text-center">
            <div className="w-12 h-12 bg-stone-100 rounded-full flex items-center justify-center mx-auto mb-3">
              <Lock className="w-6 h-6 text-stone-400" />
            </div>
            <p className="text-sm text-stone-500">
              회비 현황은 모임 회원만<br/>확인할 수 있습니다
            </p>
          </div>
        </div>

        {/* Gallery - 게시글 공개 시에만 표시 */}
        {group.privacySettings.showPostsToNonMembers && group.gallery.length > 0 && (
          <div className="bg-white rounded-xl p-4 border border-stone-100">
            <h3 className="font-bold text-stone-900 mb-3">갤러리</h3>
            <div className="grid grid-cols-3 gap-2">
              {group.gallery.map((img, i) => (
                <div key={i} className="aspect-square rounded-lg overflow-hidden bg-stone-200">
                  <img src={img} alt="" className="w-full h-full object-cover" />
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Bottom CTA */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4 safe-area-pb">
        <div className="max-w-md mx-auto flex gap-3">
          <Button
            variant="outline"
            className="flex-1 h-12 rounded-xl"
            onClick={() => toast.info('로그인 후 문의할 수 있습니다')}
          >
            <MessageCircle className="w-5 h-5 mr-2" />
            문의하기
          </Button>
          <Button
            className="flex-1 h-12 bg-orange-500 hover:bg-orange-600 text-white rounded-xl"
            onClick={() => setShowJoinDialog(true)}
          >
            가입 신청
          </Button>
        </div>
      </div>

      {/* Join Dialog */}
      <Dialog open={showJoinDialog} onOpenChange={setShowJoinDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>가입 신청</DialogTitle>
            <DialogDescription>
              "{group.name}" 모임에 가입을 신청합니다.
              관리자 승인 후 모임에 참여할 수 있습니다.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>가입 인사 (선택)</Label>
              <Textarea
                placeholder="모임장에게 전할 인사말을 작성해주세요"
                className="min-h-24 resize-none"
                value={joinMessage}
                onChange={(e) => setJoinMessage(e.target.value)}
              />
            </div>
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
              <p className="text-xs text-blue-800">
                💡 로그인이 필요합니다. 가입 신청을 위해 먼저 로그인해주세요.
              </p>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowJoinDialog(false)}>
              취소
            </Button>
            <Link to="/login">
              <Button className="bg-orange-500 hover:bg-orange-600">
                로그인하러 가기
              </Button>
            </Link>
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
