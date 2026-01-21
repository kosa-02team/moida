import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowDownLeft, ArrowUpRight, Search, Filter, Calendar } from 'lucide-react';
import { Button } from '../../ui/button';
import { Input } from '../../ui/input';
import { Badge } from '../../ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../ui/select';

interface Transaction {
  id: number;
  title: string;
  amount: number;
  date: string;
  type: 'income' | 'expense';
  category: string;
  member?: string;
}

export function DuesHistoryView() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<'all' | 'income' | 'expense'>('all');
  const [filterMonth, setFilterMonth] = useState('all');

  // Mock data
  const transactions: Transaction[] = [
    { id: 1, title: '4월 정기 산행 뒤풀이', amount: -150000, date: '2024.04.12', type: 'expense', category: '식비' },
    { id: 2, title: '홍길동 회비 입금', amount: 30000, date: '2024.04.10', type: 'income', category: '회비', member: '홍길동' },
    { id: 3, title: '김철수 회비 입금', amount: 30000, date: '2024.04.10', type: 'income', category: '회비', member: '김철수' },
    { id: 4, title: '이영희 회비 입금', amount: 30000, date: '2024.04.08', type: 'income', category: '회비', member: '이영희' },
    { id: 5, title: '등산 장비 구입', amount: -85000, date: '2024.04.05', type: 'expense', category: '장비' },
    { id: 6, title: '박민수 회비 입금', amount: 30000, date: '2024.04.03', type: 'income', category: '회비', member: '박민수' },
    { id: 7, title: '3월 번개 모임 식사비', amount: -120000, date: '2024.03.28', type: 'expense', category: '식비' },
    { id: 8, title: '정수진 회비 입금', amount: 30000, date: '2024.03.25', type: 'income', category: '회비', member: '정수진' },
    { id: 9, title: '최영철 회비 입금', amount: 30000, date: '2024.03.25', type: 'income', category: '회비', member: '최영철' },
    { id: 10, title: '3월 정기 북한산 산행', amount: -200000, date: '2024.03.15', type: 'expense', category: '교통/식비' },
    { id: 11, title: '김민지 회비 입금', amount: 30000, date: '2024.03.10', type: 'income', category: '회비', member: '김민지' },
    { id: 12, title: '응급키트 구입', amount: -45000, date: '2024.03.05', type: 'expense', category: '장비' },
  ];

  const filteredTransactions = transactions.filter(tx => {
    const matchesSearch = tx.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          (tx.member && tx.member.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesType = filterType === 'all' || tx.type === filterType;
    const matchesMonth = filterMonth === 'all' || tx.date.includes(filterMonth);
    return matchesSearch && matchesType && matchesMonth;
  });

  const totalIncome = filteredTransactions
    .filter(tx => tx.type === 'income')
    .reduce((sum, tx) => sum + tx.amount, 0);
  
  const totalExpense = filteredTransactions
    .filter(tx => tx.type === 'expense')
    .reduce((sum, tx) => sum + Math.abs(tx.amount), 0);

  // Group transactions by month
  const groupedTransactions = filteredTransactions.reduce((groups, tx) => {
    const month = tx.date.substring(0, 7).replace('.', '년 ') + '월';
    if (!groups[month]) {
      groups[month] = [];
    }
    groups[month].push(tx);
    return groups;
  }, {} as Record<string, Transaction[]>);

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100 backdrop-blur-sm bg-white/95">
        <div className="flex items-center justify-between px-4 py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="font-bold text-lg text-stone-800">전체 내역</h1>
          <div className="w-10" />
        </div>
      </header>

      <div className="p-5 space-y-5">
        {/* Summary Cards */}
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
            <p className="text-sm text-blue-600 mb-1">총 수입</p>
            <p className="text-xl font-bold text-blue-700">+{totalIncome.toLocaleString()}원</p>
          </div>
          <div className="bg-red-50 border border-red-200 rounded-xl p-4">
            <p className="text-sm text-red-600 mb-1">총 지출</p>
            <p className="text-xl font-bold text-red-700">-{totalExpense.toLocaleString()}원</p>
          </div>
        </div>

        {/* Search and Filters */}
        <div className="space-y-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
            <Input
              placeholder="내역 또는 멤버 이름 검색"
              className="pl-10 h-11 bg-white border-stone-200 rounded-xl"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          
          <div className="flex gap-3">
            <Select value={filterType} onValueChange={(v: 'all' | 'income' | 'expense') => setFilterType(v)}>
              <SelectTrigger className="flex-1 h-10 bg-white border-stone-200 rounded-lg">
                <Filter className="w-4 h-4 mr-2 text-stone-400" />
                <SelectValue placeholder="유형" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">전체</SelectItem>
                <SelectItem value="income">수입</SelectItem>
                <SelectItem value="expense">지출</SelectItem>
              </SelectContent>
            </Select>

            <Select value={filterMonth} onValueChange={setFilterMonth}>
              <SelectTrigger className="flex-1 h-10 bg-white border-stone-200 rounded-lg">
                <Calendar className="w-4 h-4 mr-2 text-stone-400" />
                <SelectValue placeholder="기간" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">전체</SelectItem>
                <SelectItem value="2024.04">2024년 4월</SelectItem>
                <SelectItem value="2024.03">2024년 3월</SelectItem>
                <SelectItem value="2024.02">2024년 2월</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Transaction List */}
        {Object.keys(groupedTransactions).length > 0 ? (
          <div className="space-y-6">
            {Object.entries(groupedTransactions).map(([month, txList]) => (
              <div key={month} className="space-y-3">
                <h3 className="font-semibold text-stone-700 px-1">{month}</h3>
                <div className="bg-white rounded-xl border border-stone-200 overflow-hidden">
                  {txList.map((tx, i) => (
                    <div 
                      key={tx.id} 
                      className={`p-4 flex justify-between items-center ${i !== txList.length - 1 ? 'border-b border-stone-100' : ''}`}
                    >
                      <div className="flex items-center gap-3">
                        <div className={`w-10 h-10 rounded-full flex items-center justify-center ${tx.type === 'income' ? 'bg-blue-50 text-blue-600' : 'bg-red-50 text-red-600'}`}>
                          {tx.type === 'income' ? <ArrowDownLeft className="w-5 h-5" /> : <ArrowUpRight className="w-5 h-5" />}
                        </div>
                        <div>
                          <p className="font-medium text-stone-900">{tx.title}</p>
                          <div className="flex items-center gap-2 mt-0.5">
                            <span className="text-xs text-stone-400">{tx.date}</span>
                            <Badge variant="secondary" className="text-xs px-1.5 py-0 h-5 bg-stone-100 text-stone-600">
                              {tx.category}
                            </Badge>
                          </div>
                        </div>
                      </div>
                      <span className={`font-bold ${tx.type === 'income' ? 'text-blue-600' : 'text-stone-900'}`}>
                        {tx.type === 'income' ? '+' : ''}{tx.amount.toLocaleString()}원
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <div className="w-16 h-16 bg-stone-100 rounded-full flex items-center justify-center mb-4">
              <Search className="w-8 h-8 text-stone-400" />
            </div>
            <h3 className="text-lg font-semibold text-stone-700 mb-2">내역이 없습니다</h3>
            <p className="text-sm text-stone-500">검색 조건에 맞는 내역이 없습니다.</p>
          </div>
        )}
      </div>
    </div>
  );
}

