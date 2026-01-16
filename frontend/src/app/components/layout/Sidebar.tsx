import { Home, Calendar, DollarSign, BookOpen, PieChart, Settings } from 'lucide-react';
import { NavLink, useLocation } from 'react-router-dom';
import { cn } from '../ui/utils';

const navItems = [
  { to: '/', icon: Home, label: '홈' },
  { to: '/schedule', icon: Calendar, label: '일정' },
  { to: '/dues', icon: DollarSign, label: '회비' },
  { to: '/stories', icon: BookOpen, label: '스토리' },
  { to: '/stats', icon: PieChart, label: '통계' },
  { to: '/admin', icon: Settings, label: '관리' },
];

export function Sidebar() {
  const location = useLocation();
  const isGroupPage = location.pathname.startsWith('/group/');

  if (!isGroupPage) return null;

  return (
    <aside className="hidden md:flex flex-col w-64 bg-white border-r border-stone-200 h-screen sticky top-0">
      <div className="p-4 border-b border-stone-100">
        <h2 className="font-bold text-lg text-stone-800">메뉴</h2>
      </div>
      <nav className="flex-1 p-4 space-y-2">
        {navItems.map((item) => {
          const isActive = location.pathname === item.to || 
            (item.to !== '/' && location.pathname.startsWith(item.to));
          
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={cn(
                'flex items-center gap-3 px-4 py-3 rounded-lg transition-colors',
                isActive
                  ? 'bg-orange-50 text-orange-600 font-medium'
                  : 'text-stone-600 hover:bg-stone-50 hover:text-stone-900'
              )}
            >
              <item.icon className="w-5 h-5" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>
    </aside>
  );
}




