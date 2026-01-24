import { useState, useEffect, useCallback } from 'react';
import { getPost, deletePost as deletePostAPI, likePost as likePostAPI, unlikePost as unlikePostAPI, updatePost, type PostDetailResponse, type StoryUpdateRequest } from '../../../../api/post';
import { API_BASE_URL } from '@/api/client';
import { ReportDialog } from '../../report/ReportDialog';
import { getPostComments, createComment, updateComment, deleteComment, likeComment, unlikeComment, type PostCommentItem } from '../../../../api/comment';
import { getMyInfo } from '../../../../api/user';
import { getMembers, type MemberListResponse } from '../../../../api/member';
import { getVotes, getVote, answerVote, closeVote, type VoteDetailResponse, type VoteListResponse } from '../../../../api/vote';
import { askAI } from '../../../../api/ai';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Heart, MessageCircle, Send, Trash2, Flag, AlertTriangle, Edit2, Image, X, MapPin, Users, Clock, Vote, XCircle, Check, MoreVertical, Bot, Loader2 } from 'lucide-react';
import { Badge } from '../../ui/badge';
import { Progress } from '../../ui/progress';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { toast } from 'sonner';
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
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../ui/dropdown-menu';
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
import { useUserPermissions } from '../../../data/userRoles';

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
  const [selectedComment, setSelectedComment] = useState<PostCommentItem | null>(null);
  const [setReportTarget] = useState<'post' | 'comment'>('post');
  const [post, setPost] = useState<PostDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [comments, setComments] = useState<PostCommentItem[]>([]);
  const [commentPage, setCommentPage] = useState(0);
  const [hasMoreComments, setHasMoreComments] = useState(false);
  const [loadingComments, setLoadingComments] = useState(false);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [editingCommentContent, setEditingCommentContent] = useState('');
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [editContent, setEditContent] = useState('');
  const [editImages, setEditImages] = useState<string[]>([]);
  const [editLocation, setEditLocation] = useState('');
  const [editTaggedMembers, setEditTaggedMembers] = useState<number[]>([]);
  const [members, setMembers] = useState<MemberListResponse[]>([]);
  const [isUpdating, setIsUpdating] = useState(false);
  const [hasTaggedMembersChanged, setHasTaggedMembersChanged] = useState(false);

  // 투표 관련 상태
  const [linkedVote, setLinkedVote] = useState<VoteDetailResponse | null>(null);
  const [selectedVoteOptions, setSelectedVoteOptions] = useState<number[]>([]);
  const [isVoting, setIsVoting] = useState(false);
  const [isClosingVote, setIsClosingVote] = useState(false);

  // AI 채팅 관련 상태
  const [showAIChat, setShowAIChat] = useState(false);
  const [aiQuestion, setAiQuestion] = useState('');
  const [aiAnswer, setAiAnswer] = useState<string | null>(null);
  const [isAskingAI, setIsAskingAI] = useState(false);
  const [aiChatHistory, setAiChatHistory] = useState<Array<{ question: string; answer: string }>>([]);
  const [lastAIRequestTime, setLastAIRequestTime] = useState<number>(0);
  const MIN_AI_REQUEST_INTERVAL = 3000; // 최소 3초 간격

  // 현재 사용자 정보 조회
  useEffect(() => {
    async function fetchMyInfo() {
      try {
        const userInfo = await getMyInfo();
        setCurrentUserId(userInfo.userId);
      } catch (error) {
        console.error('사용자 정보 조회 실패:', error);
      }
    }
    fetchMyInfo();
  }, []);

  // 댓글 목록 조회
  const fetchComments = useCallback(async (page: number = 0) => {
    if (!groupId || !storyId) return;
    try {
      setLoadingComments(true);
      const response = await getPostComments(Number(groupId), Number(storyId), page, 20);
      if (page === 0) {
        setComments(response.comments);
      } else {
        setComments(prev => [...prev, ...response.comments]);
      }
      setHasMoreComments(response.hasNext);
      setCommentPage(page);
    } catch (error) {
      console.error('댓글 불러오기 실패:', error);
      toast.error('댓글을 불러오는데 실패했습니다.');
    } finally {
      setLoadingComments(false);
    }
  }, [groupId, storyId]);

  // 멤버 목록 조회
  useEffect(() => {
    async function fetchMembers() {
      if (!groupId) return;
      try {
        const membersData = await getMembers(Number(groupId));
        setMembers(membersData);
      } catch (error) {
        console.error('멤버 목록 조회 실패:', error);
      }
    }
    fetchMembers();
  }, [groupId]);

  // API에서 게시글 상세 조회
  useEffect(() => {
    async function fetchPost() {
      if (!groupId || !storyId) return;
      try {
        setLoading(true);
        const postData = await getPost(Number(groupId), Number(storyId));
        const userInfo = await getMyInfo().catch(() => null); // 실패해도 계속 진행
        const votesData = await getVotes(Number(groupId)).catch(() => [] as VoteListResponse[]);

        // 멤버 목록에서 작성자 정보 찾기
        const writerMember = members.find(m => m.userId === postData.writerId);
        const writerName = writerMember?.clubNickname || `사용자${postData.writerId}`;

        // 백엔드 응답에 없는 필드들을 보완
        const enrichedPost: PostDetailResponse = {
          ...postData,
          // writerName을 멤버 정보에서 가져오기
          writerName: postData.writerName || writerName,
          // writerProfileImageUrl이 없으면 null
          writerProfileImageUrl: postData.writerProfileImageUrl || null,
          // imagesUrl은 백엔드에서 이제 포함됨
          imagesUrl: postData.imagesUrl || [],
          // postLikes가 없으면 0
          postLikes: postData.postLikes || 0,
          // isLiked는 기본값 false (별도 확인 필요 시 API 호출)
          isLiked: postData.isLiked || false,
          // isMyPost는 writerId와 현재 사용자 ID 비교
          isMyPost: userInfo ? postData.writerId === userInfo.userId : false,
          // taggedMemberIds는 빈 배열 (별도 API 호출 필요 시 추가)
          taggedMemberIds: postData.taggedMemberIds || []
        };

        setPost(enrichedPost);
        setLiked(enrichedPost.isLiked || false);
        setLikeCount(enrichedPost.postLikes || 0);

        // currentUserId 설정 (아직 설정되지 않은 경우)
        if (userInfo && !currentUserId) {
          setCurrentUserId(userInfo.userId);
        }

        // 이 게시글과 연결된 투표 찾기
        const linkedVoteItem = votesData.find((v: VoteListResponse) => v.postId === Number(storyId));
        if (linkedVoteItem) {
          try {
            const voteDetail = await getVote(Number(groupId), linkedVoteItem.voteId);
            setLinkedVote(voteDetail);
            // 이미 투표한 옵션 설정
            if (voteDetail.mySelectedOptionIds) {
              setSelectedVoteOptions(voteDetail.mySelectedOptionIds);
            }
          } catch (error) {
            console.error('투표 상세 조회 실패:', error);
          }
        }

        // 댓글 목록 조회
        await fetchComments(0);
      } catch (error) {
        console.error('게시글 불러오기 실패:', error);
        toast.error('게시글을 불러오는데 실패했습니다.');
        navigate(-1);
      } finally {
        setLoading(false);
      }
    }
    fetchPost();
  }, [groupId, storyId, navigate, fetchComments, members]);


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

  const handleAddComment = async () => {
    if (!newComment.trim() || !groupId || !storyId) return;
    try {
      await createComment(Number(groupId), Number(storyId), {
        content: newComment.trim()
      });
      // 댓글 목록 새로고침
      await fetchComments(0);
      setNewComment('');
      toast.success('댓글이 등록되었습니다');
    } catch (error) {
      console.error('댓글 작성 실패:', error);
      toast.error('댓글 작성에 실패했습니다.');
    }
  };

  const handleAskAI = async () => {
    if (!aiQuestion.trim() || !groupId || !currentUserId) return;

    // 요청 간격 체크 (첫 요청이거나 3초 이상 지났으면 허용)
    const now = Date.now();
    if (lastAIRequestTime > 0) {
      const timeSinceLastRequest = now - lastAIRequestTime;
      if (timeSinceLastRequest < MIN_AI_REQUEST_INTERVAL) {
        const remainingTime = Math.ceil((MIN_AI_REQUEST_INTERVAL - timeSinceLastRequest) / 1000);
        toast.error(`잠시 후 다시 시도해주세요. (${remainingTime}초 대기 필요)`);
        return;
      }
    }

    try {
      setIsAskingAI(true);
      setAiAnswer(null);
      const response = await askAI(Number(groupId), currentUserId, aiQuestion.trim());

      // 성공한 경우에만 lastAIRequestTime 업데이트
      setLastAIRequestTime(now);

      setAiAnswer(response.answer);
      setAiChatHistory(prev => [...prev, { question: aiQuestion.trim(), answer: response.answer }]);
      setAiQuestion('');
      toast.success('AI 답변을 받았습니다');
    } catch (error) {
      console.error('AI 질문 실패:', error);
      toast.error('AI 질문에 실패했습니다.');
      setAiAnswer('죄송합니다. AI 서비스에 문제가 발생했습니다.');
    } finally {
      setIsAskingAI(false);
    }
  };

  const handleStartEditComment = (comment: PostCommentItem) => {
    setEditingCommentId(comment.commentId);
    setEditingCommentContent(comment.content);
  };

  const handleCancelEdit = () => {
    setEditingCommentId(null);
    setEditingCommentContent('');
  };

  const handleCommentLike = async (comment: PostCommentItem) => {
    if (!groupId || !storyId) return;
    try {
      if (comment.isLiked) {
        await unlikeComment(Number(groupId), Number(storyId), comment.commentId);
        setComments(prev => prev.map(c =>
          c.commentId === comment.commentId
            ? { ...c, isLiked: false, likeCount: (c.likeCount || 0) - 1 }
            : c
        ));
      } else {
        await likeComment(Number(groupId), Number(storyId), comment.commentId);
        setComments(prev => prev.map(c =>
          c.commentId === comment.commentId
            ? { ...c, isLiked: true, likeCount: (c.likeCount || 0) + 1 }
            : c
        ));
      }
    } catch (error) {
      console.error('댓글 좋아요 실패:', error);
      toast.error('댓글 좋아요에 실패했습니다');
    }
  };

  const handleUpdateComment = async () => {
    if (!editingCommentId || !editingCommentContent.trim() || !groupId || !storyId) return;
    try {
      await updateComment(Number(groupId), Number(storyId), editingCommentId, {
        content: editingCommentContent.trim()
      });
      // 댓글 목록 새로고침
      await fetchComments(commentPage);
      setEditingCommentId(null);
      setEditingCommentContent('');
      toast.success('댓글이 수정되었습니다');
    } catch (error) {
      console.error('댓글 수정 실패:', error);
      toast.error('댓글 수정에 실패했습니다.');
    }
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

  const handleDeleteComment = async () => {
    if (!selectedComment || !groupId || !storyId) return;
    try {
      await deleteComment(Number(groupId), Number(storyId), selectedComment.commentId);
      // 댓글 목록 새로고침
      await fetchComments(commentPage);
      toast.success('댓글이 삭제되었습니다');
      setShowDeleteDialog(false);
      setSelectedComment(null);
    } catch (error) {
      console.error('댓글 삭제 실패:', error);
      toast.error('댓글 삭제에 실패했습니다.');
      setShowDeleteDialog(false);
    }
  };
  // 투표 옵션 토글
  const toggleVoteOption = (optionId: number) => {
    if (!linkedVote || linkedVote.status === 'CLOSED') return;

    if (linkedVote.allowMultiple) {
      setSelectedVoteOptions(prev =>
        prev.includes(optionId)
          ? prev.filter(id => id !== optionId)
          : [...prev, optionId]
      );
    } else {
      setSelectedVoteOptions([optionId]);
    }
  };

  // 투표 제출
  const handleVoteSubmit = async () => {
    if (!groupId || !linkedVote || selectedVoteOptions.length === 0) {
      toast.error('최소 하나의 항목을 선택해주세요');
      return;
    }

    try {
      setIsVoting(true);
      await answerVote(Number(groupId), linkedVote.voteId, { optionIds: selectedVoteOptions });

      // 투표 데이터 새로고침
      const updatedVote = await getVote(Number(groupId), linkedVote.voteId);
      setLinkedVote(updatedVote);
      if (updatedVote.mySelectedOptionIds) {
        setSelectedVoteOptions(updatedVote.mySelectedOptionIds);
      }

      toast.success('투표가 완료되었습니다!');
    } catch (error: any) {
      console.error('투표 실패:', error);
      toast.error(error.message || '투표에 실패했습니다.');
    } finally {
      setIsVoting(false);
    }
  };

  // 투표 종료
  const handleCloseVote = async () => {
    if (!groupId || !linkedVote) return;

    try {
      setIsClosingVote(true);
      await closeVote(Number(groupId), linkedVote.voteId);

      // 투표 데이터 새로고침
      const updatedVote = await getVote(Number(groupId), linkedVote.voteId);
      setLinkedVote(updatedVote);

      toast.success('투표가 종료되었습니다');
    } catch (error) {
      console.error('투표 종료 실패:', error);
      toast.error('투표 종료에 실패했습니다.');
    } finally {
      setIsClosingVote(false);
    }
  };

  const handleStartEdit = () => {
    if (!post) return;
    setEditContent(post.content || '');
    setEditImages(post.imagesUrl || []);
    setEditLocation(post.place || '');
    // 백엔드 응답에 taggedMemberIds가 없을 수 있으므로, 있으면 사용하고 없으면 빈 배열
    // 기존 태그를 불러올 수 없으므로 빈 배열로 시작
    setEditTaggedMembers(post.taggedMemberIds || []);
    setHasTaggedMembersChanged(false); // 수정 다이얼로그를 열 때는 변경되지 않음
    setShowEditDialog(true);
  };

  const handleUpdatePost = async () => {
    if (!groupId || !storyId || !post) return;

    if (!editContent.trim() && editImages.length === 0) {
      toast.error('내용 또는 이미지를 입력해주세요');
      return;
    }

    try {
      setIsUpdating(true);
      const request: StoryUpdateRequest = {
        content: editContent.trim() || null,
        imagesUrl: editImages.length > 0 ? editImages : null,
        place: editLocation.trim() || null,
        // 백엔드 로직: null이면 변경 없음, 빈 리스트면 전체 삭제, 값 있으면 교체
        // 사용자가 태그를 변경했는지 여부에 따라 처리
        taggedMemberIds: hasTaggedMembersChanged
          ? editTaggedMembers  // 변경했으면 현재 배열 전송 (빈 배열이어도 삭제 의미)
          : null,  // 변경하지 않았으면 null (변경 없음)
      };
      await updatePost(Number(groupId), Number(storyId), request);
      toast.success('게시글이 수정되었습니다');
      setShowEditDialog(false);
      // 게시글 정보 새로고침
      const updatedPostData = await getPost(Number(groupId), Number(storyId));
      // 백엔드 응답 보완
      const updatedPost: PostDetailResponse = {
        ...updatedPostData,
        writerName: updatedPostData.writerName || `사용자${updatedPostData.writerId}`,
        writerProfileImageUrl: updatedPostData.writerProfileImageUrl || null,
        imagesUrl: updatedPostData.imagesUrl || [],
        postLikes: updatedPostData.postLikes || 0,
        isLiked: updatedPostData.isLiked || false,
        isMyPost: currentUserId !== null ? updatedPostData.writerId === currentUserId : false,
        taggedMemberIds: updatedPostData.taggedMemberIds || []
      };
      setPost(updatedPost);
      setLiked(updatedPost.isLiked || false);
      setLikeCount(updatedPost.postLikes || 0);
    } catch (error) {
      console.error('게시글 수정 실패:', error);
      toast.error('게시글 수정에 실패했습니다.');
    } finally {
      setIsUpdating(false);
    }
  };

  const removeEditImage = (index: number) => {
    setEditImages(editImages.filter((_, i) => i !== index));
  };

  const handleImageUpload = () => {
    // TODO: 실제 이미지 업로드 API 연동
    toast.info('이미지 업로드 기능은 준비 중입니다');
  };

  const toggleEditMember = (memberId: number) => {
    setHasTaggedMembersChanged(true); // 태그 멤버 변경 감지
    setEditTaggedMembers(prev =>
      prev.includes(memberId)
        ? prev.filter(id => id !== memberId)
        : [...prev, memberId]
    );
  };

  // 백엔드 응답에 isMyPost가 없을 수 있으므로 writerId와 currentUserId 비교로 보완
  const isMyPost = post ? (post.isMyPost !== undefined ? post.isMyPost : (currentUserId !== null && post.writerId === currentUserId)) : false;
  const canDeletePost = isMyPost || permissions.canDeletePosts;
  const canEditPost = isMyPost || permissions.canDeletePosts; // 작성자 또는 운영진 이상
  const canDeleteComment = (comment: PostCommentItem) => comment.writerId === currentUserId || permissions.canDeleteComments;
  const canEditComment = (comment: PostCommentItem) => comment.writerId === currentUserId;

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
      <div className="sticky top-0 bg-white z-[70] shadow-sm">
        <div className="flex items-center justify-between p-4">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)}>
            <ArrowLeft className="w-5 h-5" />
          </Button>
          <span className="font-medium">게시글</span>
          <div className="flex flex-col gap-1">
            {canEditPost && (
              <Button
                variant="ghost"
                size="icon"
                onClick={handleStartEdit}
                className="h-8 w-8"
                aria-label="게시글 수정"
              >
                <Edit2 className="w-4 h-4 text-stone-600" />
              </Button>
            )}
            {canDeletePost && (
              <Button
                variant="ghost"
                size="icon"
                onClick={() => {
                  setDeleteTarget('post');
                  setShowDeleteDialog(true);
                }}
                className="h-8 w-8"
                aria-label="게시글 삭제"
              >
                <Trash2 className="w-4 h-4 text-red-600" />
              </Button>
            )}
            {!canEditPost && !canDeletePost && (
              <Button
                variant="ghost"
                size="icon"
                onClick={() => {
                  // @ts-ignore
                  setReportTarget('post');
                  setShowReportDialog(true);
                }}
                className="h-8 w-8"
                aria-label="신고하기"
              >
                <Flag className="w-4 h-4 text-orange-600" />
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Post Content */}
      <div className="pb-32">
        {/* Author */}
        <div className="p-4 flex items-center gap-3">
          <img
            src={post.writerProfileImageUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${post.writerName || post.writerId}`}
            alt=""
            className="w-10 h-10 rounded-full bg-stone-200"
          />
          <div>
            <p className="font-bold text-stone-900">{post.writerName || `사용자${post.writerId}`}</p>
            <p className="text-xs text-stone-400">{new Date(post.createdAt).toLocaleDateString('ko-KR')}</p>
          </div>
        </div>

        {/* Images */}
        {post.imagesUrl && post.imagesUrl.length > 0 && (
          <div className="space-y-2">
            {post.imagesUrl.map((imgUrl, index) => (
              <div key={index} className="aspect-square bg-stone-100">
                <img
                  src={imgUrl.startsWith('http') ? imgUrl : `${API_BASE_URL}${imgUrl}`}
                  alt=""
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    const target = e.target as HTMLImageElement;
                    target.style.display = 'none';
                  }}
                />
              </div>
            ))}
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
        {post?.content && (
          <div className="px-4 pb-4">
            <p className="text-stone-800 leading-relaxed">{post.content}</p>
          </div>
        )}

        {/* Vote Section */}
        {linkedVote && (
          <div className="px-4 pb-4">
            <div className="bg-stone-50 rounded-2xl p-4 border border-stone-200">
              {/* Vote Header */}
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Badge variant="outline" className="text-orange-600 border-orange-300">
                    <Vote className="w-3 h-3 mr-1" />
                    투표
                  </Badge>
                  {linkedVote.allowMultiple && (
                    <Badge variant="secondary" className="text-xs">복수 선택</Badge>
                  )}
                  {linkedVote.status === 'CLOSED' && (
                    <Badge variant="secondary" className="bg-stone-200 text-stone-600">종료됨</Badge>
                  )}
                </div>
                {permissions.canManageGroup && linkedVote.status === 'OPEN' && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleCloseVote}
                    disabled={isClosingVote}
                    className="text-red-600 hover:text-red-700 hover:bg-red-50 h-8 px-2"
                  >
                    <XCircle className="w-4 h-4 mr-1" />
                    {isClosingVote ? '종료 중...' : '투표 종료'}
                  </Button>
                )}
              </div>

              {/* Vote Title */}
              <h3 className="font-bold text-stone-900 mb-2">{linkedVote.title}</h3>
              {linkedVote.description && (
                <p className="text-sm text-stone-600 mb-4">{linkedVote.description}</p>
              )}

              {/* Vote Deadline */}
              {linkedVote.deadline && (
                <div className="flex items-center gap-1 text-sm text-stone-500 mb-4">
                  <Clock className="w-4 h-4" />
                  <span>마감: {new Date(linkedVote.deadline).toLocaleString('ko-KR')}</span>
                </div>
              )}

              {/* Vote Options */}
              <div className="space-y-2">
                {(linkedVote.options || []).map(option => {
                  const isSelected = selectedVoteOptions.includes(option.optionId);
                  const voteCount = option.voteCount || 0;
                  const totalVotes = (linkedVote.options || []).reduce((sum, opt) => sum + (opt.voteCount || 0), 0);
                  const percentage = totalVotes > 0 ? Math.round((voteCount / totalVotes) * 100) : 0;

                  return (
                    <button
                      key={option.optionId}
                      onClick={() => toggleVoteOption(option.optionId)}
                      disabled={linkedVote.status === 'CLOSED'}
                      className={`w-full text-left p-3 rounded-xl border-2 transition-all ${isSelected
                        ? 'border-orange-500 bg-orange-50'
                        : 'border-stone-200 hover:border-stone-300 bg-white'
                        } ${linkedVote.status === 'CLOSED' ? 'cursor-default opacity-70' : 'cursor-pointer'}`}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${isSelected
                            ? 'bg-orange-500 border-orange-500'
                            : 'border-stone-300'
                            }`}>
                            {isSelected && <Check className="w-3 h-3 text-white" />}
                          </div>
                          <span className={`font-medium ${isSelected ? 'text-orange-700' : 'text-stone-900'}`}>
                            {option.optionText || '옵션'}
                          </span>
                        </div>
                        <span className={`text-sm ${isSelected ? 'text-orange-600' : 'text-stone-500'}`}>
                          {voteCount}표 ({percentage}%)
                        </span>
                      </div>
                      <Progress
                        value={percentage}
                        className="h-1.5 bg-stone-200"
                      />
                    </button>
                  );
                })}
              </div>

              {/* Vote Statistics */}
              <div className="mt-4 pt-3 border-t border-stone-200 flex items-center justify-between">
                <span className="text-sm text-stone-500">
                  총 {(linkedVote.options || []).reduce((sum, opt) => sum + (opt.voteCount || 0), 0)}명 참여
                </span>
                {linkedVote.status === 'OPEN' && (
                  <Button
                    onClick={handleVoteSubmit}
                    disabled={selectedVoteOptions.length === 0 || isVoting}
                    className="bg-orange-500 hover:bg-orange-600 text-white h-9"
                  >
                    {isVoting ? '투표 중...' : (linkedVote.mySelectedOptionIds?.length ? '투표 수정' : '투표하기')}
                  </Button>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Divider */}
        <div className="h-2 bg-stone-100"></div>

        {/* Comments */}
        <div className="p-4 space-y-4">
          <h3 className="font-bold text-stone-900">댓글 {comments.length}개</h3>

          {loadingComments && comments.length === 0 ? (
            <div className="text-center py-8 text-stone-500">댓글을 불러오는 중...</div>
          ) : comments.length > 0 ? (
            <>
              {comments.map(comment => {
                // 댓글 작성자 정보 조회
                const commentWriter = members.find(m => m.userId === comment.writerId);
                const commentWriterName = commentWriter?.clubNickname || `사용자${comment.writerId}`;
                return (
                  <div key={comment.commentId} className="flex gap-3">
                    <div className="w-8 h-8 rounded-full bg-stone-200 shrink-0 flex items-center justify-center">
                      <span className="text-xs font-medium text-stone-600">
                        {commentWriterName[0] || 'U'}
                      </span>
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="font-medium text-sm text-stone-900">
                            {commentWriterName}
                          </span>
                          <span className="text-xs text-stone-400">
                            {new Date(comment.createdAt).toLocaleDateString('ko-KR', {
                              month: 'short',
                              day: 'numeric',
                              hour: '2-digit',
                              minute: '2-digit'
                            })}
                          </span>
                        </div>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="h-6 w-6 text-stone-400">
                              <MoreVertical className="w-3 h-3" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <>
                              {canEditComment(comment) ? (
                                <>
                                  <DropdownMenuItem
                                    onClick={() => handleStartEditComment(comment)}
                                  >
                                    <Edit2 className="w-4 h-4 mr-2" />
                                    수정
                                  </DropdownMenuItem>
                                  <DropdownMenuSeparator />
                                </>
                              ) : null}
                              {canDeleteComment(comment) ? (
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
                              ) : null}
                              <DropdownMenuItem
                                className="text-orange-600"
                                onClick={() => {
                                  setSelectedComment(comment);
                                  // @ts-ignore
                                  setReportTarget('comment');
                                  setShowReportDialog(true);
                                }}
                              >
                                <Flag className="w-4 h-4 mr-2" />
                                신고
                              </DropdownMenuItem>
                            </>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                      {editingCommentId === comment.commentId ? (
                        <div className="mt-2 space-y-2">
                          <Textarea
                            value={editingCommentContent}
                            onChange={(e) => setEditingCommentContent(e.target.value)}
                            className="min-h-[60px] resize-none"
                            maxLength={500}
                          />
                          <div className="flex gap-2">
                            <Button
                              size="sm"
                              onClick={handleUpdateComment}
                              disabled={!editingCommentContent.trim()}
                              className="bg-orange-500 hover:bg-orange-600"
                            >
                              저장
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={handleCancelEdit}
                            >
                              취소
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <>
                          <p className="text-sm text-stone-700 mt-1">{comment.content}</p>
                          <div className="flex items-center gap-4 mt-2">
                            <button
                              onClick={() => handleCommentLike(comment)}
                              className="flex items-center gap-1 text-stone-500 hover:text-orange-500 transition-colors"
                            >
                              <Heart
                                className={`w-4 h-4 ${comment.isLiked ? 'fill-orange-500 text-orange-500' : ''}`}
                              />
                              <span className="text-xs font-medium">
                                {comment.likeCount || 0}
                              </span>
                            </button>
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                );
              })}
              {hasMoreComments && (
                <Button
                  variant="outline"
                  onClick={() => fetchComments(commentPage + 1)}
                  disabled={loadingComments}
                  className="w-full"
                >
                  {loadingComments ? '로딩 중...' : '더보기'}
                </Button>
              )}
            </>
          ) : (
            <div className="text-center py-8 text-stone-500">아직 댓글이 없습니다</div>
          )}
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
      {post && (
        <ReportDialog
          open={showReportDialog}
          onOpenChange={setShowReportDialog}
          type={selectedComment ? 'comment' : 'post'}
          targetId={selectedComment ? selectedComment.writerId : post.writerId}
          targetName={selectedComment ? '댓글 작성자' : (post.writerName || '작성자')}
        />
      )}

      {/* 게시글 수정 다이얼로그 */}
      <Dialog open={showEditDialog} onOpenChange={setShowEditDialog}>
        <DialogContent className="max-w-[600px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>게시글 수정</DialogTitle>
            <DialogDescription>
              게시글 내용을 수정할 수 있습니다. (일정 연결은 수정할 수 없습니다)
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            {/* Content Input */}
            <div className="space-y-2">
              <Label>내용</Label>
              <Textarea
                placeholder="게시글 내용을 입력하세요..."
                className="min-h-32 resize-none"
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                maxLength={500}
              />
              <p className="text-xs text-stone-400 text-right">{editContent.length}/500</p>
            </div>

            {/* Images */}
            {editImages.length > 0 && (
              <div className="grid grid-cols-3 gap-2">
                {editImages.map((img, index) => (
                  <div key={index} className="relative aspect-square rounded-xl overflow-hidden bg-stone-100">
                    <img src={img.startsWith('http') || img.startsWith('data:') ? img : `${API_BASE_URL}${img}`} alt="" className="w-full h-full object-cover" draggable={false} onDragStart={(e) => e.preventDefault()} />
                    <button
                      onClick={() => removeEditImage(index)}
                      className="absolute top-1 right-1 w-6 h-6 bg-black/50 rounded-full flex items-center justify-center"
                    >
                      <X className="w-4 h-4 text-white" />
                    </button>
                  </div>
                ))}
                {editImages.length < 9 && (
                  <button
                    onClick={handleImageUpload}
                    className="aspect-square rounded-xl border-2 border-dashed border-stone-200 flex flex-col items-center justify-center text-stone-400 hover:border-orange-300 hover:text-orange-500 transition-colors"
                  >
                    <Image className="w-6 h-6" />
                    <span className="text-xs mt-1">추가</span>
                  </button>
                )}
              </div>
            )}

            {/* Add Image Button (when no images) */}
            {editImages.length === 0 && (
              <button
                onClick={handleImageUpload}
                className="w-full p-6 border-2 border-dashed border-stone-200 rounded-xl flex flex-col items-center justify-center text-stone-400 hover:border-orange-300 hover:text-orange-500 transition-colors"
              >
                <Image className="w-8 h-8" />
                <span className="mt-2">사진 추가</span>
                <span className="text-xs mt-1">최대 9장까지</span>
              </button>
            )}

            {/* Location */}
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <MapPin className="w-4 h-4" />
                위치
              </Label>
              <Input
                placeholder="위치를 입력하세요"
                className="h-11 bg-stone-50 border-stone-200 rounded-xl"
                value={editLocation}
                onChange={(e) => setEditLocation(e.target.value)}
              />
            </div>

            {/* Tag Members */}
            {members.length > 0 && (
              <div className="space-y-3">
                <Label className="flex items-center gap-2">
                  <Users className="w-4 h-4" />
                  멤버 태그 (선택)
                </Label>
                <div className="flex flex-wrap gap-2">
                  {members.map(member => {
                    const isSelected = editTaggedMembers.includes(member.memberId);
                    return (
                      <button
                        key={member.memberId}
                        onClick={() => toggleEditMember(member.memberId)}
                        className={`px-3 py-1.5 rounded-full text-sm transition-colors ${isSelected
                          ? 'bg-orange-500 text-white'
                          : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                          }`}
                      >
                        @{member.clubNickname || '멤버'}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowEditDialog(false)}>
              취소
            </Button>
            <Button
              onClick={handleUpdatePost}
              disabled={isUpdating || (!editContent.trim() && editImages.length === 0)}
              className="bg-orange-500 hover:bg-orange-600"
            >
              {isUpdating ? '수정 중...' : '수정하기'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
