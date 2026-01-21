// 권한 관련 타입
export type MemberRole = 'owner' | 'treasurer' | 'manager' | 'member' | 'pending';

export interface RolePermissions {
  canManageGroup: boolean;        // 모임 정보 수정
  canManageMembers: boolean;      // 멤버 관리 (강퇴, 승인)
  canManageRoles: boolean;        // 권한 부여/회수
  canManageDues: boolean;         // 회비 정책 관리
  canDeposit: boolean;            // 회비 입금 (채우기)
  canWithdraw: boolean;           // 회비 출금 (보내기)
  canRequestSettlement: boolean;  // 정산 요청
  canApproveSettlement: boolean;  // 정산 승인
  canCreateSchedule: boolean;     // 일정 생성
  canDeleteSchedule: boolean;     // 일정 삭제
  canCreateStory: boolean;        // 스토리 작성
  canDeleteStory: boolean;        // 스토리 삭제 (본인 외)
  canVote: boolean;               // 투표
  canViewDues: boolean;           // 회비 현황 조회
}

export const ROLE_PERMISSIONS: Record<MemberRole, RolePermissions> = {
  owner: {
    canManageGroup: true,
    canManageMembers: true,
    canManageRoles: true,
    canManageDues: true,
    canDeposit: true,
    canWithdraw: true,
    canRequestSettlement: true,
    canApproveSettlement: true,
    canCreateSchedule: true,
    canDeleteSchedule: true,
    canCreateStory: true,
    canDeleteStory: true,
    canVote: true,
    canViewDues: true,
  },
  treasurer: {
    canManageGroup: false,
    canManageMembers: false,
    canManageRoles: false,
    canManageDues: true,
    canDeposit: true,
    canWithdraw: true,
    canRequestSettlement: true,
    canApproveSettlement: true,
    canCreateSchedule: false,
    canDeleteSchedule: false,
    canCreateStory: true,
    canDeleteStory: false,
    canVote: true,
    canViewDues: true,
  },
  manager: {
    canManageGroup: false,
    canManageMembers: true,
    canManageRoles: false,
    canManageDues: false,
    canDeposit: true,
    canWithdraw: false,
    canRequestSettlement: true,
    canApproveSettlement: false,
    canCreateSchedule: true,
    canDeleteSchedule: true,
    canCreateStory: true,
    canDeleteStory: true,
    canVote: true,
    canViewDues: true,
  },
  member: {
    canManageGroup: false,
    canManageMembers: false,
    canManageRoles: false,
    canManageDues: false,
    canDeposit: true,
    canWithdraw: false,
    canRequestSettlement: true,
    canApproveSettlement: false,
    canCreateSchedule: false,
    canDeleteSchedule: false,
    canCreateStory: true,
    canDeleteStory: false,
    canVote: true,
    canViewDues: true,
  },
  pending: {
    canManageGroup: false,
    canManageMembers: false,
    canManageRoles: false,
    canManageDues: false,
    canDeposit: false,
    canWithdraw: false,
    canRequestSettlement: false,
    canApproveSettlement: false,
    canCreateSchedule: false,
    canDeleteSchedule: false,
    canCreateStory: false,
    canDeleteStory: false,
    canVote: false,
    canViewDues: false,
  },
};

export const ROLE_LABELS: Record<MemberRole, string> = {
  owner: '모임장',
  treasurer: '총무',
  manager: '운영진',
  member: '회원',
  pending: '가입대기',
};

export const ROLE_COLORS: Record<MemberRole, string> = {
  owner: 'bg-purple-100 text-purple-700',
  treasurer: 'bg-green-100 text-green-700',
  manager: 'bg-blue-100 text-blue-700',
  member: 'bg-stone-100 text-stone-700',
  pending: 'bg-yellow-100 text-yellow-700',
};

// 모임 공개 설정 타입
export type GroupVisibility = 'private' | 'searchable' | 'public';

export interface GroupPrivacySettings {
  visibility: GroupVisibility;          // 공개 범위
  allowSearch: boolean;                  // 검색 허용
  showPostsToNonMembers: boolean;       // 비회원에게 게시글 공개
  showMembersToNonMembers: boolean;     // 비회원에게 멤버 목록 공개
  requireApproval: boolean;              // 가입 승인 필요
  allowInviteCode: boolean;              // 초대 코드 사용
}

export const DEFAULT_PRIVACY_SETTINGS: GroupPrivacySettings = {
  visibility: 'searchable',
  allowSearch: true,
  showPostsToNonMembers: false,
  showMembersToNonMembers: false,
  requireApproval: true,
  allowInviteCode: true,
};

// 모임 관련 타입 (확장)
export interface Group {
  id: string;
  name: string;
  image: string;
  description?: string;
  tags: string[];
  memberCount: number;
  maxMembers: number;
  type: 'club' | 'meetup' | 'study';
  isPublic: boolean;
  privacySettings: GroupPrivacySettings;
  nextEvent?: {
    title: string;
    date: string;
    location: string;
  };
  createdAt?: string;
  createdBy?: string;
}

// 멤버 관련 타입 (확장)
export interface Member {
  id: string;
  name: string;
  avatar?: string;
  email?: string;
  phone?: string;
  role: MemberRole;
  joinedDate: string;
  duesStatus: 'paid' | 'unpaid' | 'overdue';
  totalPaid?: number;
  lastPaymentDate?: string;
}

// 회비/거래 관련 타입 (확장)
export type TransactionType = 'deposit' | 'withdraw' | 'settlement';

export interface Transaction {
  id: number;
  title: string;
  amount: number;
  date: string;
  type: 'income' | 'expense';
  transactionType?: TransactionType;
  category?: string;
  member?: string;
  memberId?: string;
  approvedBy?: string;
  receipt?: string;
  note?: string;
}

export interface DuesPolicy {
  amount: number;
  paymentDay: number;
  allowMultiplePayments: boolean;
  autoNotification: boolean;
  notificationDay: number;
  gracePeriod: number;
}

// 정산 관련 타입 (확장)
export interface SettlementItem {
  id: string;
  title: string;
  amount: number;
  date: string;
  receipt?: string;
}

export interface Settlement {
  id: string;
  title: string;
  date: string;
  description?: string;
  items: SettlementItem[];
  totalAmount: number;
  status: 'pending' | 'approved' | 'rejected';
  createdBy: string;
  createdAt: string;
  targetMembers?: string[];  // 정산 대상자 목록
  perPersonAmount?: number;  // 1인당 정산 금액
}

// 일정 관련 타입
export interface Schedule {
  id: string;
  title: string;
  date: string;
  time: string;
  location: string;
  description?: string;
  attendees: string[];
  maxAttendees?: number;
  status: 'upcoming' | 'ongoing' | 'completed' | 'cancelled';
}

export interface Vote {
  id: string;
  title: string;
  options: VoteOption[];
  deadline: string;
  isMultipleChoice: boolean;
  status: 'open' | 'closed';
}

export interface VoteOption {
  id: string;
  label: string;
  votes: string[];
}

// 알림 관련 타입
export interface Notification {
  id: string;
  type: 'member' | 'schedule' | 'dues' | 'general';
  title: string;
  message: string;
  time: string;
  isRead: boolean;
  groupId?: string;
  groupName?: string;
}

// 스토리/앨범 관련 타입 (확장)
export interface Album {
  id: string;
  title: string;
  cover: string;
  count: number;
  date: string;
  description?: string;
  eventId?: string;  // 연결된 일정
}

export interface Story {
  id: string;
  image: string;
  images?: string[];
  caption?: string;
  content?: string;
  author: {
    id: string;
    name: string;
    avatar?: string;
  };
  albumId?: string;
  location?: string;
  taggedMembers?: string[];
  createdAt: string;
  likes: number;
  comments: number;
}

// 참여 통계 타입
export interface ParticipationStats {
  memberId: string;
  memberName: string;
  avatar?: string;
  totalEvents: number;
  attendedEvents: number;
  attendanceRate: number;
  recentAttendance: boolean[];  // 최근 5개 일정 참석 여부
}

// API 응답 타입
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
  };
}

// 페이지네이션 타입
export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  hasNext: boolean;
  hasPrev: boolean;
}
