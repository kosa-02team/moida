import React, { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getRecentPosts, getRecentAlbums, deletePost as deletePostAPI, likePost as likePostAPI, unlikePost as unlikePostAPI, type PostCardResponse, type AlbumCardResponse } from '../../../../api/post';
import { Folder, Heart, MessageCircle, Plus, Camera, ArrowUpDown, MoreVertical, Trash2, Flag, AlertTriangle } from 'lucide-react';
import { Card } from '../../ui/card';
import { Button } from '../../ui/button';
import { toast } from 'sonner';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../ui/dropdown-menu';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../../ui/alert-dialog';
import { useUserPermissions } from '../../../data/userRoles';
import { ReportDialog } from '../../report/ReportDialog';

type SortType = 'latest' | 'oldest' | 'popular';

interface Post {
  id: string;
  user: string;
  userImg: string;
  image: string;
  content: string;
  likes: number;
  comments: number;
  date: string;
  dateDisplay: string;
  isMyPost?: boolean;
  isLiked?: boolean;
}

export function StoriesView() {
  const { groupId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [sortBy, setSortBy] = useState<SortType>('latest');
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [showReportDialog, setShowReportDialog] = useState(false);
  const [selectedPost, setSelectedPost] = useState<Post | null>(null);

  const [albums, setAlbums] = useState<AlbumCardResponse[]>([]);
  
  useEffect(() => {
    async function fetchAlbums() {
      if (!groupId) return;
      try {
        const albumsData = await getRecentAlbums(Number(groupId), 10);
        setAlbums(albumsData);
      } catch (error) {
        console.error('앨범 목록 불러오기 실패:', error);
      }
    }
    fetchAlbums();
  }, [groupId]);

  const [allPosts, setAllPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(false);

  // API에서 게시글 목록 가져오기
  useEffect(() => {
    async function fetchPosts() {
      if (!groupId) return;
      try {
        setLoading(true);
        const posts = await getRecentPosts(Number(groupId));
        // PostCardResponse를 Post 형식으로 변환
        const convertedPosts: Post[] = posts.map((p) => ({
          id: String(p.postId),
          user: p.writerName,
          userImg: '',
          image: p.imagesUrl && p.imagesUrl.length > 0 ? p.imagesUrl[0] : '',
          content: p.content,
          likes: p.postLikes,
          comments: p.commentCount,
          date: p.createdAt,
          dateDisplay: formatDateDisplay(p.createdAt),
          isMyPost: false,
          isLiked: false,
          writerId: p.writerId, // 신고를 위한 작성자 ID
        }));
        setAllPosts(convertedPosts);
      } catch (error) {
        console.error('게시글 불러오기 실패:', error);
        toast.error('게시글을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchPosts();
  }, [groupId]);

  // 날짜 표시 형식 변환
  function formatDateDisplay(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    if (hours < 1) return '방금 전';
    if (hours < 24) return `${hours}시간 전`;
    if (days === 1) return '어제';
    if (days < 7) return `${days}일 전`;
    return date.toLocaleDateString('ko-KR');
  }

  // 정렬된 게시글
  const sortedPosts = [...allPosts].sort((a, b) => {
    switch (sortBy) {
      case 'latest':
        return new Date(b.date).getTime() - new Date(a.date).getTime();
      case 'oldest':
        return new Date(a.date).getTime() - new Date(b.date).getTime();
      case 'popular':
        return (b.likes + b.comments) - (a.likes + a.comments);
      default:
        return 0;
    }
  });

  const sortLabels: Record<SortType, string> = {
    latest: '최신순',
    oldest: '오래된순',
    popular: '인기순',
  };


  const handleDeletePost = async () => {
    if (!selectedPost || !groupId) return;
    try {
      await deletePostAPI(Number(groupId), Number(selectedPost.id));
      setAllPosts(posts => posts.filter(p => p.id !== selectedPost.id));
      toast.success('게시글이 삭제되었습니다');
      setShowDeleteDialog(false);
      setSelectedPost(null);
    } catch (error) {
      console.error('게시글 삭제 실패:', error);
      toast.error('게시글 삭제에 실패했습니다.');
    }
  };

  const handleLikePost = async (post: Post) => {
    if (!groupId) return;
    try {
      if (post.isLiked) {
        await unlikePostAPI(Number(groupId), Number(post.id));
        setAllPosts(posts => posts.map(p => 
          p.id === post.id 
            ? { ...p, isLiked: false, likes: Math.max(0, p.likes - 1) }
            : p
        ));
      } else {
        await likePostAPI(Number(groupId), Number(post.id));
        setAllPosts(posts => posts.map(p => 
          p.id === post.id 
            ? { ...p, isLiked: true, likes: p.likes + 1 }
            : p
        ));
      }
    } catch (error) {
      console.error('좋아요 처리 실패:', error);
      toast.error('좋아요 처리에 실패했습니다.');
    }
  };

  const handleReportPost = () => {
    if (!reportReason) {
      toast.error('신고 사유를 선택해주세요');
      return;
    }
    toast.success('신고가 접수되었습니다');
    setShowReportDialog(false);
    setSelectedPost(null);
    setReportReason('');
    setReportDetail('');
  };

  const canDeletePost = (post: Post) => {
    return post.isMyPost || permissions.canDeletePosts;
  };

  return (
    <div className="space-y-8 pb-20">
      {/* Create Album Button */}
      <div className="flex justify-between items-center px-1">
        <h3 className="font-bold text-lg text-stone-800">앨범</h3>
        <Link to="create">
          <Button className="bg-orange-500 hover:bg-orange-600 rounded-full">
            <Camera className="w-4 h-4 mr-2" />
            앨범 작성
          </Button>
        </Link>
      </div>

      {/* Albums / Folders */}
      <section>
        <h3 className="font-bold text-lg text-stone-800 px-1 mb-3">앨범</h3>
        {albums.length > 0 ? (
          <div className="grid grid-cols-2 gap-4">
            {albums.map(album => (
              <Link to={`../albums/${album.postId || album.scheduleId}`} key={album.postId || album.scheduleId}>
                <Card className="border-none shadow-none group cursor-pointer">
                  <div className="relative aspect-square rounded-2xl overflow-hidden mb-2">
                    <img src={album.coverImageUrl || 'https://via.placeholder.com/400'} alt="" className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                    <div className="absolute inset-0 bg-black/10 group-hover:bg-black/0 transition-colors" />
                    <div className="absolute bottom-2 right-2 bg-black/60 text-white text-xs px-2 py-1 rounded-full flex items-center gap-1">
                      <Folder className="w-3 h-3" /> {album.imageCount}
                    </div>
                  </div>
                  <h4 className="font-medium text-stone-900 truncate px-1">{album.scheduleName || '앨범'}</h4>
                  <p className="text-xs text-stone-500 px-1">{new Date(album.lastCreatedAt).toLocaleDateString('ko-KR')}</p>
                </Card>
              </Link>
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-stone-500 text-sm">앨범이 없습니다</div>
        )}
      </section>

      {/* Feed */}
      <section>
        <div className="flex justify-between items-center px-1 mb-3">
          <h3 className="font-bold text-lg text-stone-800">게시글</h3>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm" className="h-8 gap-1">
                <ArrowUpDown className="w-4 h-4" />
                {sortLabels[sortBy]}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => setSortBy('latest')}>
                최신순
                {sortBy === 'latest' && ' ✓'}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => setSortBy('oldest')}>
                오래된순
                {sortBy === 'oldest' && ' ✓'}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => setSortBy('popular')}>
                인기순(좋아요+댓글)
                {sortBy === 'popular' && ' ✓'}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {sortedPosts.length > 0 ? (
          <div className="space-y-6">
            {sortedPosts.map(post => (
            <div key={post.id} className="bg-white rounded-2xl border border-stone-100 shadow-sm overflow-hidden hover:shadow-md transition-shadow">
              <div className="p-3 flex items-center justify-between">
                <Link to={post.id} className="flex items-center gap-3">
                  <img 
                    src={post.userImg || `https://api.dicebear.com/7.x/initials/svg?seed=${post.user}`} 
                    alt="" 
                    className="w-8 h-8 rounded-full bg-stone-200" 
                  />
                  <div>
                    <p className="font-bold text-sm text-stone-900">{post.user}</p>
                    <p className="text-xs text-stone-400">{post.dateDisplay}</p>
                  </div>
                </Link>
                
                {/* 더보기 메뉴 */}
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="h-8 w-8 text-stone-400">
                      <MoreVertical className="w-4 h-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    {canDeletePost(post) && (
                      <>
                        <DropdownMenuItem 
                          className="text-red-600"
                          onClick={() => {
                            setSelectedPost(post);
                            setShowDeleteDialog(true);
                          }}
                        >
                          <Trash2 className="w-4 h-4 mr-2" />
                          삭제하기
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                      </>
                    )}
                    <DropdownMenuItem 
                      className="text-orange-600"
                      onClick={() => {
                        setSelectedPost(post);
                        setShowReportDialog(true);
                      }}
                    >
                      <Flag className="w-4 h-4 mr-2" />
                      신고하기
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
              
              <Link to={post.id}>
                {post.image ? (
                  <div className="aspect-[4/3] bg-stone-100">
                    <img src={post.image} alt="" className="w-full h-full object-cover" onError={(e) => {
                      const target = e.target as HTMLImageElement;
                      target.style.display = 'none';
                      if (target.parentElement) {
                        target.parentElement.style.display = 'none';
                      }
                    }} />
                  </div>
                ) : null}
                <div className="p-4 space-y-3">
                  <div className="flex gap-4">
                    <span className="flex items-center gap-1 text-stone-600">
                      <Heart className={`w-5 h-5 ${post.isLiked ? 'fill-red-500 text-red-500' : 'text-stone-600'} cursor-pointer hover:scale-110 transition-transform`} onClick={(e) => { e.preventDefault(); handleLikePost(post); }} />
                      <span className="text-sm font-medium">{post.likes}</span>
                    </span>
                    <span className="flex items-center gap-1 text-stone-600">
                      <MessageCircle className="w-5 h-5" />
                      <span className="text-sm font-medium">{post.comments}</span>
                    </span>
                  </div>
                  <p className="text-stone-800 text-sm leading-relaxed line-clamp-2">
                    {post.content}
                  </p>
                </div>
              </Link>
            </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-stone-500">
            <p className="text-sm">아직 게시글이 없습니다</p>
            <Link to="create">
              <Button className="mt-4 bg-orange-500 hover:bg-orange-600">
                첫 게시글 작성하기
              </Button>
            </Link>
          </div>
        )}
      </section>

      {/* FAB */}
      <div className="fixed bottom-20 right-4 md:right-[calc(50%-220px+1rem)] z-40">
        <Link to="create">
          <Button size="lg" className="rounded-full w-14 h-14 shadow-lg bg-orange-500 hover:bg-orange-600 text-white p-0">
            <Plus className="w-7 h-7" />
          </Button>
        </Link>
      </div>

      {/* 삭제 확인 다이얼로그 */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 bg-red-100 rounded-full">
                <AlertTriangle className="w-6 h-6 text-red-600" />
              </div>
              <AlertDialogTitle className="text-xl">게시글 삭제</AlertDialogTitle>
            </div>
            <AlertDialogDescription>
              이 게시글을 삭제하시겠습니까? 삭제된 게시글은 복구할 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeletePost}
              className="bg-red-500 hover:bg-red-600"
            >
              삭제하기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* 신고 다이얼로그 */}
      {selectedPost && (
        <ReportDialog
          open={showReportDialog}
          onOpenChange={(open) => {
            setShowReportDialog(open);
            if (!open) setSelectedPost(null);
          }}
          type="post"
          targetId={selectedPost.writerId || Number(selectedPost.id)}
          targetName={selectedPost.user}
        />
      )}
    </div>
  );
}
