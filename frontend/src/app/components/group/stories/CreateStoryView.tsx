import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { createStory, type StoryCreateRequest, type PostIdResponse } from '../../../../api/post';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Image, X, MapPin, Calendar, Users, Send, Vote, Plus } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Textarea } from '../../ui/textarea';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Badge } from '../../ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';
import { Switch } from '../../ui/switch';
import { getMembers, type MemberListResponse } from '../../../../api/member';
import { getSchedules } from '../../../../api/schedule';
import { getMyInfo } from '../../../../api/user';
import { createVote, type VoteCreateRequest, type VoteOptionCreateRequest } from '../../../../api/vote';

export function CreateStoryView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const [content, setContent] = useState('');
  const [images, setImages] = useState<string[]>([]);
  const [location, setLocation] = useState('');
  const [taggedMembers, setTaggedMembers] = useState<number[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [linkedScheduleId, setLinkedScheduleId] = useState<number | null>(null);
  const [members, setMembers] = useState<MemberListResponse[]>([]);
  const [schedules, setSchedules] = useState<Array<{ scheduleId: number; scheduleName: string; eventDate: string }>>([]);
  const [userInfo, setUserInfo] = useState<{ realName: string; loginId: string } | null>(null);
  const [loading, setLoading] = useState(true);
  
  // 일반 투표 관련 상태
  const [createGeneralVote, setCreateGeneralVote] = useState(false);
  const [voteTitle, setVoteTitle] = useState('');
  const [voteDescription, setVoteDescription] = useState('');
  const [voteDeadline, setVoteDeadline] = useState('');
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [allowMultiple, setAllowMultiple] = useState(false);
  const [voteOptions, setVoteOptions] = useState<VoteOptionCreateRequest[]>([
    { optionText: '', order: 1 }
  ]);

  useEffect(() => {
    async function fetchData() {
      if (!groupId) return;
      try {
        setLoading(true);
        const membersData = await getMembers(Number(groupId), 'ACTIVE');
        const schedulesData = await getSchedules(Number(groupId));
        const myInfo = await getMyInfo();
        setMembers(membersData);
        setSchedules(schedulesData.map(s => ({
          scheduleId: s.scheduleId,
          scheduleName: s.scheduleName,
          eventDate: s.eventDate
        })));
        setUserInfo({ realName: myInfo.realName, loginId: myInfo.loginId });
      } catch (error) {
        console.error('데이터 조회 실패:', error);
        toast.error('데이터를 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [groupId]);

  const handleImageUpload = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.multiple = true;
    input.onchange = (e) => {
      const files = (e.target as HTMLInputElement).files;
      if (!files) return;
      
      const fileArray = Array.from(files);
      if (images.length + fileArray.length > 9) {
        toast.error('최대 9장까지 추가할 수 있습니다');
        return;
      }
      
      fileArray.forEach((file) => {
        if (!file.type.startsWith('image/')) {
          toast.error('이미지 파일만 업로드 가능합니다');
          return;
        }
        
        const reader = new FileReader();
        reader.onload = (event) => {
          const result = event.target?.result as string;
          if (result) {
            setImages(prev => [...prev, result]);
          }
        };
        reader.onerror = () => {
          toast.error('이미지 읽기에 실패했습니다');
        };
        reader.readAsDataURL(file);
      });
    };
    input.click();
  };

  const removeImage = (index: number) => {
    setImages(images.filter((_, i) => i !== index));
  };

  const toggleMember = (memberId: number) => {
    setTaggedMembers(prev =>
      prev.includes(memberId)
        ? prev.filter(id => id !== memberId)
        : [...prev, memberId]
    );
  };

  const addVoteOption = () => {
    if (voteOptions.length >= 10) {
      toast.error('투표 옵션은 최대 10개까지 추가할 수 있습니다');
      return;
    }
    setVoteOptions([...voteOptions, { optionText: '', order: voteOptions.length + 1 }]);
  };

  const removeVoteOption = (index: number) => {
    if (voteOptions.length <= 1) {
      toast.error('투표 옵션은 최소 1개 이상 필요합니다');
      return;
    }
    const newOptions = voteOptions.filter((_, i) => i !== index).map((opt, i) => ({
      ...opt,
      order: i + 1
    }));
    setVoteOptions(newOptions);
  };

  const updateVoteOption = (index: number, text: string) => {
    const newOptions = [...voteOptions];
    newOptions[index] = { ...newOptions[index], optionText: text };
    setVoteOptions(newOptions);
  };

  const handleSubmit = async () => {
    if (!groupId) {
      toast.error('모임 ID를 찾을 수 없습니다');
      return;
    }

    // 일반 투표 생성 시
    if (createGeneralVote) {
      // 투표 유효성 검사
      if (!voteTitle.trim()) {
        toast.error('투표 제목을 입력해주세요');
        return;
      }
      if (voteOptions.some(opt => !opt.optionText.trim())) {
        toast.error('모든 투표 옵션을 입력해주세요');
        return;
      }
      if (voteDeadline && new Date(voteDeadline) <= new Date()) {
        toast.error('투표 마감일은 현재 시간보다 미래여야 합니다');
        return;
      }

      // 일반 투표만 생성 (백엔드가 자동으로 게시글 생성)
      setIsSubmitting(true);
      try {
        const voteRequest: VoteCreateRequest = {
          voteType: 'GENERAL',
          title: voteTitle.trim(),
          description: voteDescription.trim() || undefined,
          isAnonymous,
          allowMultiple,
          deadline: voteDeadline || undefined,
          options: voteOptions.map(opt => ({
            optionText: opt.optionText.trim(),
            order: opt.order
          }))
        };
        await createVote(Number(groupId), voteRequest);
        toast.success('일반 투표가 작성되었습니다');
        navigate(-1);
      } catch (error) {
        console.error('투표 작성 실패:', error);
        toast.error('투표 작성에 실패했습니다');
      } finally {
        setIsSubmitting(false);
      }
      return;
    }

    // 일반 게시글 생성 시
    if (!content.trim() && images.length === 0) {
      toast.error('내용 또는 이미지를 입력해주세요');
      return;
    }

    setIsSubmitting(true);
    try {
      const request: StoryCreateRequest = {
        content: content.trim(),
        imagesUrl: images.length > 0 ? images : undefined,
        scheduleId: linkedScheduleId || null,
        place: location.trim() || null,
        taggedMemberIds: taggedMembers.length > 0 ? taggedMembers : undefined,
      };
      await createStory(Number(groupId), request);
      toast.success('게시글이 작성되었습니다');
      navigate(-1);
    } catch (error) {
      console.error('게시글 작성 실패:', error);
      toast.error('게시글 작성에 실패했습니다');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-white pb-24 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-white pb-24" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* Header */}
      <header className="sticky top-[145px] z-[70] bg-white shadow-sm">
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
            <h1 className="ml-2 text-lg font-semibold text-stone-800">게시글 작성</h1>
          </div>
          <Button
            onClick={handleSubmit}
            disabled={
              isSubmitting || 
              (createGeneralVote 
                ? (!voteTitle.trim() || voteOptions.some(opt => !opt.optionText.trim()))
                : (!content.trim() && images.length === 0)
              )
            }
            className="bg-orange-500 hover:bg-orange-600 text-white rounded-full px-4"
          >
            <>
              {isSubmitting ? (
                <span>작성 중...</span>
              ) : (
                <>
                  <Send className="w-4 h-4 mr-1" />
                  게시
                </>
              )}
            </>
          </Button>
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Author Info */}
        {userInfo && (
          <div className="flex items-center gap-3">
            <Avatar className="w-12 h-12" draggable={false}>
              <AvatarFallback>{userInfo.realName[0]}</AvatarFallback>
            </Avatar>
            <div>
              <p className="font-medium text-stone-900">{userInfo.realName}</p>
              <p className="text-xs text-stone-500">{userInfo.loginId}</p>
            </div>
          </div>
        )}

        {/* Linked Event */}
        {linkedScheduleId && (
          <div className="flex items-center gap-2 p-3 bg-orange-50 rounded-xl">
            <Calendar className="w-4 h-4 text-orange-600" />
            <span className="text-sm text-orange-700">
              {schedules.find(s => s.scheduleId === linkedScheduleId)?.scheduleName || '일정'}
            </span>
            <button
              onClick={() => setLinkedScheduleId(null)}
              className="ml-auto"
            >
              <X className="w-4 h-4 text-orange-600" />
            </button>
          </div>
        )}

        {/* 일정 선택 */}
        {schedules.length > 0 && !linkedScheduleId && (
          <div className="space-y-2">
            <Label className="flex items-center gap-2 text-stone-700">
              <Calendar className="w-4 h-4" />
              일정 연결 (선택)
            </Label>
            <div className="flex flex-wrap gap-2">
              {schedules.slice(0, 5).map(schedule => (
                <button
                  key={schedule.scheduleId}
                  onClick={() => setLinkedScheduleId(schedule.scheduleId)}
                  className="px-3 py-1.5 rounded-full text-sm bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors"
                >
                  {schedule.scheduleName}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Content Input */}
        <Textarea
          placeholder="게시글 내용을 입력하세요..."
          className="min-h-32 border-none shadow-none text-base resize-none focus-visible:ring-0 p-0"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          maxLength={500}
        />
        <p className="text-xs text-stone-400 text-right">{content.length}/500</p>

        {/* Images */}
        {images.length > 0 && (
          <div className="grid grid-cols-3 gap-2">
            {images.map((img, index) => (
              <div key={index} className="relative aspect-square rounded-xl overflow-hidden bg-stone-100">
                <img src={img} alt="" className="w-full h-full object-cover" draggable={false} onDragStart={(e) => e.preventDefault()} />
                <button
                  onClick={() => removeImage(index)}
                  className="absolute top-1 right-1 w-6 h-6 bg-black/50 rounded-full flex items-center justify-center"
                >
                  <X className="w-4 h-4 text-white" />
                </button>
              </div>
            ))}
            {images.length < 9 && (
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
        {images.length === 0 && (
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
          <Label className="flex items-center gap-2 text-stone-700">
            <MapPin className="w-4 h-4" />
            위치
          </Label>
          <Input
            placeholder="위치를 입력하세요"
            className="h-11 bg-stone-50 border-stone-200 rounded-xl"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
          />
        </div>

        {/* Tag Members */}
        {members.length > 0 && (
          <div className="space-y-3">
            <Label className="flex items-center gap-2 text-stone-700">
              <Users className="w-4 h-4" />
              멤버 태그 (선택)
            </Label>
            <div className="flex flex-wrap gap-2">
              {members.map(member => {
                const isSelected = taggedMembers.includes(member.memberId);
                return (
                  <button
                    key={member.memberId}
                    onClick={() => toggleMember(member.memberId)}
                    className={`px-3 py-1.5 rounded-full text-sm transition-colors ${
                      isSelected
                        ? 'bg-orange-500 text-white'
                        : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                    }`}
                  >
                    @{member.clubNickname || member.realName}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Selected Tags Preview */}
        {taggedMembers.length > 0 && (
          <div className="bg-stone-50 rounded-xl p-4">
            <p className="text-sm text-stone-600">
              {taggedMembers.map((memberId, i) => {
                const member = members.find(m => m.memberId === memberId);
                return (
                  <span key={memberId}>
                    <span className="text-orange-600">@{member?.clubNickname || member?.realName || '멤버'}</span>
                    {i < taggedMembers.length - 1 && ', '}
                  </span>
                );
              })}
              님을 태그했습니다
            </p>
          </div>
        )}

        {/* 일반 투표 생성 옵션 */}
        <div className="space-y-4 pt-4 border-t border-stone-200">
          <div className="flex items-center justify-between">
            <Label className="flex items-center gap-2 text-stone-700 cursor-pointer">
              <Vote className="w-4 h-4" />
              일반 투표 함께 생성 (선택)
            </Label>
            <Switch
              checked={createGeneralVote}
              onCheckedChange={setCreateGeneralVote}
              className="data-[state=checked]:bg-orange-500"
            />
          </div>

          {createGeneralVote && (
            <div className="space-y-4 p-4 bg-orange-50 rounded-xl border border-orange-100">
              <div className="space-y-2">
                <Label className="text-sm text-stone-700">투표 제목 *</Label>
                <Input
                  placeholder="투표 제목을 입력하세요"
                  className="h-11 bg-white border-stone-200 rounded-xl"
                  value={voteTitle}
                  onChange={(e) => setVoteTitle(e.target.value)}
                  maxLength={100}
                />
              </div>

              <div className="space-y-2">
                <Label className="text-sm text-stone-700">투표 설명 (선택)</Label>
                <Textarea
                  placeholder="투표에 대한 설명을 입력하세요"
                  className="bg-white border-stone-200 rounded-xl resize-none min-h-[80px]"
                  value={voteDescription}
                  onChange={(e) => setVoteDescription(e.target.value)}
                  maxLength={500}
                />
              </div>

              <div className="space-y-2">
                <Label className="text-sm text-stone-700">투표 마감일 (선택)</Label>
                <Input
                  type="datetime-local"
                  className="h-11 bg-white border-stone-200 rounded-xl [&::-webkit-calendar-picker-indicator]:opacity-100 [&::-webkit-calendar-picker-indicator]:cursor-pointer"
                  value={voteDeadline}
                  onChange={(e) => setVoteDeadline(e.target.value)}
                  placeholder="투표 마감일 선택"
                />
              </div>

              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <Label className="text-sm text-stone-700">투표 옵션 *</Label>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={addVoteOption}
                    className="h-8 text-xs"
                  >
                    <Plus className="w-3 h-3 mr-1" />
                    옵션 추가
                  </Button>
                </div>
                {voteOptions.map((option, index) => (
                  <div key={index} className="flex items-center gap-2">
                    <Input
                      placeholder={`옵션 ${index + 1}`}
                      className="flex-1 h-10 bg-white border-stone-200 rounded-xl"
                      value={option.optionText}
                      onChange={(e) => updateVoteOption(index, e.target.value)}
                      maxLength={100}
                    />
                    {voteOptions.length > 1 && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={() => removeVoteOption(index)}
                        className="h-10 w-10 text-stone-400 hover:text-red-500"
                      >
                        <X className="w-4 h-4" />
                      </Button>
                    )}
                  </div>
                ))}
              </div>

              <div className="space-y-3 pt-2">
                <div className="flex items-center justify-between">
                  <Label className="text-sm text-stone-700">익명 투표</Label>
                  <Switch
                    checked={isAnonymous}
                    onCheckedChange={setIsAnonymous}
                    className="data-[state=checked]:bg-orange-500"
                  />
                </div>
                <div className="flex items-center justify-between">
                  <Label className="text-sm text-stone-700">중복 선택 허용</Label>
                  <Switch
                    checked={allowMultiple}
                    onCheckedChange={setAllowMultiple}
                    className="data-[state=checked]:bg-orange-500"
                  />
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
