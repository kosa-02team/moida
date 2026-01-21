import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Moon, Sun, Globe, Bell, Smartphone, Palette, ChevronRight, Check } from 'lucide-react';
import { Button } from '../ui/button';
import { Switch } from '../ui/switch';
import { Label } from '../ui/label';
import { RadioGroup, RadioGroupItem } from '../ui/radio-group';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';

export function SettingsView() {
  const navigate = useNavigate();
  const [darkMode, setDarkMode] = useState(false);
  const [pushNotifications, setPushNotifications] = useState(true);
  const [language, setLanguage] = useState('ko');
  const [showLanguageDialog, setShowLanguageDialog] = useState(false);
  const [showThemeDialog, setShowThemeDialog] = useState(false);
  const [theme, setTheme] = useState<'light' | 'dark' | 'system'>('light');

  const languages = [
    { value: 'ko', label: '한국어' },
    { value: 'en', label: 'English' },
    { value: 'ja', label: '日本語' },
    { value: 'zh', label: '中文' },
  ];

  const themes = [
    { value: 'light', label: '라이트 모드', icon: Sun },
    { value: 'dark', label: '다크 모드', icon: Moon },
    { value: 'system', label: '시스템 설정', icon: Smartphone },
  ];

  return (
    <div className="min-h-screen bg-stone-50">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate(-1)}
            className="-ml-2"
          >
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">앱 설정</h1>
        </div>
      </header>

      <div className="p-4 space-y-4">
        {/* Display Settings */}
        <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
          <div className="px-4 py-3 bg-stone-50 border-b border-stone-100">
            <h3 className="text-sm font-medium text-stone-500">화면 설정</h3>
          </div>
          <div className="divide-y divide-stone-100">
            {/* Theme */}
            <button
              onClick={() => setShowThemeDialog(true)}
              className="w-full flex items-center justify-between p-4 hover:bg-stone-50 transition-colors"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-stone-100 rounded-lg flex items-center justify-center">
                  <Palette className="w-5 h-5 text-stone-600" />
                </div>
                <div className="text-left">
                  <p className="font-medium text-stone-900">테마</p>
                  <p className="text-xs text-stone-500">
                    {themes.find(t => t.value === theme)?.label}
                  </p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-stone-300" />
            </button>

            {/* Language */}
            <button
              onClick={() => setShowLanguageDialog(true)}
              className="w-full flex items-center justify-between p-4 hover:bg-stone-50 transition-colors"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-stone-100 rounded-lg flex items-center justify-center">
                  <Globe className="w-5 h-5 text-stone-600" />
                </div>
                <div className="text-left">
                  <p className="font-medium text-stone-900">언어</p>
                  <p className="text-xs text-stone-500">
                    {languages.find(l => l.value === language)?.label}
                  </p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-stone-300" />
            </button>
          </div>
        </div>

        {/* Notification Settings */}
        <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
          <div className="px-4 py-3 bg-stone-50 border-b border-stone-100">
            <h3 className="text-sm font-medium text-stone-500">알림 설정</h3>
          </div>
          <div className="divide-y divide-stone-100">
            <div className="flex items-center justify-between p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-stone-100 rounded-lg flex items-center justify-center">
                  <Bell className="w-5 h-5 text-stone-600" />
                </div>
                <div>
                  <p className="font-medium text-stone-900">푸시 알림</p>
                  <p className="text-xs text-stone-500">앱 알림 허용</p>
                </div>
              </div>
              <Switch
                checked={pushNotifications}
                onCheckedChange={setPushNotifications}
                className="data-[state=checked]:bg-orange-500"
              />
            </div>

            <div className="flex items-center justify-between p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-stone-100 rounded-lg flex items-center justify-center">
                  <Smartphone className="w-5 h-5 text-stone-600" />
                </div>
                <div>
                  <p className="font-medium text-stone-900">진동</p>
                  <p className="text-xs text-stone-500">알림 시 진동</p>
                </div>
              </div>
              <Switch defaultChecked className="data-[state=checked]:bg-orange-500" />
            </div>
          </div>
        </div>

        {/* Notification Types */}
        <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
          <div className="px-4 py-3 bg-stone-50 border-b border-stone-100">
            <h3 className="text-sm font-medium text-stone-500">알림 유형</h3>
          </div>
          <div className="divide-y divide-stone-100">
            {[
              { label: '새 일정 알림', description: '새로운 일정이 등록되면 알림' },
              { label: '회비 알림', description: '회비 납부 기한 및 정산 알림' },
              { label: '투표 알림', description: '새로운 투표 및 마감 알림' },
              { label: '멤버 알림', description: '새 멤버 가입 및 변동 알림' },
              { label: '스토리 알림', description: '새 스토리 등록 알림' },
            ].map((item) => (
              <div key={item.label} className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium text-stone-900">{item.label}</p>
                  <p className="text-xs text-stone-500">{item.description}</p>
                </div>
                <Switch defaultChecked className="data-[state=checked]:bg-orange-500" />
              </div>
            ))}
          </div>
        </div>

        {/* Cache & Storage */}
        <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
          <div className="px-4 py-3 bg-stone-50 border-b border-stone-100">
            <h3 className="text-sm font-medium text-stone-500">저장공간</h3>
          </div>
          <button
            onClick={() => {}}
            className="w-full flex items-center justify-between p-4 hover:bg-stone-50 transition-colors"
          >
            <div>
              <p className="font-medium text-stone-900">캐시 삭제</p>
              <p className="text-xs text-stone-500">현재 25.3MB 사용 중</p>
            </div>
            <ChevronRight className="w-5 h-5 text-stone-300" />
          </button>
        </div>
      </div>

      {/* Language Dialog */}
      <Dialog open={showLanguageDialog} onOpenChange={setShowLanguageDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>언어 선택</DialogTitle>
            <DialogDescription>앱 표시 언어를 선택하세요</DialogDescription>
          </DialogHeader>
          <RadioGroup value={language} onValueChange={setLanguage} className="py-4">
            {languages.map((lang) => (
              <div
                key={lang.value}
                className="flex items-center justify-between p-3 rounded-lg hover:bg-stone-50 cursor-pointer"
                onClick={() => setLanguage(lang.value)}
              >
                <Label htmlFor={lang.value} className="cursor-pointer flex-1">
                  {lang.label}
                </Label>
                <RadioGroupItem
                  value={lang.value}
                  id={lang.value}
                  className="data-[state=checked]:bg-orange-500 data-[state=checked]:border-orange-500"
                />
              </div>
            ))}
          </RadioGroup>
          <Button
            onClick={() => setShowLanguageDialog(false)}
            className="w-full bg-orange-500 hover:bg-orange-600"
          >
            확인
          </Button>
        </DialogContent>
      </Dialog>

      {/* Theme Dialog */}
      <Dialog open={showThemeDialog} onOpenChange={setShowThemeDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>테마 선택</DialogTitle>
            <DialogDescription>앱 테마를 선택하세요</DialogDescription>
          </DialogHeader>
          <div className="py-4 space-y-2">
            {themes.map((t) => (
              <button
                key={t.value}
                onClick={() => setTheme(t.value as any)}
                className={`w-full flex items-center gap-3 p-4 rounded-xl border-2 transition-colors ${
                  theme === t.value
                    ? 'border-orange-500 bg-orange-50'
                    : 'border-stone-100 hover:border-stone-200'
                }`}
              >
                <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                  theme === t.value ? 'bg-orange-500 text-white' : 'bg-stone-100 text-stone-600'
                }`}>
                  <t.icon className="w-5 h-5" />
                </div>
                <span className="font-medium text-stone-900">{t.label}</span>
                {theme === t.value && (
                  <Check className="w-5 h-5 text-orange-500 ml-auto" />
                )}
              </button>
            ))}
          </div>
          <Button
            onClick={() => setShowThemeDialog(false)}
            className="w-full bg-orange-500 hover:bg-orange-600"
          >
            확인
          </Button>
        </DialogContent>
      </Dialog>
    </div>
  );
}



