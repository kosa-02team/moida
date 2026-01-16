import { Outlet, NavLink, useParams, Link } from 'react-router-dom';
import { ArrowLeft, Menu } from 'lucide-react';
import { Button } from '../ui/button';

export function GroupLayout() {
  const { groupId } = useParams();
  
  const tabs = [
    { label: '홈', path: '' }, // Relative path to /group/:id
    { label: '일정', path: 'schedule' },
    { label: '회비', path: 'dues' },
    { label: '스토리', path: 'stories' },
    { label: '통계', path: 'stats' },
    { label: '관리', path: 'admin' },
  ];

  return (
    <div className="min-h-screen bg-stone-50 text-stone-900 font-sans">
      <div className="max-w-md md:max-w-2xl lg:max-w-4xl mx-auto min-h-screen bg-white shadow-xl relative flex flex-col">
        {/* Top Navigation Bar */}
        <header className="flex items-center justify-between px-4 py-3 bg-white border-b border-stone-100 sticky top-0 z-30 backdrop-blur-sm bg-white/95">
          <Link to="/">
            <Button variant="ghost" size="icon" className="-ml-2" aria-label="뒤로가기">
              <ArrowLeft className="w-6 h-6 text-stone-800" />
            </Button>
          </Link>
          <h1 className="font-bold text-lg text-stone-800 truncate px-2">주말 등산 클럽</h1>
          <Button variant="ghost" size="icon" className="-mr-2" aria-label="메뉴">
            <Menu className="w-6 h-6 text-stone-800" />
          </Button>
        </header>

        {/* Scrollable Tabs */}
        <div className="flex overflow-x-auto scrollbar-hide border-b border-stone-100 bg-white sticky top-[57px] z-20 md:justify-center">
          {tabs.map((tab) => (
            <NavLink
              key={tab.path}
              to={tab.path} // Relative path
              end={tab.path === ''} // Only match exact for home
              className={({ isActive }) =>
                `flex-none px-5 py-3 text-sm font-medium whitespace-nowrap transition-colors border-b-2 ${
                  isActive
                    ? 'border-orange-500 text-orange-600'
                    : 'border-transparent text-stone-500 hover:text-stone-700'
                }`
              }
            >
              {tab.label}
            </NavLink>
          ))}
        </div>

        {/* Main Content */}
        <main className="flex-1 overflow-y-auto bg-stone-50 p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
