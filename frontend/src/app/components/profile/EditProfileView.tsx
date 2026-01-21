import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Camera } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Avatar, AvatarFallback, AvatarImage } from '../ui/avatar';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '../ui/alert-dialog';
import { getMyInfo, updateMyInfo, UserResponse } from '@/api/user';
import { removeToken } from '@/api/client';

export function EditProfileView() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [user, setUser] = useState<UserResponse | null>(null);
  const [name, setName] = useState('');
  const [avatar, setAvatar] = useState('');

  useEffect(() => {
    async function fetchUser() {
      try {
        setInitialLoading(true);
        const userInfo = await getMyInfo();
        setUser(userInfo);
        setName(userInfo.realName);
      } catch (error) {
        console.error('프로필 조회 실패:', error);
        toast.error('프로필을 불러오는데 실패했습니다.');
      } finally {
        setInitialLoading(false);
      }
    }
    fetchUser();
  }, []);

  const handleAvatarUpload = () => {
    toast.info('이미지 업로드 기능은 준비 중입니다');
  };

  const handleSave = async () => {
    if (!name.trim()) {
      toast.error('이름을 입력해주세요');
      return;
    }

    setIsLoading(true);
    try {
      await updateMyInfo({ realName: name });
      toast.success('프로필이 수정되었습니다');
      navigate(-1);
    } catch (error) {
      console.error('프로필 수정 실패:', error);
      toast.error('프로필 수정에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteAccount = async () => {
    try {
      setIsLoading(true);
      await deleteAccount();
      removeToken();
      localStorage.removeItem('refreshToken');
      toast.success('회원 탈퇴가 완료되었습니다');
      navigate('/login');
    } catch (error) {
      console.error('회원 탈퇴 실패:', error);
      toast.error('회원 탈퇴에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  if (initialLoading) {
    return (
      <div className="min-h-screen bg-white flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="min-h-screen bg-white flex items-center justify-center">
        <div className="text-stone-500">프로필을 불러올 수 없습니다.</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-white">
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
            <h1 className="ml-2 text-lg font-semibold text-stone-800">프로필 수정</h1>
          </div>
          <Button
            onClick={handleSave}
            disabled={isLoading}
            className="bg-orange-500 hover:bg-orange-600 text-white rounded-full px-4"
          >
            {isLoading ? '저장 중...' : '저장'}
          </Button>
        </div>
      </header>

      <div className="p-6 space-y-8">
        {/* Avatar */}
        <div className="flex flex-col items-center">
          <div className="relative">
            <Avatar className="w-28 h-28 border-4 border-orange-100">
              <AvatarImage src={avatar} />
              <AvatarFallback className="text-3xl bg-orange-100 text-orange-600">
                {name?.[0] || '?'}
              </AvatarFallback>
            </Avatar>
            <button
              onClick={handleAvatarUpload}
              className="absolute bottom-0 right-0 w-10 h-10 bg-orange-500 rounded-full flex items-center justify-center text-white shadow-lg border-2 border-white"
            >
              <Camera className="w-5 h-5" />
            </button>
          </div>
          <button
            onClick={() => setAvatar('')}
            className="mt-3 text-sm text-stone-500 hover:text-red-500"
          >
            기본 이미지로 변경
          </button>
        </div>

        {/* Form */}
        <div className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="name">이름 *</Label>
            <Input
              id="name"
              placeholder="이름을 입력하세요"
              className="h-12 bg-stone-50 border-stone-200 rounded-xl"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label className="text-stone-500">로그인 ID</Label>
            <Input
              value={user.loginId}
              disabled
              className="h-12 bg-stone-100 border-stone-200 rounded-xl text-stone-500"
            />
            <p className="text-xs text-stone-500">로그인 ID는 변경할 수 없습니다.</p>
          </div>
        </div>

        {/* Danger Zone */}
        <div className="pt-6 border-t border-stone-100">
          <h3 className="text-sm font-medium text-stone-500 mb-4">계정 관리</h3>
          
          <Button
            variant="outline"
            className="w-full h-12 text-stone-600 border-stone-200 rounded-xl mb-3"
            onClick={() => navigate('/forgot-password')}
          >
            비밀번호 변경
          </Button>

          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button
                variant="outline"
                className="w-full h-12 text-red-500 border-red-200 hover:bg-red-50 rounded-xl"
              >
                회원 탈퇴
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>정말 탈퇴하시겠습니까?</AlertDialogTitle>
                <AlertDialogDescription>
                  회원 탈퇴 시 모든 데이터가 삭제되며, 복구할 수 없습니다.
                  <br />
                  가입한 모임에서 자동으로 탈퇴됩니다.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>취소</AlertDialogCancel>
                <AlertDialogAction
                  onClick={handleDeleteAccount}
                  className="bg-red-500 hover:bg-red-600"
                >
                  탈퇴하기
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      </div>
    </div>
  );
}



