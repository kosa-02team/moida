import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Camera, X } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Textarea } from '../../ui/textarea';
import { Badge } from '../../ui/badge';

export function EditGroupView() {
  const navigate = useNavigate();
  
  // Mock data - 실제로는 API에서 가져와야 함
  const [name, setName] = useState('주말 등산 클럽');
  const [description, setDescription] = useState('매주 토요일 서울 근교 산행합니다. 초보자 환영!');
  const [tags, setTags] = useState<string[]>(['등산', '운동', '친목']);
  const [tagInput, setTagInput] = useState('');
  const [image, setImage] = useState('https://images.unsplash.com/photo-1551632811-561732d1e306?w=800&auto=format&fit=crop&q=60');

  const handleAddTag = () => {
    if (tagInput.trim() && !tags.includes(tagInput.trim())) {
      setTags([...tags, tagInput.trim()]);
      setTagInput('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter(tag => tag !== tagToRemove));
  };

  const handleSubmit = () => {
    if (!name.trim()) {
      toast.error('모임 이름을 입력해주세요');
      return;
    }
    // 실제로는 API 호출
    console.log({ name, description, tags, image });
    toast.success('모임 정보가 저장되었습니다');
    setTimeout(() => navigate(-1), 500);
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100 backdrop-blur-sm bg-white/95">
        <div className="flex items-center justify-between px-4 py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="font-bold text-lg text-stone-800">모임 정보 수정</h1>
          <div className="w-10" /> {/* Spacer for centering */}
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Image Upload */}
        <div className="flex justify-center py-4">
          <div className="relative">
            <div className="w-32 h-32 bg-stone-100 rounded-2xl overflow-hidden border-2 border-stone-200">
              <img src={image} alt="Group cover" className="w-full h-full object-cover" />
            </div>
            <button className="absolute bottom-0 right-0 p-2 bg-orange-500 text-white rounded-full shadow-lg hover:bg-orange-600 transition-colors">
              <Camera className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Name */}
        <div className="space-y-2">
          <Label htmlFor="name" className="text-base font-medium">모임 이름</Label>
          <Input 
            id="name" 
            placeholder="모임 이름을 입력하세요" 
            className="h-12 text-lg bg-white border-stone-200 focus-visible:ring-orange-500 rounded-xl"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>

        {/* Description */}
        <div className="space-y-2">
          <Label htmlFor="description" className="text-base font-medium">모임 소개</Label>
          <Textarea 
            id="description" 
            placeholder="모임에 대한 소개를 작성하세요"
            className="min-h-24 bg-white border-stone-200 focus-visible:ring-orange-500 rounded-xl resize-none"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        {/* Tags */}
        <div className="space-y-2">
          <Label htmlFor="tags" className="text-base font-medium">태그</Label>
          <div className="flex gap-2">
            <Input 
              id="tags" 
              placeholder="태그를 입력하고 엔터" 
              className="flex-1 h-12 bg-white border-stone-200 focus-visible:ring-orange-500 rounded-xl"
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  handleAddTag();
                }
              }}
            />
            <Button 
              type="button"
              onClick={handleAddTag}
              className="h-12 px-4 bg-orange-500 hover:bg-orange-600 text-white rounded-xl"
            >
              추가
            </Button>
          </div>
          <div className="flex flex-wrap gap-2 pt-2">
            {tags.map(tag => (
              <Badge 
                key={tag} 
                variant="secondary" 
                className="bg-orange-100 text-orange-700 font-normal px-3 py-1 flex items-center gap-1.5"
              >
                #{tag}
                <button
                  onClick={() => handleRemoveTag(tag)}
                  className="hover:bg-orange-200 rounded-full p-0.5 transition-colors"
                >
                  <X className="w-3 h-3" />
                </button>
              </Badge>
            ))}
          </div>
        </div>

        {/* Submit Button */}
        <div className="pt-4">
          <Button 
            onClick={handleSubmit}
            className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl"
          >
            저장하기
          </Button>
        </div>
      </div>
    </div>
  );
}

