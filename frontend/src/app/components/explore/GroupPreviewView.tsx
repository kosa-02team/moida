import { useState } from 'react';
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

export function GroupPreviewView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const [isLiked, setIsLiked] = useState(false);
  const [showJoinDialog, setShowJoinDialog] = useState(false);
  const [showReportDialog, setShowReportDialog] = useState(false);
  const [joinMessage, setJoinMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 비공개 모임 ID 목록 (실제로는 API에서 받아옴)
  const privateGroupIds = ['2', '5', '8', '10'];
  const isPrivateGroup = privateGroupIds.includes(groupId || '');

  // Mock data - 실제로는 API에서 가져옴
  const groupData: Record<string, any> = {
    '1': { name: '주말 등산 클럽', description: '매주 토요일 서울 근교 산행합니다. 초보자 환영!', tags: ['등산', '운동', '친목'], image: 'https://images.unsplash.com/photo-1551632811-561732d1e306?w=800' },
    '2': { name: '프라이빗 독서 모임', description: '비공개로 진행되는 프리미엄 독서 모임입니다.', tags: ['독서', '토론', '인문학'], image: 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=800' },
    '3': { name: '개발자 네트워킹', description: 'IT 업계 종사자들의 네트워킹 모임입니다.', tags: ['개발', 'IT', '네트워킹'], image: 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800' },
    '4': { name: '요가 & 명상 클럽', description: '바쁜 일상 속 마음의 평화를 찾아요.', tags: ['요가', '명상', '건강'], image: 'https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800' },
    '5': { name: '비밀 투자 스터디', description: '소수 정예로 진행하는 투자 전략 스터디입니다.', tags: ['주식', '투자', '재테크'], image: 'https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=800' },
    '6': { name: '주말 러닝 크루', description: '함께 뛰면 더 즐거워요! 러닝 크루', tags: ['러닝', '운동', '건강'], image: 'https://images.unsplash.com/photo-1571008887538-b36bb32f4571?w=800' },
    '7': { name: '사진 동호회', description: '출사와 사진 리뷰를 함께하는 모임', tags: ['사진', '출사', '취미'], image: 'https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=800' },
    '8': { name: '프리미엄 와인 모임', description: '와인 애호가들의 비공개 테이스팅 모임', tags: ['와인', '테이스팅', '프리미엄'], image: 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=800' },
    '9': { name: '영어 회화 스터디', description: '영어로 자유롭게 대화하는 스터디입니다.', tags: ['영어', '회화', '스터디'], image: 'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=800' },
    '10': { name: 'VIP 골프 모임', description: '골프를 사랑하는 분들의 프라이빗 모임', tags: ['골프', '운동', 'VIP'], image: 'https://images.unsplash.com/photo-1535131749006-b7f58c99034b?w=800' },
  };

  const currentGroupData = groupData[groupId || '1'] || groupData['1'];

  const group = {
    id: groupId,
    name: currentGroupData.name,
    image: currentGroupData.image + '&auto=format&fit=crop&q=60',
    description: currentGroupData.description + ' 산을 좋아하는 분들과 함께 건강하고 즐거운 주말을 보내요.',
    tags: currentGroupData.tags,
    memberCount: 15,
    maxMembers: 50,
    type: 'club',
    location: '서울',
    createdAt: '2024.01.15',
    leader: {
      name: '홍길동',
      avatar: '',
    },
    recentMembers: [
      { id: '1', name: '김철수', avatar: '' },
      { id: '2', name: '이영희', avatar: '' },
      { id: '3', name: '박민수', avatar: '' },
    ],
    upcomingEvents: [
      { id: '1', title: '4월 정기 모임', date: '4월 12일 (토) 10:00', location: '강남역' },
      { id: '2', title: '5월 특별 모임', date: '5월 10일 (토) 09:00', location: '홍대입구역' },
    ],
    // 공개 설정 - 비공개 모임인 경우 게시글 비공개
    privacySettings: {
      showPostsToNonMembers: !isPrivateGroup, // 비공개 모임이면 false
      showMembersToNonMembers: !isPrivateGroup,
    },
    // 공개된 게시글 (showPostsToNonMembers가 true일 때만 표시)
    publicPosts: [
      {
        id: '1',
        title: '4월 정기 산행 후기',
        content: '날씨가 너무 좋았던 하루! 다음에도 참여하세요~',
        image: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=400&h=300&fit=crop',
        author: '홍길동',
        date: '2024.04.12',
        likes: 12,
        comments: 5,
      },
      {
        id: '2',
        title: '3월 신입 환영회',
        content: '새로운 멤버분들 환영합니다!',
        image: 'https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=400&h=300&fit=crop',
        author: '김철수',
        date: '2024.03.20',
        likes: 8,
        comments: 3,
      },
    ],
    gallery: [
      'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=300&h=300&fit=crop',
      'https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?w=300&h=300&fit=crop',
      'https://images.unsplash.com/photo-1502224562085-639556652f33?w=300&h=300&fit=crop',
    ],
  };

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
        targetName={group.name}
      />
    </div>
  );
}
