import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Plus, Trash2, Calendar, MapPin, HelpCircle, Info } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Textarea } from '../../ui/textarea';
import { RadioGroup, RadioGroupItem } from '../../ui/radio-group';
import { Switch } from '../../ui/switch';
import { Card, CardContent } from '../../ui/card';
import { createVote, type VoteCreateRequest } from '../../../../api/vote';

type VoteType = 'schedule' | 'location' | 'both';

interface DateOption {
  id: string;
  datetime: string;
}

interface LocationOption {
  id: string;
  name: string;
  address: string;
}

export function VoteCreateView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [voteType, setVoteType] = useState<VoteType>('schedule');
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // 일정 옵션
  const [dateOptions, setDateOptions] = useState<DateOption[]>([
    { id: '1', datetime: '' }
  ]);
  
  // 장소 옵션
  const [locationOptions, setLocationOptions] = useState<LocationOption[]>([
    { id: '1', name: '', address: '' }
  ]);
  
  const [deadline, setDeadline] = useState('');
  const [allowMultiple, setAllowMultiple] = useState(true);
  const [isAnonymous, setIsAnonymous] = useState(false);

  // 일정 옵션 관리
  const addDateOption = () => {
    setDateOptions([...dateOptions, { id: Date.now().toString(), datetime: '' }]);
  };
  
  const removeDateOption = (id: string) => {
    if (dateOptions.length > 1) {
      setDateOptions(dateOptions.filter(d => d.id !== id));
    }
  };
  
  const updateDateOption = (id: string, datetime: string) => {
    setDateOptions(dateOptions.map(d => d.id === id ? { ...d, datetime } : d));
  };

  // 장소 옵션 관리
  const addLocationOption = () => {
    setLocationOptions([...locationOptions, { id: Date.now().toString(), name: '', address: '' }]);
  };
  
  const removeLocationOption = (id: string) => {
    if (locationOptions.length > 1) {
      setLocationOptions(locationOptions.filter(l => l.id !== id));
    }
  };
  
  const updateLocationOption = (id: string, field: 'name' | 'address', value: string) => {
    setLocationOptions(locationOptions.map(l => l.id === id ? { ...l, [field]: value } : l));
  };

  const handleSubmit = () => {
    if (!title.trim()) {
      toast.error('투표 제목을 입력해주세요');
      return;
    }

    if ((voteType === 'schedule' || voteType === 'both') && dateOptions.every(d => !d.datetime)) {
      toast.error('최소 1개 이상의 일정 후보를 입력해주세요');
      return;
    }

    if ((voteType === 'location' || voteType === 'both') && locationOptions.every(l => !l.name)) {
      toast.error('최소 1개 이상의 장소 후보를 입력해주세요');
      return;
    }

    toast.success('투표가 생성되었습니다');
    navigate('..');
  };

  return (
    <div className="bg-white min-h-screen pb-20">
      <header className="flex items-center p-4 border-b border-stone-100 sticky top-0 bg-white z-10">
        <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
          <ArrowLeft className="w-5 h-5 text-stone-600" />
        </Button>
        <h1 className="text-lg font-semibold ml-2 text-stone-800">투표 만들기</h1>
      </header>

      <div className="p-6 space-y-8">
        {/* 기본 정보 */}
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="title" className="text-base font-medium">
              투표 제목 <span className="text-red-500">*</span>
            </Label>
            <Input 
              id="title" 
              placeholder="예: 5월 정기모임 날짜 정하기" 
              className="h-12 bg-stone-50 border-stone-200 rounded-xl"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="description" className="text-base font-medium">설명 (선택)</Label>
            <Textarea
              id="description"
              placeholder="투표에 대한 추가 설명을 입력하세요"
              className="bg-stone-50 border-stone-200 rounded-xl resize-none"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
        </div>

        {/* 투표 유형 선택 */}
        <div className="space-y-4">
          <Label className="text-base font-medium">투표 유형</Label>
          <RadioGroup value={voteType} onValueChange={(v) => setVoteType(v as VoteType)} className="grid grid-cols-1 gap-3">
            <Card className={`border-2 transition-all cursor-pointer ${voteType === 'schedule' ? 'border-orange-500 bg-orange-50' : 'border-stone-200'}`}>
              <CardContent className="p-4 flex items-center gap-3">
                <RadioGroupItem value="schedule" id="schedule" />
                <Label htmlFor="schedule" className="flex-1 cursor-pointer">
                  <div className="flex items-center gap-2">
                    <Calendar className="w-5 h-5 text-orange-600" />
                    <span className="font-medium">일정 투표</span>
                  </div>
                  <p className="text-sm text-stone-500 mt-1">날짜/시간을 선택합니다</p>
                </Label>
              </CardContent>
            </Card>
            
            <Card className={`border-2 transition-all cursor-pointer ${voteType === 'location' ? 'border-blue-500 bg-blue-50' : 'border-stone-200'}`}>
              <CardContent className="p-4 flex items-center gap-3">
                <RadioGroupItem value="location" id="location" />
                <Label htmlFor="location" className="flex-1 cursor-pointer">
                  <div className="flex items-center gap-2">
                    <MapPin className="w-5 h-5 text-blue-600" />
                    <span className="font-medium">장소 투표</span>
                  </div>
                  <p className="text-sm text-stone-500 mt-1">장소를 선택합니다</p>
                </Label>
              </CardContent>
            </Card>

            <Card className={`border-2 transition-all cursor-pointer ${voteType === 'both' ? 'border-purple-500 bg-purple-50' : 'border-stone-200'}`}>
              <CardContent className="p-4 flex items-center gap-3">
                <RadioGroupItem value="both" id="both" />
                <Label htmlFor="both" className="flex-1 cursor-pointer">
                  <div className="flex items-center gap-2">
                    <HelpCircle className="w-5 h-5 text-purple-600" />
                    <span className="font-medium">일정 + 장소</span>
                  </div>
                  <p className="text-sm text-stone-500 mt-1">날짜와 장소를 함께 투표합니다</p>
                </Label>
              </CardContent>
            </Card>
          </RadioGroup>
        </div>

        {/* 일정 옵션 (일정 또는 둘 다 선택 시) */}
        {(voteType === 'schedule' || voteType === 'both') && (
          <div className="space-y-3">
            <Label className="text-base font-medium flex items-center gap-2">
              <Calendar className="w-4 h-4 text-orange-600" />
              후보 날짜/시간
            </Label>
            {dateOptions.map((option, index) => (
              <div key={option.id} className="flex gap-2">
                <Input 
                  type="datetime-local" 
                  value={option.datetime} 
                  onChange={(e) => updateDateOption(option.id, e.target.value)}
                  className="h-12 bg-stone-50 border-stone-200 rounded-xl flex-1"
                />
                {dateOptions.length > 1 && (
                  <Button 
                    variant="ghost" 
                    size="icon" 
                    onClick={() => removeDateOption(option.id)} 
                    className="text-red-400 hover:text-red-500 hover:bg-red-50"
                  >
                    <Trash2 className="w-5 h-5" />
                  </Button>
                )}
              </div>
            ))}
            <Button 
              variant="outline" 
              onClick={addDateOption} 
              className="w-full border-dashed border-orange-300 text-orange-600 hover:bg-orange-50 rounded-xl h-12"
            >
              <Plus className="w-4 h-4 mr-2" />
              일정 후보 추가
            </Button>
          </div>
        )}

        {/* 장소 옵션 (장소 또는 둘 다 선택 시) */}
        {(voteType === 'location' || voteType === 'both') && (
          <div className="space-y-3">
            <Label className="text-base font-medium flex items-center gap-2">
              <MapPin className="w-4 h-4 text-blue-600" />
              후보 장소
            </Label>
            {locationOptions.map((option, index) => (
              <Card key={option.id} className="border-stone-200">
                <CardContent className="p-3 space-y-2">
                  <div className="flex gap-2">
                    <Input 
                      placeholder="장소 이름 (예: 강남역 스타벅스)"
                      value={option.name} 
                      onChange={(e) => updateLocationOption(option.id, 'name', e.target.value)}
                      className="h-10 bg-stone-50 border-stone-200 rounded-lg flex-1"
                    />
                    {locationOptions.length > 1 && (
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        onClick={() => removeLocationOption(option.id)} 
                        className="text-red-400 hover:text-red-500 hover:bg-red-50 h-10 w-10"
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    )}
                  </div>
                  <Input 
                    placeholder="주소 또는 상세 설명 (선택)"
                    value={option.address} 
                    onChange={(e) => updateLocationOption(option.id, 'address', e.target.value)}
                    className="h-10 bg-stone-50 border-stone-200 rounded-lg"
                  />
                </CardContent>
              </Card>
            ))}
            <Button 
              variant="outline" 
              onClick={addLocationOption} 
              className="w-full border-dashed border-blue-300 text-blue-600 hover:bg-blue-50 rounded-xl h-12"
            >
              <Plus className="w-4 h-4 mr-2" />
              장소 후보 추가
            </Button>
          </div>
        )}

        {/* 장소+일정 안내 */}
        {voteType === 'both' && (
          <Card className="border-purple-200 bg-purple-50">
            <CardContent className="p-4 flex gap-3">
              <Info className="w-5 h-5 text-purple-600 shrink-0" />
              <p className="text-sm text-purple-700">
                일정과 장소를 각각 투표받은 뒤, 가장 많은 표를 받은 조합으로 결정됩니다.
                장소에 따라 참여 가능 여부가 달라질 수 있으므로 함께 투표하면 좋습니다.
              </p>
            </CardContent>
          </Card>
        )}

        {/* 투표 마감일 */}
        <div className="space-y-2">
          <Label className="text-base font-medium">투표 마감일</Label>
          <Input 
            type="datetime-local" 
            value={deadline}
            onChange={(e) => setDeadline(e.target.value)}
            className="h-12 bg-stone-50 border-stone-200 rounded-xl"
          />
        </div>

        {/* 추가 설정 */}
        <div className="space-y-4">
          <Label className="text-base font-medium">추가 설정</Label>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm text-stone-900">복수 선택 허용</Label>
                <p className="text-xs text-stone-500">여러 항목에 투표할 수 있습니다</p>
              </div>
              <Switch 
                checked={allowMultiple} 
                onCheckedChange={setAllowMultiple}
                className="data-[state=checked]:bg-orange-500"
              />
            </div>
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm text-stone-900">익명 투표</Label>
                <p className="text-xs text-stone-500">누가 투표했는지 공개하지 않습니다</p>
              </div>
              <Switch 
                checked={isAnonymous} 
                onCheckedChange={setIsAnonymous}
                className="data-[state=checked]:bg-orange-500"
              />
            </div>
          </div>
        </div>

        {/* 제출 버튼 */}
        <div className="pt-4">
          <Button 
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="w-full h-12 text-lg bg-orange-500 hover:bg-orange-600 rounded-xl text-white shadow-md disabled:opacity-50"
          >
            {isSubmitting ? '생성 중...' : '투표 올리기'}
          </Button>
        </div>
      </div>
    </div>
  );
}
