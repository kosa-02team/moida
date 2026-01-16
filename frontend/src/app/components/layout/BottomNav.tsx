import { Home, Calendar, DollarSign, BookOpen, PieChart, Settings } from 'lucide-react';
import { NavLink } from 'react-router-dom';

export function BottomNav() {
  const navItems = [
    { to: '/', icon: Home, label: '홈' },
    { to: '/schedule', icon: Calendar, label: '일정' },
    { to: '/dues', icon: DollarSign, label: '회비' },
    { to: '/stories', icon: BookOpen, label: '스토리' },
    { to: '/stats', icon: PieChart, label: '통계' },
    { to: '/admin', icon: Settings, label: '관리' },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur-sm border-t border-stone-200 pb-safe z-50 md:hidden">
      <div className="flex justify-between items-center px-2 h-16 max-w-md mx-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex flex-col items-center justify-center w-full h-full space-y-1 transition-colors ${
                isActive ? 'text-orange-500' : 'text-stone-400 hover:text-stone-600'
              }`
            }
            aria-label={item.label}
          >
            <item.icon className="w-5 h-5" />
            <span className="text-[10px] font-medium">{item.label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
