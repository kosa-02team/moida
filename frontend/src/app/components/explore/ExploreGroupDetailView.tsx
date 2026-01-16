import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { 
  ArrowLeft, MapPin, Users, Calendar, Eye, Lock, Heart, MessageCircle, 
  ChevronRight, Flag, Share2, UserPlus
} from 'lucide-react';
import { Button } from '../ui/button';
import { Card, CardContent } from '../ui/card';
import { Badge } from '../ui/badge';
import { toast } from 'sonner';
import { ReportDialog } from '../report/ReportDialog';

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

// Mock data
const MOCK_GROUPS: Record<string, PublicGroup> = {
  '1': {
    id: '1',
    name: '주말 등산 클럽',
    image: 'https://images.unsplash.com/photo-1551632811-561732d1e306?w=800&auto=format&fit=crop',
    description: '매주 토요일 서울 근교 산행합니다. 초보자도 환영합니다! 함께 건강한 주말을 보내요.',
    tags: ['등산', '운동', '친목', '주말'],
    memberCount: 15,
    maxMembers: 50,
    type: 'club',
    location: '서울',
    isPostPublic: true,
    nextEvent: { 
      title: '관악산 정기 산행', 
      date: '2025-04-12',
      location: '사당역 4번 출구'
    },
    recentPosts: [
      {
        id: '1',
        user: '김산악',
        image: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=400&auto=format&fit=crop',
        content: '날씨가 너무 좋았던 하루! 다들 고생하셨습니다 ㅎㅎ',
        likes: 12,
        comments: 4,
        date: '2시간 전',
      },
      {
        id: '2',
        user: '이영희',
        image: 'https://images.unsplash.com/photo-1502224562085-639556652f33?w=400&auto=format&fit=crop',
        content: '정말 즐거웠어요! 다음 모임도 기대됩니다 🎉',
        likes: 8,
        comments: 2,
        date: '5시간 전',
      },
    ],
  },
  '2': {
    id: '2',
    name: '프라이빗 독서 모임',
    image: 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=800&auto=format&fit=crop',
    description: '비공개로 진행되는 프리미엄 독서 모임입니다. 매달 1권의 책을 선정하여 깊이 있는 토론을 진행합니다.',
    tags: ['독서', '토론', '인문학'],
    memberCount: 8,
    maxMembers: 15,
    type: 'study',
    location: '강남',
    isPostPublic: false,
    nextEvent: { 
      title: '4월 독서 토론', 
      date: '2025-04-18',
      location: '강남 스터디카페'
    },
  },
};

export function ExploreGroupDetailView() {
  const { groupId } = useParams();
  const navigate = useNavigate();
  const [showReportDialog, setShowReportDialog] = useState(false);

  const group = MOCK_GROUPS[groupId || '1'] || MOCK_GROUPS['1'];

  const handleJoinRequest = () => {
    toast.success('가입 신청이 완료되었습니다. 승인을 기다려주세요.');
  };

  const handleShare = () => {
    navigator.clipboard.writeText(window.location.href);
    toast.success('링크가 복사되었습니다');
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-32">
      {/* Header Image */}
      <div className="relative h-56">
        <img
          src={group.image}
          alt={group.name}
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-b from-black/30 to-black/60" />
        
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
            {group.location && (
              <div className="flex items-center gap-2 text-stone-600">
                <MapPin className="w-5 h-5 text-orange-500" />
                <span>{group.location}</span>
              </div>
            )}
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
                <span className="flex items-center gap-1">
                  <MapPin className="w-4 h-4" />
                  {group.nextEvent.location}
                </span>
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
      </div>

      {/* Fixed Bottom CTA */}
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



