import { Link } from 'react-router-dom';
import { Home, ArrowLeft, Search, Mountain } from 'lucide-react';
import { Button } from './ui/button';

export function NotFoundView() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-50 via-white to-stone-100 flex flex-col items-center justify-center p-6">
      {/* Illustration */}
      <div className="relative mb-8">
        <div className="w-32 h-32 bg-orange-100 rounded-full flex items-center justify-center">
          <Mountain className="w-16 h-16 text-orange-300" />
        </div>
        <div className="absolute -top-2 -right-2 w-16 h-16 bg-orange-500 rounded-2xl flex items-center justify-center text-white text-3xl font-bold shadow-lg rotate-12">
          ?
        </div>
      </div>

      {/* Text */}
      <div className="text-center space-y-3 mb-8">
        <h1 className="text-6xl font-bold text-stone-900">404</h1>
        <h2 className="text-xl font-semibold text-stone-700">페이지를 찾을 수 없습니다</h2>
        <p className="text-stone-500 max-w-xs">
          요청하신 페이지가 존재하지 않거나,<br />
          이동되었을 수 있습니다.
        </p>
      </div>

      {/* Actions */}
      <div className="flex flex-col sm:flex-row gap-3 w-full max-w-xs">
        <Link to="/" className="flex-1">
          <Button className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white rounded-xl">
            <Home className="w-5 h-5 mr-2" />
            홈으로 가기
          </Button>
        </Link>
        <Button
          variant="outline"
          onClick={() => window.history.back()}
          className="flex-1 h-12 rounded-xl"
        >
          <ArrowLeft className="w-5 h-5 mr-2" />
          이전 페이지
        </Button>
      </div>

      {/* Help */}
      <div className="mt-12 text-center">
        <p className="text-sm text-stone-500 mb-3">이 페이지를 찾고 계셨나요?</p>
        <div className="flex flex-wrap justify-center gap-2">
          <Link to="/" className="text-sm text-orange-600 hover:underline">나의 모임</Link>
          <span className="text-stone-300">•</span>
          <Link to="/explore" className="text-sm text-orange-600 hover:underline">모임 둘러보기</Link>
          <span className="text-stone-300">•</span>
          <Link to="/help" className="text-sm text-orange-600 hover:underline">도움말</Link>
        </div>
      </div>
    </div>
  );
}



