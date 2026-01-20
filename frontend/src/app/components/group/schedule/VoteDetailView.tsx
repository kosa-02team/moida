import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Clock, Users, Check, BarChart2, XCircle } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';
import { Badge } from '../../ui/badge';
import { Progress } from '../../ui/progress';
import { getVote, answerVote, closeVote, type VoteDetailResponse } from '../../../../api/vote';
import { useUserPermissions } from '../../../data/userRoles';
import { getMyInfo } from '../../../../api/user';

export function VoteDetailView() {
  const navigate = useNavigate();
  const { groupId, voteId } = useParams();
  const permissions = useUserPermissions(groupId || '1');
  const [vote, setVote] = useState<VoteDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedOptions, setSelectedOptions] = useState<number[]>([]);
  const [hasVoted, setHasVoted] = useState(false);
  const [isClosing, setIsClosing] = useState(false);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);

  useEffect(() => {
    async function fetchVote() {
      if (!groupId || !voteId) return;
      try {
        setLoading(true);
        
        // 현재 사용자 정보 조회
        let userId: number | null = null;
        try {
          const userInfo = await getMyInfo();
          userId = userInfo.userId;
          setCurrentUserId(userId);
        } catch (error) {
          console.error('사용자 정보 조회 실패:', error);
        }
        
        const voteData = await getVote(Number(groupId), Number(voteId));
        setVote(voteData);
        
        // 이미 투표했는지 확인
        if (userId) {
          const userHasVoted = voteData.options.some(option => 
            option.voters?.some(voter => voter.userId === userId)
          );
          setHasVoted(userHasVoted);
          
          // 이미 투표한 경우 선택된 옵션 설정
          if (userHasVoted) {
            const votedOptions = voteData.options
              .filter(option => option.voters?.some(voter => voter.userId === userId))
              .map(option => option.optionId);
            setSelectedOptions(votedOptions);
          }
        }
      } catch (error) {
        console.error('투표 정보 불러오기 실패:', error);
        toast.error('투표 정보를 불러오는데 실패했습니다.');
        navigate(-1);
      } finally {
        setLoading(false);
      }
    }
    fetchVote();
  }, [groupId, voteId, navigate]);

  if (loading || !vote) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  const deadline = vote.deadline ? new Date(vote.deadline) : null;
  const daysLeft = deadline ? Math.ceil((deadline.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24)) : null;
  const totalParticipants = vote.options.reduce((sum, opt) => sum + (opt.voteCount || 0), 0);
  const maxVotes = vote.options.length > 0 ? Math.max(...vote.options.map(o => o.voteCount || 0)) : 0;

  const toggleOption = (optionId: number) => {
    if (hasVoted) return;
    
    if (vote.allowMultiple) {
      setSelectedOptions(prev =>
        prev.includes(optionId)
          ? prev.filter(id => id !== optionId)
          : [...prev, optionId]
      );
    } else {
      setSelectedOptions([optionId]);
    }
  };

  const handleVote = async () => {
    if (selectedOptions.length === 0) {
      toast.error('최소 하나의 항목을 선택해주세요');
      return;
    }
    if (!groupId || !voteId) return;
    
    try {
      await answerVote(Number(groupId), Number(voteId), { optionIds: selectedOptions });
      setHasVoted(true);
      toast.success('투표가 완료되었습니다!');
      // 투표 후 데이터 다시 불러오기
      const voteData = await getVote(Number(groupId), Number(voteId));
      setVote(voteData);
    } catch (error) {
      console.error('투표 실패:', error);
      toast.error('투표에 실패했습니다.');
    }
  };

  const handleChangeVote = () => {
    setHasVoted(false);
    toast.info('투표를 수정할 수 있습니다');
  };

  const handleCloseVote = async () => {
    if (!groupId || !voteId) return;
    try {
      setIsClosing(true);
      await closeVote(Number(groupId), Number(voteId));
      toast.success('투표가 종료되었습니다');
      // 투표 데이터 다시 불러오기
      const voteData = await getVote(Number(groupId), Number(voteId));
      setVote(voteData);
    } catch (error) {
      console.error('투표 종료 실패:', error);
      toast.error('투표 종료에 실패했습니다.');
    } finally {
      setIsClosing(false);
    }
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-24">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigate(-1)}
              className="-ml-2"
            >
              <ArrowLeft className="w-6 h-6 text-stone-800" />
            </Button>
            <h1 className="ml-2 text-lg font-semibold text-stone-800">투표</h1>
          </div>
          {permissions.canManageGroup && vote.status === 'OPEN' && (
            <Button
              variant="outline"
              size="sm"
              onClick={handleCloseVote}
              disabled={isClosing}
              className="text-red-600 border-red-200 hover:bg-red-50"
            >
              <XCircle className="w-4 h-4 mr-1" />
              {isClosing ? '종료 중...' : '투표 종료'}
            </Button>
          )}
        </div>
      </header>

      <div className="p-5 space-y-5">
        {/* Title & Info */}
        <div className="bg-white rounded-2xl p-4 border border-stone-100">
          <div className="flex items-center gap-2 mb-3">
            <Badge className="bg-purple-100 text-purple-700">일정 투표</Badge>
            {vote.allowMultiple && (
              <Badge variant="outline" className="text-stone-500">복수 선택</Badge>
            )}
          </div>
          <h2 className="text-xl font-bold text-stone-900 mb-2">{vote.title}</h2>
          {vote.description && (
            <p className="text-sm text-stone-600 mb-4">{vote.description}</p>
          )}
          
          <div className="flex items-center justify-between text-sm">
            <div className="flex items-center gap-2">
              <Avatar className="w-6 h-6">
                <AvatarFallback className="text-xs">U</AvatarFallback>
              </Avatar>
              <span className="text-stone-500">작성자</span>
            </div>
            <div className="flex items-center gap-4 text-stone-500">
              <span className="flex items-center gap-1">
                <Users className="w-4 h-4" />
                {totalParticipants}명 참여
              </span>
              {daysLeft !== null && (
                <span className="flex items-center gap-1 text-orange-600">
                  <Clock className="w-4 h-4" />
                  {daysLeft > 0 ? `${daysLeft}일 남음` : '마감됨'}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Vote Options */}
        <div className="space-y-3">
          {vote.options.map(option => {
            const isSelected = selectedOptions.includes(option.optionId);
            const voteCount = option.voteCount || 0;
            const percentage = totalParticipants > 0 
              ? Math.round((voteCount / totalParticipants) * 100)
              : 0;
            const isLeading = voteCount === maxVotes && maxVotes > 0;
            const optionDate = option.eventDate ? new Date(option.eventDate) : null;

            return (
              <button
                key={option.optionId}
                onClick={() => toggleOption(option.optionId)}
                disabled={hasVoted || vote.status === 'CLOSED'}
                className={`w-full text-left bg-white rounded-2xl p-4 border-2 transition-all ${
                  isSelected
                    ? 'border-orange-500 bg-orange-50'
                    : 'border-stone-100 hover:border-stone-200'
                } ${hasVoted || vote.status === 'CLOSED' ? 'cursor-default' : ''}`}
              >
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <div className={`w-6 h-6 rounded-full border-2 flex items-center justify-center ${
                      isSelected
                        ? 'bg-orange-500 border-orange-500'
                        : 'border-stone-300'
                    }`}>
                      {isSelected && <Check className="w-4 h-4 text-white" />}
                    </div>
                    <div>
                      <p className="font-bold text-stone-900">
                        {optionDate 
                          ? optionDate.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })
                          : option.optionText}
                      </p>
                      {optionDate && (
                        <p className="text-sm text-stone-500">
                          {optionDate.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                        </p>
                      )}
                      {option.location && (
                        <p className="text-sm text-stone-500">{option.location}</p>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {isLeading && (
                      <Badge className="bg-green-100 text-green-700">
                        <BarChart2 className="w-3 h-3 mr-1" />
                        1위
                      </Badge>
                    )}
                    <span className="text-sm font-medium text-stone-600">
                      {voteCount}표
                    </span>
                  </div>
                </div>

                {/* Progress Bar */}
                <div className="mb-3">
                  <Progress 
                    value={percentage} 
                    className="h-2 bg-stone-100"
                  />
                </div>

                {/* Voters */}
                {!vote.isAnonymous && option.voters && option.voters.length > 0 && (
                  <div className="flex items-center gap-2">
                    <div className="flex -space-x-1.5">
                      {option.voters.slice(0, 5).map(voter => (
                        <Avatar key={voter.userId} className="w-6 h-6 border-2 border-white">
                          <AvatarImage src={voter.profileImageUrl} />
                          <AvatarFallback className="text-xs">{voter.realName[0]}</AvatarFallback>
                        </Avatar>
                      ))}
                    </div>
                    {option.voters.length > 5 && (
                      <span className="text-xs text-stone-500">
                        +{option.voters.length - 5}명
                      </span>
                    )}
                  </div>
                )}
              </button>
            );
          })}
        </div>

        {/* Vote Info */}
        <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
          <p className="text-sm text-blue-800">
            {deadline && `💡 마감일: ${deadline.toLocaleString('ko-KR')}`}
            <br />
            {vote.allowMultiple ? '가능한 항목을 모두 선택해주세요.' : '하나의 항목만 선택할 수 있습니다.'}
          </p>
        </div>
      </div>

      {/* Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-stone-100 p-4 safe-area-pb">
        <div className="max-w-md mx-auto">
          {hasVoted ? (
            <Button
              onClick={handleChangeVote}
              variant="outline"
              className="w-full h-12 rounded-xl"
            >
              투표 수정하기
            </Button>
          ) : (
            <Button
              onClick={handleVote}
              disabled={selectedOptions.length === 0 || vote.status === 'CLOSED'}
              className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white rounded-xl disabled:opacity-50"
            >
              {vote.status === 'CLOSED' ? '마감된 투표' : `투표하기 (${selectedOptions.length}개 선택)`}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}



