package back.controller.post.rag;

import back.dto.post.rag.RagAnswerResponse;
import back.service.club.ClubAuthService;
import back.service.post.ai.PostSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts/rag")
@RequiredArgsConstructor
public class PostSearchController {

    private final ClubAuthService clubAuthService;
    private final PostSearchService postSearchService;

    // 1. 벡터 검색 결과만 (디버깅용)
    @GetMapping("/search")
    public ResponseEntity<List<PostSearchService.SearchHit>> search(
            @RequestParam Long clubId,
            @RequestParam Long userId,
            @RequestParam String question
    ) {
        clubAuthService.validateAndGetClubForReadPosts(clubId, userId);
        return ResponseEntity.ok(
                postSearchService.searchHits(question)
        );
    }

    // 2. RAG 답변 (실사용)
    @GetMapping("/answer")
    public ResponseEntity<RagAnswerResponse> answer(
            @RequestParam Long clubId,
            @RequestParam Long userId,
            @RequestParam String question
    ) {
        clubAuthService.validateAndGetClubForReadPosts(clubId, userId);
        String answer = postSearchService.answerWithRag(question);
        return ResponseEntity.ok(new RagAnswerResponse(answer));
    }

}
