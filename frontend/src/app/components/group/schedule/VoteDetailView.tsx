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
  const [hasPendingChanges, setHasPendingChanges] = useState(false); // 사용자가 아직 제출하지 않은 변경사항 추적

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
    
    // 실시간 업데이트를 위한 인터벌 (5초마다)
    const interval = setInterval(async () => {
      if (groupId && voteId) {
        try {
          const voteData = await getVote(Number(groupId), Number(voteId));
          setVote(voteData);
          
          // 현재 사용자의 투표 상태 업데이트
          // 단, 사용자가 선택 중인 경우(hasPendingChanges)에는 selectedOptions를 덮어쓰지 않음
          if (currentUserId) {
            const userHasVoted = voteData.options.some(option => 
              option.voters?.some(voter => voter.userId === currentUserId)
            );
            setHasVoted(userHasVoted);
            
            // 사용자가 아직 제출하지 않은 변경사항이 없을 때만 서버 데이터로 selectedOptions 업데이트
            // hasPendingChanges 상태는 함수 외부에서 확인할 수 없으므로 함수형 업데이트 사용 안함
            // 대신 이 부분은 건너뛰고, handleVote 성공 후에만 업데이트하도록 변경
          }
        } catch (error) {
          // 에러 발생 시 조용히 무시 (투표가 삭제되었거나 권한이 없는 경우)
          console.error('투표 정보 갱신 실패:', error);
        }
      }
    }, 5000);
    
    return () => clearInterval(interval);
  }, [groupId, voteId, navigate, currentUserId]);

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
    // 이미 투표한 경우에도 수정 가능하도록 변경
    if (vote.status === 'CLOSED') return;
    
    // 사용자가 선택을 변경했음을 표시 (폴링에서 덮어쓰지 않도록)
    setHasPendingChanges(true);
    
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
      setHasPendingChanges(false); // 제출 완료 후 pending 상태 해제
      toast.success(hasVoted ? '투표가 수정되었습니다!' : '투표가 완료되었습니다!');
      // 투표 후 데이터 다시 불러오기
      const voteData = await getVote(Number(groupId), Number(voteId));
      setVote(voteData);
      
      // 수정 후 다시 선택된 옵션 설정
      if (currentUserId) {
        const votedOptions = voteData.options
          .filter(option => option.voters?.some(voter => voter.userId === currentUserId))
          .map(option => option.optionId);
        setSelectedOptions(votedOptions);
      }
    } catch (error: any) {
      console.error('투표 실패:', error);
      // 409 에러는 이미 선택한 옵션이므로 다른 메시지 표시
      if (error.message?.includes('이미 선택한 옵션')) {
        toast.error('이미 선택한 옵션입니다. 다른 옵션을 선택해주세요.');
      } else {
        toast.error('투표에 실패했습니다.');
      }
    }
  };

  const handleChangeVote = () => {
    // 기존 선택 유지하고 수정 모드로 전환
    setHasVoted(false);
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
                disabled={vote.status === 'CLOSED'}
                className={`w-full text-left bg-white rounded-2xl p-4 border-2 transition-all ${
                  isSelected
                    ? 'border-orange-500 bg-orange-50'
                    : 'border-stone-100 hover:border-stone-200'
                } ${vote.status === 'CLOSED' ? 'cursor-default opacity-60' : 'cursor-pointer'}`}
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
          <Button
            onClick={handleVote}
            disabled={selectedOptions.length === 0 || vote.status === 'CLOSED'}
            className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white rounded-xl disabled:opacity-50"
          >
            {vote.status === 'CLOSED' 
              ? '마감된 투표' 
              : hasVoted 
                ? `투표 수정하기 (${selectedOptions.length}개 선택)`
                : `투표하기 (${selectedOptions.length}개 선택)`}
          </Button>
        </div>
      </div>
    </div>
  );
}



