/**
 * 투표(Vote) 관련 API
 */

import { get, post } from './client';

export interface VoteListResponse {
  voteId: number;
  postId?: number;
  voteType: string;
  scheduleId?: number;
  title: string;
  status: string;
  deadline?: string;
  closedAt?: string;
  createdAt: string;
  totalVoteCount: number;
}

export interface VoteDetailResponse {
  voteId: number;
  postId?: number;
  voteType: string;
  scheduleId?: number;
  creatorId: number;
  title: string;
  description?: string;
  isAnonymous: boolean;
  allowMultiple: boolean;
  deadline?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  options: VoteOptionResponse[];
}

export interface VoteOptionResponse {
  optionId: number;
  voteId: number;
  optionText: string;
  order: number;
  eventDate?: string;
  location?: string;
  voteCount?: number;
  voters?: { userId: number; realName: string; profileImageUrl?: string }[];
}

export interface VoteCreateRequest {
  voteType: string;
  scheduleId?: number;
  title: string;
  description?: string;
  isAnonymous: boolean;
  allowMultiple: boolean;
  deadline?: string;
  options: VoteOptionCreateRequest[];
}

export interface VoteOptionCreateRequest {
  optionText: string;
  order: number;
  eventDate?: string;
  location?: string;
}

export interface VoteAnswerRequest {
  optionIds: number[];
}

export interface VoteResponse {
  voteId: number;
  voteType: string;
  title: string;
  description?: string;
  isAnonymous: boolean;
  allowMultiple: boolean;
  deadline?: string;
  status: string;
  createdAt: string;
}

/**
 * 모임의 투표 목록 조회
 */
export const getVotes = async (clubId: number): Promise<VoteListResponse[]> => {
  return get<VoteListResponse[]>(`/api/clubs/${clubId}/votes`);
};

/**
 * 단일 투표 상세 조회
 */
export const getVote = async (
  clubId: number,
  voteId: number
): Promise<VoteDetailResponse> => {
  return get<VoteDetailResponse>(`/api/clubs/${clubId}/votes/${voteId}`);
};

/**
 * 투표 생성
 */
export const createVote = async (
  clubId: number,
  request: VoteCreateRequest
): Promise<VoteResponse> => {
  return post<VoteResponse>(`/api/clubs/${clubId}/votes`, request);
};

/**
 * 투표 종료
 */
export const closeVote = async (
  clubId: number,
  voteId: number
): Promise<void> => {
  return post<void>(`/api/clubs/${clubId}/votes/${voteId}/close`);
};

/**
 * 투표 참여
 */
export const answerVote = async (
  clubId: number,
  voteId: number,
  request: VoteAnswerRequest
): Promise<void> => {
  return post<void>(`/api/clubs/${clubId}/votes/${voteId}/answers`, request);
};
