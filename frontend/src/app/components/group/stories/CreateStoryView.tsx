import { useState, useEffect, useRef, type CSSProperties } from 'react';
import { useParams } from 'react-router-dom';
import { createStory, type StoryCreateRequest } from '../../../../api/post';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Image, X, MapPin, Calendar, Users, Send, Vote, Plus } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Textarea } from '../../ui/textarea';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Avatar, AvatarFallback,  } from '../../ui/avatar';
import { Switch } from '../../ui/switch';
import { getMembers, type MemberListResponse } from '../../../../api/member';
import { getSchedules } from '../../../../api/schedule';
import { getMyInfo } from '../../../../api/user';
import { type VoteOptionCreateRequest } from '../../../../api/vote';

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
  
  // 카테고리 선택 (일반 게시글 / 투표)
  const [postCategory, setPostCategory] = useState<'story' | 'vote'>('story');
  
  // 투표 관련 상태
  const [voteTitle, setVoteTitle] = useState('');
  const [voteDescription, setVoteDescription] = useState('');
  const [voteDeadline, setVoteDeadline] = useState('');
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [allowMultiple, setAllowMultiple] = useState(false);
  const [voteOptions, setVoteOptions] = useState<VoteOptionCreateRequest[]>([
    { optionText: '', order: 1 }
  ]);
  const containerRef = useRef<HTMLDivElement | null>(null);

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

  // selectstart는 div 표준 prop이 아니므로 ref + addEventListener 사용
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const handler = (e: Event) => {
      const t = e.target as Node;
      if (!(t instanceof HTMLTextAreaElement || t instanceof HTMLInputElement)) {
        e.preventDefault();
      }
    };
    el.addEventListener('selectstart', handler);
    return () => el.removeEventListener('selectstart', handler);
  }, []);

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

    // 투표 게시글 생성 시
    if (postCategory === 'vote') {
      // 투표 유효성 검사
      if (!voteTitle.trim()) {
        toast.error('투표 제목을 입력해주세요');
        return;
      }
      if (voteOptions.filter(opt => opt.optionText.trim()).length < 2) {
        toast.error('투표 옵션은 최소 2개 이상 필요합니다');
        return;
      }
      if (voteDeadline && new Date(voteDeadline) <= new Date()) {
        toast.error('투표 마감일은 현재 시간보다 미래여야 합니다');
        return;
      }

      setIsSubmitting(true);
      try {
        // voteDeadline을 ISO 형식으로 변환 (datetime-local은 "YYYY-MM-DDTHH:mm" 형식)
        let formattedDeadline: string | null = null;
        if (voteDeadline && voteDeadline.trim()) {
          // datetime-local 형식을 ISO 형식으로 변환 (초 추가)
          const date = new Date(voteDeadline.trim());
          if (!isNaN(date.getTime())) {
            formattedDeadline = date.toISOString();
          }
        }

        const request: StoryCreateRequest = {
          title: voteTitle.trim(),
          content: voteDescription.trim() || null,
          voteOptions: voteOptions
            .filter(opt => opt.optionText.trim())
            .map((opt, idx) => ({
              optionText: opt.optionText.trim(),
              order: idx + 1,
              eventDate: opt.eventDate || null,
              location: opt.location || null
            })),
          voteDeadline: formattedDeadline,
          isAnonymous: isAnonymous || false,
          allowMultiple: allowMultiple || false,
        };
        await createStory(Number(groupId), request);
        toast.success('투표 게시글이 작성되었습니다');
        navigate(-1);
      } catch (error) {
        console.error('투표 게시글 작성 실패:', error);
        toast.error('투표 게시글 작성에 실패했습니다');
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
    <div 
      ref={containerRef}
      className="min-h-screen bg-white pb-24 select-none" 
      onDragStart={(e) => {
        // 입력 필드가 아닌 경우에만 드래그 방지
        if (!(e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLInputElement)) {
          e.preventDefault();
          e.dataTransfer.effectAllowed = 'none';
          const canvas = document.createElement('canvas');
          canvas.width = 1;
          canvas.height = 1;
          e.dataTransfer.setDragImage(canvas, 0, 0);
        }
      }} 
      onDragOver={(e) => {
        if (!(e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLInputElement)) {
          e.preventDefault();
          e.dataTransfer.dropEffect = 'none';
        }
      }}
      onDragEnd={(e) => {
        if (!(e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLInputElement)) {
          e.preventDefault();
        }
      }}
      style={{ 
        userSelect: 'none',
        WebkitUserSelect: 'none',
        WebkitUserDrag: 'none',
        backgroundColor: '#ffffff'
      } as CSSProperties}
    >
      {/* Header */}
      <header className="sticky top-0 z-[70] bg-white shadow-sm" style={{ backgroundColor: '#ffffff' }}>
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
              (postCategory === 'vote'
                ? (!voteTitle.trim() || voteOptions.filter(opt => opt.optionText.trim()).length < 2)
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

      <div className="p-5 space-y-6 pt-6" style={{ backgroundColor: '#ffffff' }}>
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

        {/* 카테고리 선택 */}
        <div className="space-y-2">
          <Label className="text-base font-medium text-stone-700">게시글 유형</Label>
          <div className="flex gap-2">
            <button
              onClick={() => setPostCategory('story')}
              className={`flex-1 px-4 py-3 rounded-xl border-2 transition-colors ${
                postCategory === 'story'
                  ? 'border-orange-500 bg-orange-50 text-orange-700'
                  : 'border-stone-200 bg-white text-stone-600 hover:border-stone-300'
              }`}
            >
              <div className="flex items-center justify-center gap-2">
                <span className="text-sm font-medium">일반 게시글</span>
              </div>
            </button>
            <button
              onClick={() => setPostCategory('vote')}
              className={`flex-1 px-4 py-3 rounded-xl border-2 transition-colors ${
                postCategory === 'vote'
                  ? 'border-orange-500 bg-orange-50 text-orange-700'
                  : 'border-stone-200 bg-white text-stone-600 hover:border-stone-300'
              }`}
            >
              <div className="flex items-center justify-center gap-2">
                <Vote className="w-4 h-4" />
                <span className="text-sm font-medium">투표</span>
              </div>
            </button>
          </div>
        </div>

        {/* 투표 게시글 UI */}
        {postCategory === 'vote' ? (
          <>
            {/* 투표 제목 */}
            <div className="space-y-2">
              <Label htmlFor="voteTitle" className="text-base font-medium text-stone-700">
                투표 제목 <span className="text-red-500">*</span>
              </Label>
              <Input
                id="voteTitle"
                placeholder="투표 제목을 입력하세요"
                className="h-12 bg-white border-stone-200 focus-visible:ring-orange-500 rounded-xl"
                value={voteTitle}
                onChange={(e) => setVoteTitle(e.target.value)}
                maxLength={200}
              />
              <p className="text-xs text-stone-400 text-right">{voteTitle.length}/200</p>
            </div>

            {/* 투표 설명 */}
            <div className="space-y-2">
              <Label htmlFor="voteDescription" className="text-base font-medium text-stone-700">
                투표 설명 (선택)
              </Label>
              <Textarea
                id="voteDescription"
                placeholder="투표에 대한 설명을 입력하세요"
                className="min-h-24 bg-white border-stone-200 focus-visible:ring-orange-500 rounded-xl resize-none"
                value={voteDescription}
                onChange={(e) => setVoteDescription(e.target.value)}
                maxLength={500}
              />
              <p className="text-xs text-stone-400 text-right">{voteDescription.length}/500</p>
            </div>

            {/* 투표 옵션 */}
            <div className="space-y-2">
              <Label className="text-base font-medium text-stone-700">
                투표 옵션 <span className="text-red-500">*</span> (최소 2개)
              </Label>
              {voteOptions.map((option, index) => (
                <div key={index} className="flex gap-2">
                  <Input
                    placeholder={`옵션 ${index + 1}`}
                    className="flex-1 h-12 bg-white border-stone-200 focus-visible:ring-orange-500 rounded-xl"
                    value={option.optionText}
                    onChange={(e) => updateVoteOption(index, e.target.value)}
                  />
                  {voteOptions.length > 2 && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => removeVoteOption(index)}
                      className="h-12 w-12 text-stone-400 hover:text-red-500"
                    >
                      <X className="w-5 h-5" />
                    </Button>
                  )}
                </div>
              ))}
              {voteOptions.length < 10 && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={addVoteOption}
                  className="w-full h-12 border-stone-200 text-stone-600 hover:bg-stone-50 rounded-xl"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  옵션 추가
                </Button>
              )}
            </div>

            {/* 투표 설정 */}
            <div className="space-y-4 bg-stone-50 rounded-xl p-4">
              <div className="flex items-center justify-between">
                <div>
                  <Label className="text-base font-medium text-stone-700">익명 투표</Label>
                  <p className="text-xs text-stone-500 mt-1">투표자가 익명으로 표시됩니다</p>
                </div>
                <Switch
                  checked={isAnonymous}
                  onCheckedChange={setIsAnonymous}
                  className="data-[state=checked]:bg-orange-500"
                />
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <Label className="text-base font-medium text-stone-700">복수 선택 허용</Label>
                  <p className="text-xs text-stone-500 mt-1">여러 옵션을 선택할 수 있습니다</p>
                </div>
                <Switch
                  checked={allowMultiple}
                  onCheckedChange={setAllowMultiple}
                  className="data-[state=checked]:bg-orange-500"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="voteDeadline" className="text-base font-medium text-stone-700">
                  마감일 (선택)
                </Label>
                <Input
                  id="voteDeadline"
                  type="datetime-local"
                  className="h-12 bg-white border-stone-200 focus-visible:ring-orange-500 rounded-xl"
                  value={voteDeadline}
                  onChange={(e) => setVoteDeadline(e.target.value)}
                />
              </div>
            </div>
          </>
        ) : (
          <>
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
              className="min-h-32 border-none shadow-none text-base resize-none focus-visible:ring-0 p-0 select-text"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              onDragStart={(e) => e.stopPropagation()}
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
                className="h-11 bg-stone-50 border-stone-200 rounded-xl select-text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                onDragStart={(e) => e.stopPropagation()}
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
          </>
        )}
      </div>
    </div>
  );
}
