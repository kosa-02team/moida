/**
 * Post/Story API
 */

import { get, post, put, del } from './client';

// ==================== Types ====================

export interface PostCardResponse {
  clubId: number;
  postId: number;
  writerId: number;
  writerName: string;
  title: string;
  content: string;
  imagesUrl: string[];
  postLikes: number;
  commentCount: number;
  createdAt: string;
  scheduleId?: number | null;
  isLiked?: boolean;
}

export interface PostDetailResponse {
  // 백엔드 DTO 필수 필드
  postId: number;
  clubId: number;
  writerId: number;
  category: string;
  title: string;
  content: string;
  scheduleId: number | null;
  place: string | null;
  createdAt: string;
  updatedAt: string;
  imagesUrl: string[]; // 백엔드에서 이제 포함됨
  // 아래 필드들은 백엔드 DTO에 없으므로 optional로 처리 (별도 조회 필요)
  writerName?: string;
  writerProfileImageUrl?: string | null;
  postLikes?: number;
  isLiked?: boolean;
  isMyPost?: boolean;
  taggedMemberIds?: number[];
}

export interface StoryCreateRequest {
  scheduleId?: number | null;
  content?: string | null;
  title?: string | null;
  imagesUrl?: string[];
  place?: string | null;
  taggedMemberIds?: number[];
  // 투표 관련 필드
  voteOptions?: Array<{
    optionText: string;
    order: number;
    eventDate?: string | null;
    location?: string | null;
  }>;
  voteDeadline?: string | null;
  isAnonymous?: boolean;
  allowMultiple?: boolean;
}

export interface StoryUpdateRequest {
  content?: string | null;
  imagesUrl?: string[] | null;
  place?: string | null;
  taggedMemberIds?: number[] | null;
}

export interface PostIdResponse {
  postId: number;
}

export interface AlbumCardResponse {
  clubId: number;
  postId: number;
  scheduleId: number | null;
  scheduleName: string | null;
  coverImageUrl: string | null;
  imageCount: number;
  lastCreatedAt: string;
}

// ==================== API Functions ====================

export const getRecentPosts = async (
  clubId: number,
  page: number = 0,
  size: number = 20
): Promise<PostCardResponse[]> => {
  const url = `/api/clubs/${clubId}/posts/recent?page=${page}&size=${size}`;
  return get<PostCardResponse[]>(url);
};

export const getPost = async (
  clubId: number,
  postId: number
): Promise<PostDetailResponse> => {
  const url = `/api/clubs/${clubId}/posts/${postId}`;
  return get<PostDetailResponse>(url);
};

export const getRecentAlbums = async (
  clubId: number,
  limit: number = 2
): Promise<AlbumCardResponse[]> => {
  const url = `/api/clubs/${clubId}/posts/albums/recent?limit=${limit}`;
  return get<AlbumCardResponse[]>(url);
};

export const createStory = async (
  clubId: number,
  request: StoryCreateRequest
): Promise<PostIdResponse> => {
  const url = `/api/clubs/${clubId}/posts`;
  return post<PostIdResponse>(url, request);
};

export const updatePost = async (
  clubId: number,
  postId: number,
  request: StoryUpdateRequest
): Promise<PostIdResponse> => {
  const url = `/api/clubs/${clubId}/posts/${postId}`;
  return put<PostIdResponse>(url, request);
};

export const deletePost = async (
  clubId: number,
  postId: number
): Promise<void> => {
  const url = `/api/clubs/${clubId}/posts/${postId}`;
  return del<void>(url);
};

export const blindPost = async (
  clubId: number,
  postId: number
): Promise<void> => {
  const url = `/api/clubs/${clubId}/posts/${postId}/blind`;
  return put<void>(url);
};

export const likePost = async (
  clubId: number,
  postId: number
): Promise<void> => {
  const url = `/api/clubs/${clubId}/posts/${postId}/likes`;
  return post<void>(url);
};

export const unlikePost = async (
  clubId: number,
  postId: number
): Promise<void> => {
  const url = `/api/clubs/${clubId}/posts/${postId}/likes`;
  return del<void>(url);
};
