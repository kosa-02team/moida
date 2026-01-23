import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Camera, X } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Textarea } from '../../ui/textarea';
import { Badge } from '../../ui/badge';
import { getClub, updateClub, type ClubDetailResponse, type ClubUpdateRequest } from '../../../../api/club-full';
import { getMembers } from '../../../../api/member';
import { getMyInfo } from '../../../../api/user';

export function EditGroupView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const [loading, setLoading] = useState(true);
  const [canEdit, setCanEdit] = useState(false);
  
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');
  const [image, setImage] = useState<string | null>(null);
  
  useEffect(() => {
    async function checkPermission() {
      if (!groupId) return;
      try {
        const myInfo = await getMyInfo();
        const members = await getMembers(Number(groupId), 'ACTIVE');
        const currentMember = members.find(m => m.userId === myInfo.userId);
        if (currentMember) {
          const roles = currentMember.roles || [];
          // 운영진(STAFF) 이상만 수정 가능
          setCanEdit(roles.includes('OWNER') || roles.includes('STAFF'));
        }
      } catch (error) {
        console.error('권한 확인 실패:', error);
      }
    }
    checkPermission();
  }, [groupId]);

  useEffect(() => {
    async function fetchClubData() {
      if (!groupId) return;
      try {
        setLoading(true);
        const club = await getClub(Number(groupId));
        setName(club.clubName || '');
        setDescription(''); // TODO: 백엔드에 description 필드 추가 필요
        setTags([]); // TODO: 백엔드에 tags 필드 추가 필요
        
        // localStorage에서 기존 이미지 불러오기
        const savedImage = localStorage.getItem(`club_image_${groupId}`);
        if (savedImage) {
          setImage(savedImage);
        }
      } catch (error) {
        console.error('모임 정보 조회 실패:', error);
        toast.error('모임 정보를 불러오는데 실패했습니다.');
        navigate(-1);
      } finally {
        setLoading(false);
      }
    }
    fetchClubData();
  }, [groupId, navigate]);

  const handleAddTag = () => {
    if (tagInput.trim() && !tags.includes(tagInput.trim())) {
      setTags([...tags, tagInput.trim()]);
      setTagInput('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter(tag => tag !== tagToRemove));
  };

  const handleImageUpload = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (!file) return;
      
      if (!file.type.startsWith('image/')) {
        toast.error('이미지 파일만 업로드 가능합니다');
        return;
      }
      
      const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
      if (file.size > MAX_FILE_SIZE) {
        toast.error('이미지 크기는 5MB 이하만 가능합니다');
        return;
      }
      
      const reader = new FileReader();
      reader.onload = (event) => {
        const result = event.target?.result as string;
        if (result) {
          setImage(result);
        }
      };
      reader.onerror = () => {
        toast.error('이미지 읽기에 실패했습니다');
      };
      reader.readAsDataURL(file);
    };
    input.click();
  };

  const removeImage = () => {
    setImage(null);
  };

  const handleSubmit = async () => {
    if (!name.trim()) {
      toast.error('모임 이름을 입력해주세요');
      return;
    }
    if (!groupId) {
      toast.error('모임 ID가 없습니다');
      return;
    }

    try {
      const request: ClubUpdateRequest = {
        clubName: name.trim(),
        // TODO: description, tags, image 필드 추가 (백엔드 API 확장 필요)
      };
      await updateClub(Number(groupId), request);
      
      // 모임 이미지를 로컬 스토리지에 저장
      if (image && groupId) {
        localStorage.setItem(`club_image_${groupId}`, image);
      } else if (!image && groupId) {
        // 이미지가 제거된 경우 localStorage에서도 삭제
        localStorage.removeItem(`club_image_${groupId}`);
      }
      
      toast.success('모임 정보가 저장되었습니다');
      setTimeout(() => navigate(-1), 500);
    } catch (error) {
      console.error('모임 정보 수정 실패:', error);
      toast.error('모임 정보 수정에 실패했습니다.');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 pb-20 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  if (!canEdit) {
    return (
      <div className="min-h-screen bg-stone-50 pb-20 flex items-center justify-center">
        <div className="text-center">
          <p className="text-stone-700 font-medium mb-2">권한이 없습니다</p>
          <p className="text-sm text-stone-500 mb-4">모임장 또는 운영진만 모임 정보를 수정할 수 있습니다.</p>
          <Button onClick={() => navigate(-1)} variant="outline">
            돌아가기
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-4" onDragStart={(e) => e.preventDefault()} onDragOver={(e) => e.preventDefault()}>
      {/* Header */}
      <header className="sticky top-0 z-[70] bg-white shadow-sm -mx-4 md:-mx-6 px-4 md:px-6 mb-4">
        <div className="flex items-center justify-between py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="font-bold text-lg text-stone-800">모임 정보 수정</h1>
          <div className="w-10" /> {/* Spacer for centering */}
        </div>
      </header>

      <div className="space-y-6">
        {/* Image Upload */}
        <div className="flex justify-center py-4">
          <div className="relative">
            <div className="w-32 h-32 bg-stone-100 rounded-2xl overflow-hidden border-2 border-stone-200 flex items-center justify-center">
              {image ? (
                <img src={image} alt="Group cover" className="w-full h-full object-cover" draggable={false} onDragStart={(e) => e.preventDefault()} />
              ) : (
                <Camera className="w-8 h-8 text-stone-400" />
              )}
            </div>
            {image ? (
              <button 
                className="absolute bottom-0 right-0 p-2 bg-red-500 text-white rounded-full shadow-lg hover:bg-red-600 transition-colors"
                onClick={removeImage}
              >
                <X className="w-4 h-4" />
              </button>
            ) : (
              <button 
                className="absolute bottom-0 right-0 p-2 bg-orange-500 text-white rounded-full shadow-lg hover:bg-orange-600 transition-colors"
                onClick={handleImageUpload}
              >
                <Camera className="w-4 h-4" />
              </button>
            )}
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

