import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Wallet, Users, Search, TrendingUp, Info, Coins, UserPlus, UserMinus } from 'lucide-react';
import { Button } from '../../ui/button';
import { Card, CardContent } from '../../ui/card';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';
import { Badge } from '../../ui/badge';
import { Input } from '../../ui/input';
import { getGroupById, MANAGEMENT_TYPE_INFO } from '../../../data/mockData';
import { getRoleLabel, getRoleColor } from '../../../data/userRoles';

interface MemberShare {
  id: string;
  name: string;
  avatar?: string;
  role: 'owner' | 'treasurer' | 'manager' | 'member';
  share: number;       // 현재 지분 (공정정산형: 모두 동일)
  contribution: number; // 총 입금액
  joinedDate: string;
}

export function ShareManagementView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const [searchQuery, setSearchQuery] = useState('');

  // 모임 정보 가져오기
  const group = getGroupById(groupId || '1');
  
  if (!group) {
    return <div className="p-4">모임을 찾을 수 없습니다.</div>;
  }

  const { account } = group;
  const isFairType = account.managementType === 'fair';
  const typeInfo = MANAGEMENT_TYPE_INFO[account.managementType];

  // Mock 멤버 지분 데이터
  // 공정정산형: 모든 멤버 동일 지분
  const memberShares: MemberShare[] = [
    { id: '1', name: '홍길동', avatar: '', role: 'owner', share: account.perPersonShare || 0, contribution: 150000, joinedDate: '2024.01.15' },
    { id: '2', name: '김철수', avatar: '', role: 'treasurer', share: account.perPersonShare || 0, contribution: 100000, joinedDate: '2024.01.20' },
    { id: '3', name: '이영희', avatar: '', role: 'manager', share: account.perPersonShare || 0, contribution: 90000, joinedDate: '2024.02.01' },
    { id: '4', name: '박민수', avatar: '', role: 'member', share: account.perPersonShare || 0, contribution: 85000, joinedDate: '2024.02.15' },
    { id: '5', name: '정지훈', avatar: '', role: 'member', share: account.perPersonShare || 0, contribution: 80000, joinedDate: '2024.03.01' },
    { id: '6', name: '최유진', avatar: '', role: 'member', share: account.perPersonShare || 0, contribution: 75000, joinedDate: '2024.03.10' },
  ];

  const roleLabels: Record<string, string> = {
    owner: '모임장',
    treasurer: '총무',
    manager: '운영진',
    member: '회원',
  };

  const roleColors: Record<string, string> = {
    owner: 'bg-orange-100 text-orange-700',
    treasurer: 'bg-green-100 text-green-700',
    manager: 'bg-blue-100 text-blue-700',
    member: 'bg-stone-100 text-stone-600',
  };

  const filteredMembers = memberShares.filter(m =>
    m.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="-ml-2">
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">지분 관리</h1>
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Account Summary */}
        <Card className="border-stone-200 bg-gradient-to-br from-stone-900 to-stone-800 text-white">
          <CardContent className="p-5">
            <div className="flex justify-between items-start mb-4">
              <div>
                <p className="text-stone-400 text-sm">총 모임 통장 잔액</p>
                <p className="text-3xl font-bold">{account.totalBalance.toLocaleString()}원</p>
              </div>
              <Badge className={isFairType ? 'bg-green-500 text-white' : 'bg-blue-500 text-white'}>
                {typeInfo.name}
              </Badge>
            </div>
            <div className="flex items-center gap-4 text-sm text-stone-400">
              <span className="flex items-center gap-1">
                <Users className="w-4 h-4" />
                {account.memberCount}명
              </span>
              {isFairType && (
                <span className="flex items-center gap-1">
                  <Coins className="w-4 h-4" />
                  1인당 {(account.perPersonShare || 0).toLocaleString()}원
                </span>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Management Type Info */}
        <Card className={isFairType ? 'border-green-200 bg-green-50' : 'border-blue-200 bg-blue-50'}>
          <CardContent className="p-4">
            <div className="flex items-start gap-3">
              <Info className={`w-5 h-5 mt-0.5 ${isFairType ? 'text-green-600' : 'text-blue-600'}`} />
              <div>
                <p className={`text-sm font-medium ${isFairType ? 'text-green-800' : 'text-blue-800'}`}>
                  {typeInfo.name} 안내
                </p>
                <ul className={`text-xs mt-2 space-y-1 ${isFairType ? 'text-green-700' : 'text-blue-700'}`}>
                  {typeInfo.features.map((feature, i) => (
                    <li key={i}>• {feature}</li>
                  ))}
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 공정정산형: 가입/탈퇴 시 정산 안내 */}
        {isFairType && (
          <div className="grid grid-cols-2 gap-4">
            <Card className="border-blue-200 bg-blue-50">
              <CardContent className="p-4">
                <div className="flex items-center gap-2 mb-2">
                  <UserPlus className="w-5 h-5 text-blue-600" />
                  <span className="font-medium text-blue-800">신규 가입 시</span>
                </div>
                <p className="text-2xl font-bold text-blue-700">
                  {(account.entryFee || 0).toLocaleString()}원
                </p>
                <p className="text-xs text-blue-600 mt-1">
                  기존 멤버와 동일한 지분 확보
                </p>
              </CardContent>
            </Card>
            <Card className="border-orange-200 bg-orange-50">
              <CardContent className="p-4">
                <div className="flex items-center gap-2 mb-2">
                  <UserMinus className="w-5 h-5 text-orange-600" />
                  <span className="font-medium text-orange-800">탈퇴 시 환불</span>
                </div>
                <p className="text-2xl font-bold text-orange-700">
                  {(account.perPersonShare || 0).toLocaleString()}원
                </p>
                <p className="text-xs text-orange-600 mt-1">
                  본인 지분 전액 환불
                </p>
              </CardContent>
            </Card>
          </div>
        )}

        {/* 운영비형: 누적 금액 안내 */}
        {!isFairType && (
          <div className="grid grid-cols-2 gap-4">
            <Card className="border-stone-100">
              <CardContent className="p-4">
                <div className="flex items-center gap-2 mb-2">
                  <TrendingUp className="w-5 h-5 text-blue-600" />
                  <span className="font-medium text-stone-800">총 입금</span>
                </div>
                <p className="text-2xl font-bold text-blue-600">
                  {(account.totalDeposited || 0).toLocaleString()}원
                </p>
              </CardContent>
            </Card>
            <Card className="border-stone-100">
              <CardContent className="p-4">
                <div className="flex items-center gap-2 mb-2">
                  <Wallet className="w-5 h-5 text-orange-600" />
                  <span className="font-medium text-stone-800">총 사용</span>
                </div>
                <p className="text-2xl font-bold text-orange-600">
                  {(account.totalUsed || 0).toLocaleString()}원
                </p>
              </CardContent>
            </Card>
          </div>
        )}

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
          <Input
            placeholder="멤버 검색"
            className="pl-10 h-11 bg-white border-stone-200 rounded-xl"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Member Shares List */}
        <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
          <div className="px-4 py-3 bg-stone-50 border-b border-stone-100 flex justify-between items-center">
            <h3 className="font-bold text-stone-900">
              {isFairType ? '멤버별 지분 (모두 동일)' : '멤버별 납부 현황'}
            </h3>
            <span className="text-xs text-stone-500">{filteredMembers.length}명</span>
          </div>
          <div className="divide-y divide-stone-100">
            {filteredMembers.map((member) => (
              <div key={member.id} className="p-4">
                <div className="flex items-center gap-3">
                  <Avatar className="w-10 h-10">
                    <AvatarImage src={member.avatar} />
                    <AvatarFallback>{member.name[0]}</AvatarFallback>
                  </Avatar>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <p className="font-medium text-stone-900">{member.name}</p>
                      <Badge className={roleColors[member.role]}>
                        {roleLabels[member.role]}
                      </Badge>
                    </div>
                    <p className="text-xs text-stone-500">가입일: {member.joinedDate}</p>
                  </div>
                  <div className="text-right">
                    {isFairType ? (
                      <>
                        <p className="text-lg font-bold text-green-600">
                          {member.share.toLocaleString()}원
                        </p>
                        <p className="text-xs text-green-600">지분</p>
                      </>
                    ) : (
                      <>
                        <p className="text-lg font-bold text-blue-600">
                          {member.contribution.toLocaleString()}원
                        </p>
                        <p className="text-xs text-stone-500">총 납부</p>
                      </>
                    )}
                  </div>
                </div>

                {/* 공정정산형: 동일 지분 표시 */}
                {isFairType && (
                  <div className="mt-3 flex items-center justify-between p-2 bg-green-50 rounded-lg">
                    <span className="text-xs text-green-700 flex items-center gap-1">
                      <Coins className="w-3 h-3" />
                      1/{account.memberCount} 지분
                    </span>
                    <span className="text-xs text-green-600">
                      = {(100 / account.memberCount).toFixed(1)}%
                    </span>
                  </div>
                )}

                {/* 운영비형: 납부 내역 */}
                {!isFairType && (
                  <div className="mt-3 text-xs text-stone-500">
                    총 {member.contribution.toLocaleString()}원 납부
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* 운영비형 안내 */}
        {!isFairType && (
          <Card className="border-amber-200 bg-amber-50">
            <CardContent className="p-4">
              <div className="flex items-start gap-3">
                <Info className="w-5 h-5 text-amber-600 mt-0.5" />
                <div>
                  <p className="text-sm font-medium text-amber-800">운영비형 안내</p>
                  <p className="text-xs text-amber-700 mt-1">
                    운영비형 모임은 개인 지분 개념이 없습니다.<br/>
                    탈퇴 시 환불이 없으며, 모임 통장 잔액은 계속 축적됩니다.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
