package back.service.post.ai;

import back.domain.post.Posts;
import back.repository.post.PostMemberTagRepository;
import back.repository.post.PostRepository;
import back.service.post.ai.chroma.ChromaCollectionHolder;
import back.service.post.ai.gemini.GeminiChatClient;
import back.service.post.ai.gemini.embedding.EmbeddingCache;
import back.service.post.ai.gemini.embedding.GeminiEmbeddingClient;
import back.service.post.ai.gemini.prompt.RagAnswerPrompt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final WebClient chromaWebClient;
    private final GeminiEmbeddingClient geminiEmbeddingClient;
    private final GeminiChatClient geminiChatClient;
    private final ChromaCollectionHolder chromaCollectionHolder;
    private final PostRepository postRepository;
    private final PostMemberTagRepository postMemberTagRepository;

    private final EmbeddingCache embeddingCache;

    public record SearchHit(
            Long postId,
            double distance,
            List<String> memberNames
    ) {}

    public List<SearchHit> searchHits(String query) {

        float[] embedding = embeddingCache.get(query);
        if (embedding == null) {
            embedding = geminiEmbeddingClient.embed(query);
            embeddingCache.put(query, embedding);
        }

        return searchHits(embedding);
    }

    public List<SearchHit> searchHits(float[] embedding) {

        Map<String, Object> body = new HashMap<>();
        body.put("query_embeddings", List.of(toList(embedding)));
        body.put("n_results", 10);

        Map<String, Object> response =
                chromaWebClient.post()
                        .uri(
                                "/tenants/default_tenant/databases/default_database/collections/{id}/query",
                                chromaCollectionHolder.getCollectionId()
                        )
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        List<Map<String, Object>> metadatas =
                ((List<List<Map<String, Object>>>) response.get("metadatas")).get(0);

        List<Double> distances =
                ((List<List<Double>>) response.get("distances")).get(0);

        return IntStream.range(0, metadatas.size())
                .mapToObj(i -> {
                    Map<String, Object> meta = metadatas.get(i);

                    List<String> memberNames =
                            meta.get("memberNames") instanceof String s && !s.isBlank()
                                    ? Arrays.asList(s.split(","))
                                    : List.of();

                    return new SearchHit(
                            ((Number) meta.get("postId")).longValue(),
                            distances.get(i),
                            memberNames
                    );
                })
                .sorted(Comparator.comparingDouble(SearchHit::distance))
                .toList();
    }

    public String answerWithRag(String query) {

        // 1. Embedding 캐시 확인
        float[] embedding = embeddingCache.get(query);
        if (embedding == null) {
            embedding = geminiEmbeddingClient.embed(query); // Gemini 호출 1회
            embeddingCache.put(query, embedding);
        }

        // 2. 벡터 검색
        List<SearchHit> hits = searchHits(embedding);
        if (hits.isEmpty()) {
            return "관련 기록이 없습니다.";
        }

        List<Long> postIds = hits.stream()
                .map(SearchHit::postId)
                .toList();

        // 3. 질문에 포함된 닉네임 literal 필터
        List<String> allNicknames =
                postMemberTagRepository.findAllDistinctMemberNames();

        List<String> matchedMembers = allNicknames.stream()
                .filter(query::contains)
                .toList();

        if (!matchedMembers.isEmpty()) {
            List<Long> filteredPostIds =
                    postMemberTagRepository.findPostIdsByMemberNames(matchedMembers);

            postIds = postIds.stream()
                    .filter(filteredPostIds::contains)
                    .toList();
        }

        if (postIds.isEmpty()) {
            return "조건에 맞는 기록이 없습니다.";
        }

        // 4. postId → 참여 멤버 (DB 기준)
        Map<Long, List<String>> memberMap = new HashMap<>();
        for (Object[] row :
                postMemberTagRepository.findMemberNamesGroupedByPostIds(postIds)) {

            Long postId = ((Number) row[0]).longValue();
            String nickname = (String) row[1];

            memberMap.computeIfAbsent(postId, k -> new ArrayList<>()).add(nickname);
        }

        // 5. 게시글 조회
        Map<Long, Posts> postMap =
                postRepository.findAllById(postIds).stream()
                        .collect(Collectors.toMap(Posts::getPostId, p -> p));

        // 6. Context 구성
        String context = postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .map(p -> """
            [게시글]
            제목: %s
            내용: %s
            장소: %s
            작성자: %s
            날짜: %s
            함께 간 사람: %s
            """
                        .formatted(
                                p.getTitle(),
                                p.getContent(),
                                p.getPlace(),
                                p.getWriter().getNickname(),
                                p.getCreatedAt().toLocalDate(),
                                String.join(", ",
                                        memberMap.getOrDefault(p.getPostId(), List.of()))
                        ))
                .collect(Collectors.joining("\n"));

        // 7. Gemini Generate (딱 1번)
        return geminiChatClient.generate(
                RagAnswerPrompt.TEMPLATE.formatted(context, query)
        );
    }

    private List<Float> toList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float f : arr) list.add(f);
        return list;
    }


}
