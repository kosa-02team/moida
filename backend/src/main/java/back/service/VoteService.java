package back.service;

import back.dto.VoteCreateRequest;
import back.dto.VoteResponse;

public interface VoteService {

    /**
     * 모임에 속한 일정/참석 투표를 생성합니다.
     *
     * @param clubId  투표가 속한 모임 ID
     * @param userId  투표 생성자(현재 로그인 유저) ID
     * @param request 투표 생성 요청 정보
     * @return 생성된 투표 정보
     */
    VoteResponse createVote(Long clubId, Long userId, VoteCreateRequest request);
}
