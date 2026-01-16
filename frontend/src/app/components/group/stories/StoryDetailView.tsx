import { useState, useEffect } from 'react';
import { getPost, deletePost as deletePostAPI, likePost as likePostAPI, unlikePost as unlikePostAPI, type PostDetailResponse } from '../../../../api/post';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Heart, MessageCircle, Send, MoreVertical, Trash2, Flag, AlertTriangle } from 'lucide-react';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
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
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../ui/dialog';
import { Label } from '../../ui/label';
import { Textarea } from '../../ui/textarea';
import { RadioGroup, RadioGroupItem } from '../../ui/radio-group';
import { useUserPermissions } from '../../../data/userRoles';

interface Comment {
  id: string;
  user: string;
  userImg: string;
  content: string;
  date: string;
  isMyComment?: boolean;
}

export function StoryDetailView() {
  const { groupId, storyId } = useParams();
  const navigate = useNavigate();
  const permissions = useUserPermissions(groupId || '1');
  
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [newComment, setNewComment] = useState('');
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [showReportDialog, setShowReportDialog] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<'post' | 'comment'>('post');
  const [selectedComment, setSelectedComment] = useState<Comment | null>(null);
  const [reportReason, setReportReason] = useState('');
  const [reportDetail, setReportDetail] = useState('');
  const [post, setPost] = useState<PostDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [comments, setComments] = useState<Comment[]>([]);

  // API에서 게시글 상세 조회
  useEffect(() => {
    async function fetchPost() {
      if (!groupId || !storyId) return;
      try {
        setLoading(true);
        const postData = await getPost(Number(groupId), Number(storyId));
        setPost(postData);
        setLiked(postData.isLiked || false);
        setLikeCount(postData.postLikes || 0);
      } catch (error) {
        console.error('게시글 불러오기 실패:', error);
        toast.error('게시글을 불러오는데 실패했습니다.');
        navigate(-1);
      } finally {
        setLoading(false);
      }
    }
    fetchPost();
  }, [groupId, storyId, navigate]);

  const reportReasons = [
    { value: 'spam', label: '스팸/광고' },
    { value: 'inappropriate', label: '부적절한 내용' },
    { value: 'harassment', label: '괴롭힘/혐오 표현' },
    { value: 'copyright', label: '저작권 침해' },
    { value: 'other', label: '기타' },
  ];

  const handleLike = async () => {
    if (!groupId || !storyId) return;
    try {
      if (liked) {
        await unlikePostAPI(Number(groupId), Number(storyId));
        setLiked(false);
        setLikeCount(prev => Math.max(0, prev - 1));
      } else {
        await likePostAPI(Number(groupId), Number(storyId));
        setLiked(true);
        setLikeCount(prev => prev + 1);
      }
    } catch (error) {
      console.error('좋아요 처리 실패:', error);
      toast.error('좋아요 처리에 실패했습니다.');
    }
  };

  const handleAddComment = () => {
    if (!newComment.trim()) return;
    const comment: Comment = {
      id: String(Date.now()),
      user: '나',
      userImg: '',
      content: newComment,
      date: '방금',
      isMyComment: true,
    };
    setComments([...comments, comment]);
    setNewComment('');
    toast.success('댓글이 등록되었습니다');
  };

  const handleDeletePost = async () => {
    if (!groupId || !storyId) return;
    try {
      await deletePostAPI(Number(groupId), Number(storyId));
      toast.success('게시글이 삭제되었습니다');
      setShowDeleteDialog(false);
      navigate(-1);
    } catch (error) {
      console.error('게시글 삭제 실패:', error);
      toast.error('게시글 삭제에 실패했습니다.');
    }
  };

  const handleDeleteComment = () => {
    if (!selectedComment) return;
    setComments(comments.filter(c => c.id !== selectedComment.id));
    toast.success('댓글이 삭제되었습니다');
    setShowDeleteDialog(false);
    setSelectedComment(null);
  };

  const handleReport = () => {
    if (!reportReason) {
      toast.error('신고 사유를 선택해주세요');
      return;
    }
    toast.success('신고가 접수되었습니다');
    setShowReportDialog(false);
    setReportReason('');
    setReportDetail('');
  };

  const canDeletePost = post?.isMyPost || permissions.canDeletePosts;
  const canDeleteComment = (comment: Comment) => comment.isMyComment || permissions.canDeleteComments;

  if (loading || !post) {
    return (
      <div className="min-h-screen bg-white flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <div className="sticky top-0 bg-white/80 backdrop-blur-lg z-10 border-b border-stone-100">
        <div className="flex items-center justify-between p-4">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)}>
            <ArrowLeft className="w-5 h-5" />
          </Button>
          <span className="font-medium">게시글</span>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon">
                <MoreVertical className="w-5 h-5" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              {canDeletePost && (
                <>
                  <DropdownMenuItem 
                    className="text-red-600"
                    onClick={() => {
                      setDeleteTarget('post');
                      setShowDeleteDialog(true);
                    }}
                  >
                    <Trash2 className="w-4 h-4 mr-2" />
                    게시글 삭제
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                </>
              )}
              <DropdownMenuItem 
                className="text-orange-600"
                onClick={() => setShowReportDialog(true)}
              >
                <Flag className="w-4 h-4 mr-2" />
                신고하기
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {/* Post Content */}
      <div className="pb-32">
        {/* Author */}
        <div className="p-4 flex items-center gap-3">
          <img 
            src={post.writerProfileImageUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${post.writerName}`} 
            alt="" 
            className="w-10 h-10 rounded-full bg-stone-200" 
          />
          <div>
            <p className="font-bold text-stone-900">{post.writerName}</p>
            <p className="text-xs text-stone-400">{new Date(post.createdAt).toLocaleDateString('ko-KR')}</p>
          </div>
        </div>

        {/* Image */}
        {post.imagesUrl && post.imagesUrl.length > 0 && (
          <div className="aspect-square bg-stone-100">
            <img src={post.imagesUrl[0]} alt="" className="w-full h-full object-cover" />
          </div>
        )}

        {/* Actions */}
        <div className="p-4 flex items-center gap-4">
          <button onClick={handleLike} className="flex items-center gap-1">
            <Heart className={`w-6 h-6 ${liked ? 'fill-red-500 text-red-500' : 'text-stone-600'}`} />
            <span className="font-medium">{likeCount}</span>
          </button>
          <div className="flex items-center gap-1 text-stone-600">
            <MessageCircle className="w-6 h-6" />
            <span className="font-medium">{comments.length}</span>
          </div>
        </div>

        {/* Content */}
        <div className="px-4 pb-4">
          <p className="text-stone-800 leading-relaxed">{post?.content || ''}</p>
        </div>

        {/* Divider */}
        <div className="h-2 bg-stone-100"></div>

        {/* Comments */}
        <div className="p-4 space-y-4">
          <h3 className="font-bold text-stone-900">댓글 {comments.length}개</h3>
          
          {comments.map(comment => (
            <div key={comment.id} className="flex gap-3">
              <img 
                src={comment.userImg || `https://api.dicebear.com/7.x/initials/svg?seed=${comment.user}`}
                alt="" 
                className="w-8 h-8 rounded-full bg-stone-200 shrink-0" 
              />
              <div className="flex-1">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-sm text-stone-900">{comment.user}</span>
                    <span className="text-xs text-stone-400">{comment.date}</span>
                  </div>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon" className="h-6 w-6 text-stone-400">
                        <MoreVertical className="w-3 h-3" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      {canDeleteComment(comment) && (
                        <>
                          <DropdownMenuItem 
                            className="text-red-600"
                            onClick={() => {
                              setSelectedComment(comment);
                              setDeleteTarget('comment');
                              setShowDeleteDialog(true);
                            }}
                          >
                            <Trash2 className="w-4 h-4 mr-2" />
                            삭제
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                        </>
                      )}
                      <DropdownMenuItem 
                        className="text-orange-600"
                        onClick={() => {
                          setSelectedComment(comment);
                          setShowReportDialog(true);
                        }}
                      >
                        <Flag className="w-4 h-4 mr-2" />
                        신고
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
                <p className="text-sm text-stone-700 mt-1">{comment.content}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Comment Input */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4">
        <div className="max-w-[500px] mx-auto flex gap-2">
          <Input
            placeholder="댓글을 입력해주세요.."
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleAddComment()}
            className="flex-1"
          />
          <Button onClick={handleAddComment} className="bg-orange-500 hover:bg-orange-600">
            <Send className="w-4 h-4" />
          </Button>
        </div>
      </div>

      {/* 삭제 확인 다이얼로그 */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 bg-red-100 rounded-full">
                <AlertTriangle className="w-6 h-6 text-red-600" />
              </div>
              <AlertDialogTitle className="text-xl">
                {deleteTarget === 'post' ? '게시글 삭제' : '댓글 삭제'}
              </AlertDialogTitle>
            </div>
            <AlertDialogDescription>
              {deleteTarget === 'post' 
                ? '이 게시글을 삭제하시겠습니까? 삭제된 게시글은 복구할 수 없습니다.'
                : '이 댓글을 삭제하시겠습니까?'
              }
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={deleteTarget === 'post' ? handleDeletePost : handleDeleteComment}
              className="bg-red-500 hover:bg-red-600"
            >
              삭제하기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* 신고 다이얼로그 */}
      <Dialog open={showReportDialog} onOpenChange={setShowReportDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Flag className="w-5 h-5 text-orange-500" />
              {selectedComment ? '댓글 신고' : '게시글 신고'}
            </DialogTitle>
            <DialogDescription>
              신고 사유를 선택해 주시고 상세 내용을 입력해주세요.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4 space-y-4">
            <div className="space-y-3">
              <Label>신고 사유</Label>
              <RadioGroup value={reportReason} onValueChange={setReportReason}>
                {reportReasons.map(reason => (
                  <div key={reason.value} className="flex items-center space-x-2">
                    <RadioGroupItem value={reason.value} id={`detail-${reason.value}`} />
                    <Label htmlFor={`detail-${reason.value}`} className="cursor-pointer">
                      {reason.label}
                    </Label>
                  </div>
                ))}
              </RadioGroup>
            </div>
            <div className="space-y-2">
              <Label>상세 내용 (선택)</Label>
              <Textarea
                placeholder="구체적인 내용을 입력해주세요"
                value={reportDetail}
                onChange={(e) => setReportDetail(e.target.value)}
                className="min-h-[100px]"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowReportDialog(false)}>
              취소
            </Button>
            <Button onClick={handleReport} className="bg-orange-500 hover:bg-orange-600">
              신고하기
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
