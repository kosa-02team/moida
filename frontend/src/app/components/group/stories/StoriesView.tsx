import { useState, useEffect } from 'react';
import { useParams, Link, useOutletContext } from 'react-router-dom';
import { getRecentPosts, likePost, unlikePost, type PostCardResponse } from '@/api/post';
import { getVotes, getVote, answerVote, type VoteListResponse, type VoteDetailResponse } from '@/api/vote';
import { Card, CardContent } from '../../ui/card';
import { Badge } from '../../ui/badge';
import { Button } from '../../ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';
import { Heart, MessageCircle, Calendar, Clock, Users, ChevronRight, Plus } from 'lucide-react';
import { toast } from 'sonner';
import { ClubDetailResponse } from '@/api/club-full';

interface GroupContextType {
  club: ClubDetailResponse | null;
  loading: boolean;
}

interface PostWithVote extends PostCardResponse {
  voteId?: number;
  voteDetail?: VoteDetailResponse;
  mySelectedOptionIds?: number[];
  totalVoteCount?: number;
  isLiked?: boolean;
}

export function StoriesView() {
  const { groupId } = useParams<{ groupId: string }>();
  useOutletContext<GroupContextType>(); // context 구독
  const [allPosts, setAllPosts] = useState<PostWithVote[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  // 게시글과 투표 데이터 가져오기
  useEffect(() => {
    async function fetchPostsAndVotes() {
      if (!groupId) return;

      try {
        setLoading(true);
        // Promise.all에 as const를 적용하여 타입 추론 문제 해결
        const [posts, votes] = await Promise.all([
          getRecentPosts(Number(groupId), page, 20),
          getVotes(Number(groupId))
        ] as const);
        // 타입 단언으로 명시적 타입 지정
        const typedPosts = posts as PostCardResponse[];
        const typedVotes = votes as VoteListResponse[];

        // 투표를 postId로 매핑
        const voteMap = new Map<number, VoteListResponse>();
        for (const vote of typedVotes) {
          const postId = vote.postId;
          if (postId !== null && postId !== undefined && typeof postId === 'number') {
            voteMap.set(postId, vote);
          }
        }

        // 게시글에 투표 정보 추가
        const postsWithVotes: PostWithVote[] = await Promise.all(
          typedPosts.map(async (post): Promise<PostWithVote> => {
            const vote = voteMap.get(post.postId);
            if (vote) {
              try {
                const voteDetail = await getVote(Number(groupId), vote.voteId);
                return {
                  ...post,
                  voteId: vote.voteId,
                  voteDetail,
                  mySelectedOptionIds: voteDetail.mySelectedOptionIds || [],
                  totalVoteCount: voteDetail.options.reduce((sum, opt) => sum + (opt.voteCount || 0), 0)
                };
              } catch (error) {
                console.error(`투표 ${vote.voteId} 조회 실패:`, error);
                return { ...post, voteId: vote.voteId };
              }
            }
            return post as PostWithVote;
          })
        );

        if (page === 0) {
          setAllPosts(postsWithVotes);
        } else {
          setAllPosts(prev => [...prev, ...postsWithVotes]);
        }

        setHasMore(typedPosts.length === 20);
      } catch (error) {
        console.error('게시글 불러오기 실패:', error);
        toast.error('게시글을 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }

    fetchPostsAndVotes();
  }, [groupId, page]);

  // 날짜 포맷팅
  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
      const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
      if (diffHours === 0) {
        const diffMinutes = Math.floor(diffTime / (1000 * 60));
        return diffMinutes <= 0 ? '방금 전' : `${diffMinutes}분 전`;
      }
      return `${diffHours}시간 전`;
    }
    if (diffDays === 1) return '어제';
    if (diffDays < 7) return `${diffDays}일 전`;
    return date.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' });
  };

  // 투표 옵션 토글 핸들러
  const handleVoteOptionToggle = async (post: PostWithVote, optionId: number) => {
    if (!groupId || !post.voteId || !post.voteDetail) return;

    const voteDetail = post.voteDetail;
    const currentSelected = post.mySelectedOptionIds || [];
    const isSelected = currentSelected.includes(optionId);

    try {
      let newSelected: number[];

      if (voteDetail.allowMultiple) {
        // 복수 선택: 토글 방식
        if (isSelected) {
          newSelected = currentSelected.filter(id => id !== optionId);
          // 빈 배열 방지: 최소 1개 이상 선택해야 함
          if (newSelected.length === 0) {
            toast.info('최소 하나의 옵션을 선택해야 합니다.');
            return;
          }
        } else {
          newSelected = [...currentSelected, optionId];
        }
      } else {
        // 단일 선택: 같은 옵션 클릭 시 다른 옵션으로 변경만 가능
        if (isSelected) {
          // 이미 선택된 옵션을 다시 클릭하면 무시 (다른 옵션을 선택해야 변경됨)
          toast.info('다른 옵션을 선택하면 투표가 변경됩니다.');
          return;
        }
        newSelected = [optionId];
      }

      // 백엔드에 투표 전송 (백엔드가 전체 교체 방식으로 처리)
      await answerVote(Number(groupId), post.voteId, { optionIds: newSelected });

      // 투표 후 최신 데이터 가져오기
      const updatedVoteDetail = await getVote(Number(groupId), post.voteId);

      // UI 업데이트
      setAllPosts(posts => posts.map(p => {
        if (p.postId === post.postId) {
          return {
            ...p,
            voteDetail: updatedVoteDetail,
            mySelectedOptionIds: updatedVoteDetail.mySelectedOptionIds || [],
            totalVoteCount: updatedVoteDetail.options.reduce((sum, opt) => sum + (opt.voteCount || 0), 0)
          };
        }
        return p;
      }));

      toast.success('투표가 수정되었습니다!');
    } catch (error: any) {
      console.error('투표 실패:', error);
      toast.error(error.message || '투표에 실패했습니다.');
    }
  };

  // 좋아요 토글 핸들러
  const handlePostLike = async (e: React.MouseEvent, post: PostWithVote) => {
    e.preventDefault();
    e.stopPropagation();
    
    if (!groupId) return;
    
    try {
      if (post.isLiked) {
        await unlikePost(Number(groupId), post.postId);
        setAllPosts(posts => posts.map(p => 
          p.postId === post.postId 
            ? { ...p, isLiked: false, postLikes: Math.max(0, p.postLikes - 1) }
            : p
        ));
      } else {
        await likePost(Number(groupId), post.postId);
        setAllPosts(posts => posts.map(p => 
          p.postId === post.postId 
            ? { ...p, isLiked: true, postLikes: p.postLikes + 1 }
            : p
        ));
      }
    } catch (error) {
      console.error('좋아요 처리 실패:', error);
      toast.error('좋아요 처리에 실패했습니다.');
    }
  };

  // 더보기 버튼 클릭
  const handleLoadMore = () => {
    if (hasMore && !loading) {
      setPage(prev => prev + 1);
    }
  };

  if (loading && page === 0) {
    return (
      <div className="space-y-4 pb-20">
        {[1, 2, 3].map(i => (
          <Card key={i} className="animate-pulse">
            <CardContent className="p-4">
              <div className="h-4 bg-stone-200 rounded w-3/4 mb-2"></div>
              <div className="h-4 bg-stone-200 rounded w-1/2"></div>
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-4 pb-20">
      {/* 게시글 목록 */}
      {allPosts.map((post) => (
        <Card key={post.postId} className="overflow-hidden">
          <CardContent className="p-0">
            {/* 게시글 헤더 */}
            <div className="p-4 border-b border-stone-100">
              <div className="flex items-center gap-3">
                <Avatar className="h-10 w-10">
                  <AvatarImage src={undefined} />
                  <AvatarFallback className="bg-stone-100 text-stone-600">
                    {post.writerName.charAt(0)}
                  </AvatarFallback>
                </Avatar>
                <div className="flex-1 min-w-0">
                  <div className="font-medium text-stone-900">{post.writerName}</div>
                  <div className="text-sm text-stone-500">{formatDate(post.createdAt)}</div>
                </div>
              </div>
            </div>

            {/* 게시글 내용 */}
            <Link to={`/group/${groupId}/posts/${post.postId}`} className="block">
              <div className="p-4 space-y-3">
                {post.title && (
                  <h3 className="font-semibold text-stone-900 text-lg">{post.title}</h3>
                )}
                {post.content && (
                  <p className="text-stone-700 whitespace-pre-wrap line-clamp-3">
                    {post.content}
                  </p>
                )}
                {post.imagesUrl && post.imagesUrl.length > 0 && (
                  <div className="grid grid-cols-2 gap-2 mt-3">
                    {post.imagesUrl.slice(0, 4).map((img, idx) => (
                      <img
                        key={idx}
                        src={img}
                        alt={`${post.title || '게시글'} 이미지 ${idx + 1}`}
                        className="w-full h-32 object-cover rounded-lg"
                      />
                    ))}
                  </div>
                )}
              </div>
            </Link>

            {/* 투표 섹션 */}
            {(post.voteDetail && post.voteId !== undefined) ? (
              <div className="px-4 pb-4 border-t border-stone-100">
                <Link
                  to={`/group/${groupId}/votes/${post.voteId}`}
                  className="block mb-3 pt-3"
                  onClick={(e) => {
                    // 투표 옵션 클릭 시에는 Link 동작 방지
                    if ((e.target as HTMLElement).closest('.vote-option')) {
                      e.preventDefault();
                    }
                  }}
                >
                  <div className="flex items-center gap-2 mb-2">
                    <Badge variant="outline" className="text-xs">
                      투표
                    </Badge>
                    <span className="font-medium text-stone-900">{post.voteDetail.title}</span>
                    {post.voteDetail.deadline && (
                      <span className="text-xs text-stone-500 ml-auto flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {new Date(post.voteDetail.deadline).toLocaleDateString('ko-KR', {
                          month: 'short',
                          day: 'numeric'
                        })}
                      </span>
                    )}
                  </div>
                </Link>

                {/* 투표 옵션 (최대 4개) */}
                <div className="space-y-2">
                  {post.voteDetail.options.slice(0, 4).map((option) => {
                    const isSelected = post.mySelectedOptionIds?.includes(option.optionId) || false;
                    const voteCount = option.voteCount || 0;
                    const totalCount = post.totalVoteCount || 0;
                    const percentage = totalCount > 0 ? (voteCount / totalCount) * 100 : 0;

                    return (
                      <div
                        key={option.optionId}
                        className={`vote-option p-3 rounded-lg border-2 cursor-pointer transition-colors ${
                          isSelected
                            ? 'border-blue-500 bg-blue-50'
                            : 'border-stone-200 hover:border-stone-300'
                        }`}
                        onClick={(e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          handleVoteOptionToggle(post, option.optionId);
                        }}
                      >
                        <div className="flex items-center justify-between mb-1">
                          <span className={`text-sm font-medium ${isSelected ? 'text-blue-700' : 'text-stone-900'}`}>
                            {option.optionText}
                          </span>
                          <span className={`text-xs ${isSelected ? 'text-blue-600' : 'text-stone-500'}`}>
                            {voteCount}표 ({percentage.toFixed(0)}%)
                          </span>
                        </div>
                        {totalCount > 0 && (
                          <div className="h-1.5 bg-stone-200 rounded-full overflow-hidden">
                            <div
                              className={`h-full transition-all ${
                                isSelected ? 'bg-blue-500' : 'bg-stone-400'
                              }`}
                              style={{ width: `${percentage}%` }}
                            />
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* 4개 이상 옵션이 있으면 상세보기 링크 */}
                {post.voteDetail.options.length > 4 && post.voteId !== undefined && (
                  <Link
                    to={`/group/${groupId}/votes/${post.voteId}`}
                    className="mt-3 flex items-center justify-center gap-1 text-sm text-blue-600 hover:text-blue-700 font-medium"
                  >
                    더보기 ({post.voteDetail.options.length - 4}개 옵션 더)
                    <ChevronRight className="h-4 w-4" />
                  </Link>
                )}

                {/* 투표 통계 */}
                {post.totalVoteCount !== undefined && post.totalVoteCount > 0 ? (
                  <div className="mt-3 pt-3 border-t border-stone-100 flex items-center gap-2 text-xs text-stone-500">
                    <Users className="h-3 w-3" />
                    <span>총 {post.totalVoteCount}명 참여</span>
                  </div>
                ) : null}
              </div>
            ) : null}

            {/* 게시글 하단 (좋아요, 댓글) */}
            <div className="px-4 py-3 border-t border-stone-100 flex items-center gap-4 text-stone-500">
              <button 
                onClick={(e) => handlePostLike(e, post)}
                className="flex items-center gap-1 hover:text-red-500 transition-colors"
              >
                <Heart className={`h-4 w-4 ${post.isLiked ? 'fill-red-500 text-red-500' : ''}`} />
                <span className="text-sm">{post.postLikes}</span>
              </button>
              <Link to={`/group/${groupId}/posts/${post.postId}`} className="flex items-center gap-1 hover:text-stone-700">
                <MessageCircle className="h-4 w-4" />
                <span className="text-sm">{post.commentCount}</span>
              </Link>
            </div>
          </CardContent>
        </Card>
      ))}

      {/* 더보기 버튼 */}
      {hasMore && (
        <div className="flex justify-center pt-4">
          <Button
            variant="outline"
            onClick={handleLoadMore}
            disabled={loading}
          >
            {loading ? '불러오는 중...' : '더보기'}
          </Button>
        </div>
      )}

      {/* 게시글이 없을 때 */}
      {!loading && allPosts.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-stone-500">
          <Calendar className="h-12 w-12 mb-4 text-stone-300" />
          <p className="text-lg font-medium mb-1">아직 게시글이 없습니다</p>
          <p className="text-sm">첫 게시글을 작성해보세요!</p>
        </div>
      )}

      {/* FAB - 게시글 작성 */}
      <div className="fixed bottom-24 right-8 z-40">
        <Link to="create">
          <Button className="rounded-full h-12 px-6 shadow-lg bg-stone-900 hover:bg-stone-800 text-white flex items-center gap-2">
            <Plus className="w-5 h-5" />
            <span>글쓰기</span>
          </Button>
        </Link>
      </div>
    </div>
  );
}
