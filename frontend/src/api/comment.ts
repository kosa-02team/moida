/**
 * 댓글(Comment) 관련 API
 */

import { get, post, patch, del } from './client';

// ==================== Types ====================

export interface PostCommentRequest {
  content: string;
}

export interface PostCommentsIdResponse {
  commentId: number;
}

export interface PostCommentsResponse {
  comments: PostCommentItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface PostCommentItem {
  commentId: number;
  writerId: number;
  content: string;
  createdAt: string;
  likeCount?: number;
  isLiked?: boolean;
}

// ==================== API Functions ====================

/**
 * 댓글 목록 조회
 */
export const getPostComments = async (
  clubId: number,
  postId: number,
  page: number = 0,
  size: number = 20
): Promise<PostCommentsResponse> => {
  const url = `/api/club/${clubId}/posts/${postId}/comments?page=${page}&size=${size}`;
  return get<PostCommentsResponse>(url);
};

/**
 * 댓글 생성
 */
export const createComment = async (
  clubId: number,
  postId: number,
  request: PostCommentRequest
): Promise<PostCommentsIdResponse> => {
  const url = `/api/club/${clubId}/posts/${postId}/comments`;
  return post<PostCommentsIdResponse>(url, request);
};

/**
 * 댓글 수정
 */
export const updateComment = async (
  clubId: number,
  postId: number,
  commentId: number,
  request: PostCommentRequest
): Promise<PostCommentsIdResponse> => {
  const url = `/api/club/${clubId}/posts/${postId}/comments/${commentId}`;
  return patch<PostCommentsIdResponse>(url, request);
};

/**
 * 댓글 삭제
 */
export const deleteComment = async (
  clubId: number,
  postId: number,
  commentId: number
): Promise<PostCommentsIdResponse> => {
  const url = `/api/club/${clubId}/posts/${postId}/comments/${commentId}`;
  return del<PostCommentsIdResponse>(url);
};

/**
 * 댓글 좋아요
 */
export const likeComment = async (
  clubId: number,
  postId: number,
  commentId: number
): Promise<void> => {
  const url = `/api/club/${clubId}/posts/${postId}/comments/${commentId}/likes`;
  return post<void>(url);
};

/**
 * 댓글 좋아요 취소
 */
export const unlikeComment = async (
  clubId: number,
  postId: number,
  commentId: number
): Promise<void> => {
  const url = `/api/club/${clubId}/posts/${postId}/comments/${commentId}/likes`;
  return del<void>(url);
};
