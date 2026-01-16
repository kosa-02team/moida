import { UserRole } from './userRoles';

export type ManagementType = 'operating' | 'fair';

export interface GroupAccount {
  managementType: ManagementType;
  totalBalance: number;      // 총 잔액
  memberCount: number;       // 현재 멤버 수
  // 공정정산형 전용
  perPersonShare?: number;   // 1인당 지분 (totalBalance / memberCount, 모두 동일)
  entryFee?: number;         // 신규 가입비 (= perPersonShare)
  // 운영비형 전용
  monthlyFee?: number;       // 월 회비
  totalDeposited?: number;   // 총 입금액
  totalUsed?: number;        // 총 사용액
}

export interface Group {
  id: string;
  name: string;
  description: string;
  image: string;
  tags: string[];
  memberCount: number;
  maxMembers: number;
  type: 'club' | 'meetup' | 'study';
  isPublic: boolean;
  myRole: UserRole;
  myRoles?: UserRole[];  // 복수 역할 (운영진+총무 등)
  account: GroupAccount; // 통장 정보
  nextEvent?: {
    title: string;
    date: string;
    location: string;
  };
}

// 공정정산형: 1인당 지분 계산 (항상 totalBalance / memberCount)
export function calculatePerPersonShare(totalBalance: number, memberCount: number): number {
  if (memberCount === 0) return 0;
  return Math.floor(totalBalance / memberCount);
}

// 공정정산형: 신규 가입비 = 현재 1인당 지분
export function calculateEntryFee(group: Group): number {
  if (group.account.managementType !== 'fair') return 0;
  return group.account.perPersonShare || 0;
}

// 공정정산형: 탈퇴 시 환불금 = 현재 1인당 지분
export function calculateRefundAmount(group: Group): number {
  if (group.account.managementType !== 'fair') return 0; // 운영비형은 환불 X
  return group.account.perPersonShare || 0;
}

export const MOCK_GROUPS: Group[] = [
  // ===== 주말 등산 클럽 (공정정산형) - 모임장 =====
  {
    id: '1',
    name: '주말 등산 클럽',
    description: '매주 토요일 서울 근교 산행합니다. 초보자도 환영!',
    image: 'https://images.unsplash.com/photo-1551632811-561732d1e306?w=500&auto=format&fit=crop&q=60',
    tags: ['등산', '운동', '친목'],
    memberCount: 10,
    maxMembers: 50,
    type: 'club',
    isPublic: true,
    myRole: 'owner', // 모임장
    account: {
      managementType: 'fair', // 공정정산형
      totalBalance: 500000,   // 50만원
      memberCount: 10,
      perPersonShare: 50000,  // 1인당 5만원 (500,000 / 10명)
      entryFee: 50000,        // 신규 가입비 = 1인당 지분
    },
    nextEvent: {
      title: '관악산 정기 산행',
      date: '2025-04-12',
      location: '사당역 4번 출구'
    }
  },
  // ===== 강남 독서 모임 (운영비형) - 총무 =====
  {
    id: '2',
    name: '강남 독서 모임',
    description: '매월 1권의 책을 선정해 토론하는 독서 모임입니다.',
    image: 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500&auto=format&fit=crop&q=60',
    tags: ['독서', '자기계발', '토론'],
    memberCount: 8,
    maxMembers: 15,
    type: 'study',
    isPublic: false,
    myRole: 'treasurer', // 총무
    account: {
      managementType: 'operating', // 운영비형 - 환불 X, 축적형
      totalBalance: 320000,
      memberCount: 8,
      monthlyFee: 20000,       // 월 2만원
      totalDeposited: 480000,  // 총 48만원 입금
      totalUsed: 160000,       // 총 16만원 사용
    },
    nextEvent: {
      title: '4월 독서 토론: 데미안',
      date: '2025-04-20',
      location: '강남 스터디카페'
    }
  },
  // ===== 개발자 네트워킹 (공정정산형) - 운영진+총무 =====
  {
    id: '3',
    name: '개발자 네트워킹',
    description: 'IT 업계 종사자들의 네트워킹 모임입니다.',
    image: 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=500&auto=format&fit=crop&q=60',
    tags: ['개발', 'IT', '네트워킹'],
    memberCount: 20,
    maxMembers: 30,
    type: 'meetup',
    isPublic: true,
    myRole: 'manager', // 운영진
    myRoles: ['manager', 'treasurer'], // 운영진 + 총무 권한 동시 보유
    account: {
      managementType: 'fair', // 공정정산형
      totalBalance: 600000,   // 60만원
      memberCount: 20,
      perPersonShare: 30000,  // 1인당 3만원 (600,000 / 20명)
      entryFee: 30000,
    },
    nextEvent: {
      title: '4월 개발자 밋업',
      date: '2025-04-25',
      location: '강남역 위워크'
    }
  },
  // ===== 요가 & 명상 클럽 (운영비형) - 일반 회원 =====
  {
    id: '4',
    name: '요가 & 명상 클럽',
    description: '바쁜 일상 속 마음의 평화를 찾아요.',
    image: 'https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=500&auto=format&fit=crop&q=60',
    tags: ['요가', '명상', '건강'],
    memberCount: 12,
    maxMembers: 20,
    type: 'club',
    isPublic: true,
    myRole: 'member', // 일반 회원
    account: {
      managementType: 'operating', // 운영비형
      totalBalance: 180000,
      memberCount: 12,
      monthlyFee: 15000,       // 월 1.5만원
      totalDeposited: 360000,  // 총 36만원 입금
      totalUsed: 180000,       // 총 18만원 사용
    },
    nextEvent: {
      title: '일요일 모닝 요가',
      date: '2025-04-14',
      location: '한강공원 여의도지구'
    }
  },
  // ===== 영어 회화 스터디 (공정정산형) - 일반 회원 =====
  {
    id: '5',
    name: '영어 회화 스터디',
    description: '영어로 자유롭게 대화하는 스터디입니다.',
    image: 'https://images.unsplash.com/photo-1543109740-4bdb38fda756?w=500&auto=format&fit=crop&q=60',
    tags: ['영어', '회화', '스터디'],
    memberCount: 6,
    maxMembers: 8,
    type: 'study',
    isPublic: false,
    myRole: 'member', // 일반 회원
    account: {
      managementType: 'fair', // 공정정산형
      totalBalance: 180000,   // 18만원
      memberCount: 6,
      perPersonShare: 30000,  // 1인당 3만원 (180,000 / 6명)
      entryFee: 30000,
    },
    nextEvent: {
      title: '토요일 프리토킹',
      date: '2025-04-13',
      location: '홍대 스터디카페'
    }
  },
  // ===== 주말 러닝 크루 (운영비형) - 운영진 =====
  {
    id: '6',
    name: '주말 러닝 크루',
    description: '함께 뛰면 더 즐거워요! 러닝 크루 멤버 모집',
    image: 'https://images.unsplash.com/photo-1571008887538-b36bb32f4571?w=500&auto=format&fit=crop&q=60',
    tags: ['러닝', '운동', '건강'],
    memberCount: 30,
    maxMembers: 50,
    type: 'club',
    isPublic: true,
    myRole: 'manager', // 운영진
    account: {
      managementType: 'operating', // 운영비형
      totalBalance: 450000,
      memberCount: 30,
      monthlyFee: 10000,       // 월 1만원
      totalDeposited: 750000,  // 총 75만원 입금
      totalUsed: 300000,       // 총 30만원 사용
    },
    nextEvent: {
      title: '서울숲 10km 러닝',
      date: '2025-04-13',
      location: '서울숲 입구'
    }
  }
];

// 모임 정보 가져오기
export function getGroupById(groupId: string): Group | undefined {
  return MOCK_GROUPS.find(g => g.id === groupId);
}

// 통장 유형 설명
export const MANAGEMENT_TYPE_INFO = {
  fair: {
    name: '공정정산형',
    description: '모든 멤버가 항상 동일한 지분을 가집니다',
    features: [
      '모든 멤버 동일 지분 (총액 ÷ 인원수)',
      '신규 가입 시: 현재 1인당 지분만큼 납부',
      '탈퇴 시: 1인당 지분만큼 환불',
      '인원 변동에도 공정한 정산 가능',
    ],
    color: 'green',
  },
  operating: {
    name: '운영비형',
    description: '회비가 축적되어 운영비로 사용됩니다',
    features: [
      '탈퇴 시 환불 없음',
      '남은 돈 계속 축적',
      '운영비로 자유롭게 사용',
      '안정적인 모임에 적합',
    ],
    color: 'blue',
  },
};
