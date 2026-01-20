/**
 * 신고 관련 API
 */

import { post } from './client';

export interface ReportCreateRequest {
  targetId: number;
  reason: string;
  photoUrl?: string | null;
}

/**
 * 신고 생성
 */
export const createReport = async (
  clubId: number,
  request: ReportCreateRequest
): Promise<number> => {
  const response = await post<number>(`/api/clubs/${clubId}/reports`, request);
  return response;
};
