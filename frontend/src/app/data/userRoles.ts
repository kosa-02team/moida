export type UserRole = 'owner' | 'treasurer' | 'manager' | 'member' | 'pending';

export const ROLE_LABELS: Record<UserRole, string> = {
  owner: '모임장',
  treasurer: '총무',
  manager: '운영진',
  member: '회원',
  pending: '가입대기',
};

export const ROLE_COLORS: Record<UserRole, string> = {
  owner: 'bg-orange-500 text-white',
  treasurer: 'bg-green-500 text-white',
  manager: 'bg-blue-500 text-white',
  member: 'bg-stone-500 text-white',
  pending: 'bg-yellow-500 text-white',
};

// 역할별 권한 정의
export const ROLE_PERMISSIONS = {
  owner: {
    canManageGroup: true,      // 모임 설정 관리
    canManageDues: true,       // 회비 관리 (입금/출금)
    canWithdraw: true,         // 출금 가능
    canManageShares: true,     // 지분 관리
    canManageMembers: true,    // 멤버 관리
    canDeletePosts: true,      // 게시글 삭제
    canDeleteComments: true,   // 댓글 삭제
    canFinalizeSchedule: true, // 일정 마무리
    canChangeManagementType: true, // 통장 유형 변경
    canAssignRoles: true,      // 역할 부여
  },
  treasurer: {
    canManageGroup: false,
    canManageDues: true,
    canWithdraw: true,
    canManageShares: true,
    canManageMembers: false,
    canDeletePosts: false,
    canDeleteComments: false,
    canFinalizeSchedule: true,
    canChangeManagementType: true,
    canAssignRoles: false,
  },
  manager: {
    canManageGroup: true,
    canManageDues: false,
    canWithdraw: false,
    canManageShares: false,
    canManageMembers: true,
    canDeletePosts: true,
    canDeleteComments: true,
    canFinalizeSchedule: true,
    canChangeManagementType: false,
    canAssignRoles: false,
  },
  member: {
    canManageGroup: false,
    canManageDues: false,
    canWithdraw: false,
    canManageShares: false,
    canManageMembers: false,
    canDeletePosts: false,
    canDeleteComments: false,
    canFinalizeSchedule: false,
    canChangeManagementType: false,
    canAssignRoles: false,
  },
  pending: {
    canManageGroup: false,
    canManageDues: false,
    canWithdraw: false,
    canManageShares: false,
    canManageMembers: false,
    canDeletePosts: false,
    canDeleteComments: false,
    canFinalizeSchedule: false,
    canChangeManagementType: false,
    canAssignRoles: false,
  },
};

// 각 모임별 사용자 역할 (복수 역할 지원)
// groupId -> roles 배열
// ★★★ mockData.ts의 myRole과 일치해야 함 ★★★
const MOCK_USER_ROLES: Record<string, UserRole[]> = {
  '1': ['owner'],                  // 주말 등산 클럽 - 모임장
  '2': ['treasurer'],              // 강남 독서 모임 - 총무
  '3': ['manager', 'treasurer'],   // 개발자 네트워킹 - 운영진+총무 (복합 권한)
  '4': ['member'],                 // 요가 & 명상 클럽 - 일반 회원
  '5': ['member'],                 // 영어 회화 스터디 - 일반 회원
  '6': ['manager'],                // 주말 러닝 크루 - 운영진 ★ mockData와 일치
  'public1': ['member'],
  'public2': ['member'],
};

// 사용자의 주 역할 가져오기 (표시용)
export function useUserRole(groupId: string): { userRole: UserRole; allRoles: UserRole[] } {
  const roles = MOCK_USER_ROLES[groupId] || ['member'];
  // 주 역할은 우선순위: owner > treasurer > manager > member
  const primaryRole: UserRole = roles.includes('owner') ? 'owner' :
                     roles.includes('treasurer') ? 'treasurer' :
                     roles.includes('manager') ? 'manager' : 'member';
  return { userRole: primaryRole, allRoles: roles };
}

// 사용자가 특정 권한을 가지고 있는지 확인
export function useUserPermissions(groupId: string) {
  const { allRoles } = useUserRole(groupId);
  
  // 모든 역할의 권한을 합산
  const permissions = {
    canManageGroup: false,
    canManageDues: false,
    canWithdraw: false,
    canManageShares: false,
    canManageMembers: false,
    canDeletePosts: false,
    canDeleteComments: false,
    canFinalizeSchedule: false,
    canChangeManagementType: false,
    canAssignRoles: false,
  };

  for (const role of allRoles) {
    const rolePerms = ROLE_PERMISSIONS[role];
    for (const key of Object.keys(permissions) as (keyof typeof permissions)[]) {
      if (rolePerms[key]) {
        permissions[key] = true;
      }
    }
  }

  return permissions;
}

// 역할 표시 라벨 (복합 역할)
export function getRoleLabel(groupId: string): string {
  const roles = MOCK_USER_ROLES[groupId] || ['member'];
  if (roles.includes('owner')) return '모임장';
  if (roles.includes('treasurer') && roles.includes('manager')) return '운영진+총무';
  if (roles.includes('treasurer')) return '총무';
  if (roles.includes('manager')) return '운영진';
  return '회원';
}

// 역할 색상 (복합 역할)
export function getRoleColor(groupId: string): string {
  const roles = MOCK_USER_ROLES[groupId] || ['member'];
  if (roles.includes('owner')) return 'bg-orange-500 text-white';
  if (roles.includes('treasurer') && roles.includes('manager')) return 'bg-purple-500 text-white';
  if (roles.includes('treasurer')) return 'bg-green-500 text-white';
  if (roles.includes('manager')) return 'bg-blue-500 text-white';
  return 'bg-stone-500 text-white';
}

// 역할 설명
export const ROLE_DESCRIPTIONS: Record<UserRole, string> = {
  owner: '모든 권한을 가진 모임 관리자입니다.',
  treasurer: '회비 관리, 지분 확인, 출금이 가능합니다.',
  manager: '멤버 관리, 게시글/댓글 삭제가 가능합니다.',
  member: '일반적인 모임 활동이 가능합니다.',
  pending: '가입 승인 대기 중입니다.',
};

// 역할 배지 정보
export function getRoleBadgeInfo(groupId: string) {
  const roles = MOCK_USER_ROLES[groupId] || ['member'];
  return {
    label: getRoleLabel(groupId),
    color: getRoleColor(groupId),
    roles,
    isMultiRole: roles.length > 1,
  };
}
