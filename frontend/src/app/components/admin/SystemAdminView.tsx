import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import * as React from 'react';
import {
  ArrowLeft,
  Shield,
  Flag,
  Users,
  UserX,
  Ban,
  Search,
  Eye,
  CheckCircle,
  XCircle,
  Clock,
  Trash2,
  Home,
  AlertTriangle,
  User,
  PauseCircle,
  PlayCircle,
  MoreVertical
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../ui/button';
import { Card, CardContent } from '../ui/card';
import { Input } from '../ui/input';
import { Badge } from '../ui/badge';
import { Avatar, AvatarFallback } from '../ui/avatar';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { RadioGroup, RadioGroupItem } from '../ui/radio-group';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../ui/alert-dialog';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';
import {
  getDashboard,
  getReports,
  getReportDetail,
  processReport,
  getUsers,
  manageUser,
  getClubs,
  manageClub,
  AdminDashboardResponse,
  AdminReportResponse,
  AdminUserResponse,
  AdminClubResponse,
} from '@/api/admin';

type SuspendDuration = '1day' | '3days' | '7days' | '30days' | 'permanent';

export function SystemAdminView() {
  const navigate = useNavigate();

  // 상태
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [selectedReport, setSelectedReport] = useState<AdminReportResponse | null>(null);
  const [selectedReportDetail, setSelectedReportDetail] = useState<AdminReportResponse | null>(null);
  const [isLoadingReportDetail, setIsLoadingReportDetail] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [showSuspendDialog, setShowSuspendDialog] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [suspendTarget, setSuspendTarget] = useState<{ type: 'user' | 'group'; id: number; name: string } | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<{ type: 'user' | 'group'; id: number; name: string } | null>(null);
  const [suspendDuration, setSuspendDuration] = useState<SuspendDuration>('7days');
  const [suspendReason, setSuspendReason] = useState('');

  // API 데이터
  const [dashboard, setDashboard] = useState<AdminDashboardResponse | null>(null);
  const [reports, setReports] = useState<AdminReportResponse[]>([]);
  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [clubs, setClubs] = useState<AdminClubResponse[]>([]);
  const [activeMenu, setActiveMenu] = useState<{ type: 'user' | 'group', id: number } | null>(null);

  // API 데이터 불러오기
  const fetchData = async () => {
    try {
      setIsLoading(true);
      const [dashboardData, reportsData, usersData, clubsData] = await Promise.all([
        getDashboard(),
        getReports(0, 100),
        getUsers(0, 100),
        getClubs(0, 100),
      ]);
      setDashboard(dashboardData);
      setReports(reportsData.content);
      setUsers(usersData.content);
      setClubs(clubsData.content);
    } catch (error) {
      console.error('데이터 로드 실패:', error);
      toast.error('데이터를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // 신고 상세 조회
  useEffect(() => {
    if (selectedReport) {
      const fetchReportDetail = async () => {
        try {
          setIsLoadingReportDetail(true);
          const detail = await getReportDetail(selectedReport.reportId);
          setSelectedReportDetail(detail);
        } catch (error) {
          console.error('신고 상세 조회 실패:', error);
          toast.error('신고 상세 정보를 불러오는데 실패했습니다.');
          // 실패 시 목록에서 가져온 데이터 사용
          setSelectedReportDetail(selectedReport);
        } finally {
          setIsLoadingReportDetail(false);
        }
      };
      fetchReportDetail();
    } else {
      setSelectedReportDetail(null);
    }
  }, [selectedReport]);

  // 로딩 중이거나 권한이 없으면 조기 반환
  if (isLoading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-center">
          <Shield className="w-12 h-12 text-red-500 mx-auto mb-4 animate-pulse" />
          <p className="text-stone-600">권한 확인 중...</p>
        </div>
      </div>
    );
  }

  const statusLabels: Record<string, string> = {
    PENDING: '대기 중',
    REVIEWING: '검토 중',
    RESOLVED: '처리 완료',
    DISMISSED: '기각',
    ACTIVE: '활성',
    INACTIVE: '정지(비활성)',
    BANNED: '정지',
    DELETED: '삭제됨',
  };

  const statusColors: Record<string, string> = {
    PENDING: 'bg-amber-100 text-amber-700',
    REVIEWING: 'bg-blue-100 text-blue-700',
    RESOLVED: 'bg-green-100 text-green-700',
    DISMISSED: 'bg-stone-100 text-stone-600',
    ACTIVE: 'bg-green-100 text-green-700',
    INACTIVE: 'bg-amber-100 text-amber-700',
    BANNED: 'bg-red-100 text-red-700',
    DELETED: 'bg-stone-200 text-stone-500',
  };

  const durationLabels: Record<SuspendDuration, string> = {
    '1day': '1일',
    '3days': '3일',
    '7days': '7일',
    '30days': '30일',
    'permanent': '영구',
  };

  const filteredReports = reports.filter(r => {
    const matchesSearch = r.targetName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.reporterName.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = filterStatus === 'all' || r.status === filterStatus;
    return matchesSearch && matchesStatus;
  });

  const filteredUsers = users.filter(u =>
    u.realName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    u.loginId.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredGroups = clubs.filter(g =>
    g.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    g.ownerName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleUpdateStatus = async (reportId: number, action: string) => {
    try {
      await processReport(reportId, action);
      // 신고 목록 새로고침
      const reportsData = await getReports(0, 100);
      setReports(reportsData.content);
      toast.success('신고가 처리되었습니다');
      setSelectedReport(null);
      setSelectedReportDetail(null);
    } catch (error) {
      console.error('신고 처리 실패:', error);
      toast.error('신고 처리에 실패했습니다');
    }
  };

  const handleSuspend = async () => {
    if (!suspendTarget || !suspendReason) {
      toast.error('정지 사유를 입력해주세요');
      return;
    }

    try {
      if (suspendTarget.type === 'user') {
        // User -> BAN
        await manageUser(suspendTarget.id, 'BAN');
        setUsers(prev => prev.map(u =>
          u.userId === suspendTarget.id ? { ...u, status: 'BANNED' } : u
        ));
      } else {
        // Group -> CLOSE (INACTIVE)
        await manageClub(suspendTarget.id, 'CLOSE');
        setClubs(prev => prev.map(g =>
          g.clubId === suspendTarget.id ? { ...g, status: 'INACTIVE' } : g
        ));
      }

      toast.success(`${suspendTarget.name}이(가) 정지 처리되었습니다`);
      setShowSuspendDialog(false);
      setSuspendTarget(null);
      setSuspendReason('');
      setSuspendDuration('7days');
    } catch (error) {
      console.error('정지 처리 실패:', error);
      toast.error('정지 처리에 실패했습니다');
    }
  };

  const handleDelete = () => {
    toast.error('삭제 기능은 현재 지원하지 않습니다. 정지 기능을 이용해주세요.');
    setShowDeleteDialog(false);
    setDeleteTarget(null);
  };

  const handleActivate = async (type: 'user' | 'group', id: string, name: string) => {
    try {
      if (type === 'user') {
        await manageUser(Number(id), 'ACTIVATE');
        setUsers(prev => prev.map(u => u.userId === Number(id) ? { ...u, status: 'ACTIVE' } : u));
      } else {
        await manageClub(Number(id), 'ACTIVATE');
        setClubs(prev => prev.map(g => g.clubId === Number(id) ? { ...g, status: 'ACTIVE' } : g));
      }
      toast.success(`${name}의 정지가 해제되었습니다`);
    } catch (error) {
      console.error('정지 해제 실패:', error);
      toast.error('정지 해제에 실패했습니다');
    }
  };

  const stats = {
    totalReports: reports.length,
    pending: reports.filter(r => r.status === 'pending').length,
    totalUsers: users.length,
    suspendedUsers: users.filter(u => u.status === 'suspended').length,
    bannedUsers: users.filter(u => u.status === 'banned').length,
    totalGroups: clubs.length,
    suspendedGroups: clubs.filter(g => g.status === 'suspended').length,
    bannedGroups: clubs.filter(g => g.status === 'banned').length,
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {activeMenu && <div className="fixed inset-0 z-40" onClick={() => setActiveMenu(null)} />}

      {/* Header */}
      <header className="sticky top-0 z-30 bg-gradient-to-r from-red-600 to-red-700 text-white">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate('/')} className="text-white hover:bg-white/20">
              <Home className="w-6 h-6" />
            </Button>
            <div className="flex items-center gap-2">
              <Shield className="w-6 h-6" />
              <div>
                <h1 className="text-lg font-bold">시스템 관리자</h1>
                <p className="text-xs text-red-100">admin@moim.com</p>
              </div>
            </div>
          </div>
          <Badge className="bg-white/20 text-white border-none">ADMIN</Badge>
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Stats */}
        <div className="grid grid-cols-2 gap-3">
          <Card className="border-amber-200 bg-amber-50">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-amber-100 rounded-full flex items-center justify-center">
                  <Flag className="w-5 h-5 text-amber-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-amber-700">{stats.pending}</p>
                  <p className="text-xs text-amber-600">대기 중 신고</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="border-red-200 bg-red-50">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
                  <Ban className="w-5 h-5 text-red-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-red-700">{stats.bannedUsers + stats.bannedGroups}</p>
                  <p className="text-xs text-red-600">영구정지</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-4 gap-2">
          <div className="bg-white rounded-xl p-3 text-center border border-stone-100">
            <p className="text-lg font-bold text-stone-900">{stats.totalUsers}</p>
            <p className="text-xs text-stone-500">전체 사용자</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center border border-stone-100">
            <p className="text-lg font-bold text-amber-600">{stats.suspendedUsers}</p>
            <p className="text-xs text-stone-500">정지 사용자</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center border border-stone-100">
            <p className="text-lg font-bold text-stone-900">{stats.totalGroups}</p>
            <p className="text-xs text-stone-500">전체 모임</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center border border-stone-100">
            <p className="text-lg font-bold text-amber-600">{stats.suspendedGroups}</p>
            <p className="text-xs text-stone-500">정지 모임</p>
          </div>
        </div>

        {/* Tabs */}
        <Tabs defaultValue="reports" className="space-y-4">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="reports" className="text-xs">
              신고 관리
              {stats.pending > 0 && (
                <Badge className="ml-1 bg-red-500 text-white text-[10px] px-1">{stats.pending}</Badge>
              )}
            </TabsTrigger>
            <TabsTrigger value="users" className="text-xs">사용자</TabsTrigger>
            <TabsTrigger value="groups" className="text-xs">모임</TabsTrigger>
          </TabsList>

          {/* Reports Tab */}
          <TabsContent value="reports" className="space-y-4">
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
                <Input
                  placeholder="검색..."
                  className="pl-10 h-11 bg-white"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
              <Select value={filterStatus} onValueChange={setFilterStatus}>
                <SelectTrigger className="w-28 h-11">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체</SelectItem>
                  <SelectItem value="PENDING">대기 중</SelectItem>
                  <SelectItem value="REVIEWING">검토 중</SelectItem>
                  <SelectItem value="RESOLVED">처리 완료</SelectItem>
                  <SelectItem value="DISMISSED">기각</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="bg-white rounded-2xl border border-stone-100">
              {filteredReports.length === 0 ? (
                <div className="p-8 text-center text-stone-500">
                  <Flag className="w-12 h-12 mx-auto mb-3 text-stone-300" />
                  <p>신고 내역이 없습니다</p>
                </div>
              ) : (
                <div className="divide-y divide-stone-100">
                  {filteredReports.map(report => (
                    <div
                      key={report.reportId}
                      className="p-4 hover:bg-stone-50 cursor-pointer"
                      onClick={() => setSelectedReport(report)}
                    >
                      <div className="flex items-start gap-3">
                        <div className="w-10 h-10 rounded-full flex items-center justify-center bg-orange-100">
                          <Flag className="w-5 h-5 text-orange-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <Badge className={`text-xs ${statusColors[report.status] || 'bg-stone-100 text-stone-600'}`}>
                              {statusLabels[report.status] || report.status}
                            </Badge>
                          </div>
                          <p className="font-medium text-stone-900 truncate">{report.targetName}</p>
                          <p className="text-sm text-stone-500">{report.reason}</p>
                          <p className="text-xs text-stone-400 mt-1">
                            신고자: {report.reporterName} · {new Date(report.createdAt).toLocaleDateString('ko-KR')}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </TabsContent>

          {/* Users Tab */}
          <TabsContent value="users" className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
              <Input
                placeholder="사용자 검색..."
                className="pl-10 h-11 bg-white"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <div className="bg-white rounded-2xl border border-stone-100">
              <div className="divide-y divide-stone-100">
                {filteredUsers.map(user => (
                  <div key={user.userId} className="p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <Avatar className="w-10 h-10">
                          <AvatarFallback>{user.realName[0]}</AvatarFallback>
                        </Avatar>
                        <div>
                          <div className="flex items-center gap-2">
                            <p className="font-medium text-stone-900">{user.realName}</p>
                            <Badge className={`text-xs ${statusColors[user.status]}`}>
                              {statusLabels[user.status]}
                            </Badge>
                          </div>
                          <p className="text-xs text-stone-500">{user.loginId}</p>
                        </div>
                      </div>

                      <div className="relative">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8"
                          onClick={() => setActiveMenu(activeMenu?.id === user.userId && activeMenu?.type === 'user' ? null : { type: 'user', id: user.userId })}
                        >
                          <MoreVertical className="w-4 h-4" />
                        </Button>

                        {activeMenu?.id === user.userId && activeMenu?.type === 'user' && (
                          <div className="absolute right-0 top-full mt-1 w-32 bg-white rounded-md shadow-lg border border-stone-200 z-50 overflow-hidden py-1">
                            {user.status === 'ACTIVE' && (
                              <button
                                className="w-full text-left px-3 py-2 text-sm text-amber-600 hover:bg-stone-50 flex items-center"
                                onClick={() => {
                                  setSuspendTarget({ type: 'user', id: user.userId, name: user.realName });
                                  setShowSuspendDialog(true);
                                  setActiveMenu(null);
                                }}
                              >
                                <PauseCircle className="w-4 h-4 mr-2" />
                                정지하기
                              </button>
                            )}
                            {(user.status === 'BANNED' || user.status === 'INACTIVE') && (
                              <button
                                className="w-full text-left px-3 py-2 text-sm text-green-600 hover:bg-stone-50 flex items-center"
                                onClick={() => {
                                  handleActivate('user', String(user.userId), user.realName);
                                  setActiveMenu(null);
                                }}
                              >
                                <PlayCircle className="w-4 h-4 mr-2" />
                                정지 해제
                              </button>
                            )}
                            <div className="h-px bg-stone-100 my-1" />
                            <button
                              className="w-full text-left px-3 py-2 text-sm text-red-600 hover:bg-stone-50 flex items-center"
                              onClick={() => {
                                setDeleteTarget({ type: 'user', id: user.userId, name: user.realName });
                                setShowDeleteDialog(true);
                                setActiveMenu(null);
                              }}
                            >
                              <Trash2 className="w-4 h-4 mr-2" />
                              계정 삭제
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </TabsContent>

          {/* Groups Tab */}
          <TabsContent value="groups" className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
              <Input
                placeholder="모임 검색..."
                className="pl-10 h-11 bg-white"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <div className="bg-white rounded-2xl border border-stone-100">
              <div className="divide-y divide-stone-100">
                {filteredGroups.map(group => (
                  <div key={group.clubId} className="p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-xl overflow-hidden bg-stone-200 flex items-center justify-center">
                          <span className="text-lg font-bold text-stone-500">{group.name[0]}</span>
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <p className="font-medium text-stone-900">{group.name}</p>
                            <Badge className={`text-xs ${statusColors[group.status]}`}>
                              {statusLabels[group.status]}
                            </Badge>
                          </div>
                          <p className="text-xs text-stone-500">모임장: {group.ownerName}</p>
                        </div>
                      </div>

                      <div className="relative">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8"
                          onClick={() => setActiveMenu(activeMenu?.id === group.clubId && activeMenu?.type === 'group' ? null : { type: 'group', id: group.clubId })}
                        >
                          <MoreVertical className="w-4 h-4" />
                        </Button>

                        {activeMenu?.id === group.clubId && activeMenu?.type === 'group' && (
                          <div className="absolute right-0 top-full mt-1 w-32 bg-white rounded-md shadow-lg border border-stone-200 z-50 overflow-hidden py-1">
                            {group.status === 'ACTIVE' && (
                              <button
                                className="w-full text-left px-3 py-2 text-sm text-amber-600 hover:bg-stone-50 flex items-center"
                                onClick={() => {
                                  setSuspendTarget({ type: 'group', id: group.clubId, name: group.name });
                                  setShowSuspendDialog(true);
                                  setActiveMenu(null);
                                }}
                              >
                                <PauseCircle className="w-4 h-4 mr-2" />
                                정지하기
                              </button>
                            )}
                            {(group.status === 'INACTIVE' || group.status === 'BANNED') && (
                              <button
                                className="w-full text-left px-3 py-2 text-sm text-green-600 hover:bg-stone-50 flex items-center"
                                onClick={() => {
                                  handleActivate('group', String(group.clubId), group.name);
                                  setActiveMenu(null);
                                }}
                              >
                                <PlayCircle className="w-4 h-4 mr-2" />
                                정지 해제
                              </button>
                            )}
                            <div className="h-px bg-stone-100 my-1" />
                            <button
                              className="w-full text-left px-3 py-2 text-sm text-red-600 hover:bg-stone-50 flex items-center"
                              onClick={() => {
                                setDeleteTarget({ type: 'group', id: group.clubId, name: group.name });
                                setShowDeleteDialog(true);
                                setActiveMenu(null);
                              }}
                            >
                              <Trash2 className="w-4 h-4 mr-2" />
                              모임 삭제
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </div>

      {/* Report Detail Dialog */}
      <AlertDialog open={!!selectedReport} onOpenChange={() => setSelectedReport(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>신고 상세</AlertDialogTitle>
            <AlertDialogDescription asChild>
              {isLoadingReportDetail ? (
                <div className="p-8 text-center text-stone-500">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500 mx-auto"></div>
                  <p className="mt-2">신고 상세 정보를 불러오는 중...</p>
                </div>
              ) : selectedReportDetail ? (
                <div className="space-y-4">
                  <div className="bg-stone-50 rounded-lg p-4 space-y-2">
                    <div className="flex justify-between">
                      <span className="text-stone-500">유형</span>
                      <span className="font-medium">신고</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-stone-500">모임</span>
                      <span className="font-medium">{selectedReportDetail.clubName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-stone-500">대상</span>
                      <span className="font-medium">{selectedReportDetail.targetName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-stone-500">신고자</span>
                      <span className="font-medium">{selectedReportDetail.reporterName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-stone-500">사유</span>
                      <span className="font-medium">{selectedReportDetail.reason}</span>
                    </div>
                    {selectedReportDetail.photoUrl && (
                      <div className="mt-2">
                        <span className="text-stone-500 text-sm block mb-1">첨부 사진</span>
                        <img
                          src={selectedReportDetail.photoUrl}
                          alt="신고 첨부 사진"
                          className="max-w-full h-auto rounded-lg border border-stone-200"
                        />
                      </div>
                    )}
                    <div className="flex justify-between mt-2 pt-2 border-t border-stone-200">
                      <span className="text-stone-500">신고 일시</span>
                      <span className="text-sm text-stone-600">{new Date(selectedReportDetail.createdAt).toLocaleString('ko-KR')}</span>
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <Button
                      className="flex-1 bg-green-500 hover:bg-green-600"
                      onClick={() => handleUpdateStatus(selectedReportDetail.reportId, 'resolved')}
                    >
                      <CheckCircle className="w-4 h-4 mr-2" />
                      처리 완료
                    </Button>
                    <Button
                      variant="outline"
                      className="flex-1"
                      onClick={() => handleUpdateStatus(selectedReportDetail.reportId, 'dismissed')}
                    >
                      <XCircle className="w-4 h-4 mr-2" />
                      기각
                    </Button>
                  </div>
                </div>
              ) : null}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>닫기</AlertDialogCancel>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Suspend Dialog */}
      <Dialog open={showSuspendDialog} onOpenChange={setShowSuspendDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <PauseCircle className="w-5 h-5 text-amber-500" />
              {suspendTarget?.type === 'user' ? '사용자' : '모임'} 정지
            </DialogTitle>
            <DialogDescription>
              {suspendTarget && `"${suspendTarget.name}"을(를) 정지합니다.`}
            </DialogDescription>
          </DialogHeader>
          <div className="py-4 space-y-4">
            <div className="space-y-3">
              <Label>정지 기간</Label>
              <RadioGroup value={suspendDuration} onValueChange={(v) => setSuspendDuration(v as SuspendDuration)}>
                <div className="grid grid-cols-3 gap-2">
                  {(['1day', '3days', '7days'] as SuspendDuration[]).map(d => (
                    <div key={d} className={`flex items-center space-x-2 border rounded-lg p-3 cursor-pointer ${suspendDuration === d ? 'border-amber-500 bg-amber-50' : 'border-stone-200'}`}>
                      <RadioGroupItem value={d} id={d} />
                      <Label htmlFor={d} className="cursor-pointer">{durationLabels[d]}</Label>
                    </div>
                  ))}
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div className={`flex items-center space-x-2 border rounded-lg p-3 cursor-pointer ${suspendDuration === '30days' ? 'border-amber-500 bg-amber-50' : 'border-stone-200'}`}>
                    <RadioGroupItem value="30days" id="30days" />
                    <Label htmlFor="30days" className="cursor-pointer">30일</Label>
                  </div>
                  <div className={`flex items-center space-x-2 border rounded-lg p-3 cursor-pointer ${suspendDuration === 'permanent' ? 'border-red-500 bg-red-50' : 'border-stone-200'}`}>
                    <RadioGroupItem value="permanent" id="permanent" />
                    <Label htmlFor="permanent" className="cursor-pointer text-red-600">영구정지</Label>
                  </div>
                </div>
              </RadioGroup>
            </div>
            <div className="space-y-2">
              <Label>정지 사유</Label>
              <Textarea
                placeholder="정지 사유를 입력하세요"
                value={suspendReason}
                onChange={(e) => setSuspendReason(e.target.value)}
                className="min-h-[80px]"
              />
            </div>
            {suspendDuration === 'permanent' && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                <p className="text-xs text-red-700">
                  ⚠️ 영구정지는 해당 {suspendTarget?.type === 'user' ? '사용자' : '모임'}의 모든 활동을 완전히 차단합니다.
                </p>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowSuspendDialog(false)}>
              취소
            </Button>
            <Button
              onClick={handleSuspend}
              className={suspendDuration === 'permanent' ? 'bg-red-500 hover:bg-red-600' : 'bg-amber-500 hover:bg-amber-600'}
            >
              {suspendDuration === 'permanent' ? '영구정지' : '정지하기'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center gap-2 text-red-600">
              <Trash2 className="w-5 h-5" />
              {deleteTarget?.type === 'user' ? '계정' : '모임'} 삭제
            </AlertDialogTitle>
            <AlertDialogDescription>
              {deleteTarget && (
                <div className="space-y-3">
                  <p>
                    <span className="font-medium text-stone-900">"{deleteTarget.name}"</span>을(를)
                    완전히 삭제하시겠습니까?
                  </p>
                  <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                    <p className="text-xs text-red-700">
                      ⚠️ 이 작업은 되돌릴 수 없습니다. 모든 관련 데이터가 영구적으로 삭제됩니다.
                    </p>
                  </div>
                </div>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-red-500 hover:bg-red-600">
              삭제하기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
