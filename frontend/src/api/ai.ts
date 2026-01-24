/**
 * AI API
 */

import { get } from './client';

// ==================== Types ====================

export interface RagAnswerResponse {
  answer: string;
}

// ==================== API Functions ====================

export const askAI = async (
  clubId: number,
  userId: number,
  question: string
): Promise<RagAnswerResponse> => {
  const url = `/api/posts/rag/answer?clubId=${clubId}&userId=${userId}&question=${encodeURIComponent(question)}`;
  return get<RagAnswerResponse>(url);
};
