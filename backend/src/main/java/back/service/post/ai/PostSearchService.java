package back.service.post.ai;

import back.domain.post.Posts;
import back.domain.schedule.Schedules;
import back.repository.post.PostMemberTagRepository;
import back.repository.post.PostRepository;
import back.repository.schedule.ScheduleRepository;
import back.service.post.ai.chroma.ChromaCollectionHolder;
import back.service.post.ai.gemini.GeminiChatClient;
import back.service.post.ai.gemini.embedding.EmbeddingCache;
import back.service.post.ai.gemini.embedding.GeminiEmbeddingClient;
import back.service.post.ai.gemini.prompt.RagAnswerPrompt;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final WebClient chromaWebClient;
    private final Optional<GeminiEmbeddingClient> geminiEmbeddingClient;
    private final Optional<GeminiChatClient> geminiChatClient;
    private final ChromaCollectionHolder chromaCollectionHolder;
    private final PostRepository postRepository;
    private final PostMemberTagRepository postMemberTagRepository;
    private final ScheduleRepository scheduleRepository;

    private final EmbeddingCache embeddingCache;

    public record SearchHit(
            Long postId,
            Long scheduleId,
            String type,
            double distance,
            List<String> memberNames
    ) {}

    public List<SearchHit> searchHits(String query) {
        if (geminiEmbeddingClient.isEmpty()) {
            return List.of(); // API 키가 없으면 빈 결과 반환
        }

        float[] embedding = embeddingCache.get(query);
        if (embedding == null) {
            embedding = geminiEmbeddingClient.get().embed(query);
            embeddingCache.put(query, embedding);
        }

        return searchHits(embedding);
    }

    public List<SearchHit> searchHits(float[] embedding) {
        return searchHits(embedding, null);
    }

    public List<SearchHit> searchHits(float[] embedding, Long clubId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("query_embeddings", List.of(toList(embedding)));
            body.put("n_results", 10);
            
            // clubId 필터링은 DB 조회 시 처리 (ChromaDB where 필터는 일단 제거)
            // if (clubId != null) {
            //     Map<String, Object> where = new HashMap<>();
            //     where.put("clubId", clubId);
            //     body.put("where", where);
            // }

            Map<String, Object> response =
                    chromaWebClient.post()
                            .uri(
                                    "/tenants/default_tenant/databases/default_database/collections/{id}/query",
                                    chromaCollectionHolder.getCollectionId()
                            )
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                            .block();
            
            if (response == null) {
                return List.of();
            }

            // 응답 구조 안전하게 파싱
            Object metadatasObj = response.get("metadatas");
            Object distancesObj = response.get("distances");
            
            if (metadatasObj == null || distancesObj == null) {
                System.err.println("ChromaDB 응답에 metadatas 또는 distances가 없습니다.");
                return List.of();
            }

            List<Map<String, Object>> metadatas;
            List<Double> distances;
            
            try {
                metadatas = ((List<List<Map<String, Object>>>) metadatasObj).get(0);
                distances = ((List<List<Double>>) distancesObj).get(0);
            } catch (ClassCastException | IndexOutOfBoundsException e) {
                System.err.println("ChromaDB 응답 파싱 실패: " + e.getMessage());
                e.printStackTrace();
                return List.of();
            }
            
            if (metadatas == null || distances == null || metadatas.size() != distances.size()) {
                System.err.println("ChromaDB 응답 데이터 불일치");
                return List.of();
            }

            return IntStream.range(0, metadatas.size())
                    .mapToObj(i -> {
                        Map<String, Object> meta = metadatas.get(i);

                        List<String> memberNames =
                                meta.get("memberNames") instanceof String s && !s.isBlank()
                                        ? Arrays.asList(s.split(","))
                                        : List.of();

                        String type = meta.get("type") instanceof String t ? t : "post";
                        Long postId = meta.get("postId") != null ? ((Number) meta.get("postId")).longValue() : null;
                        Long scheduleId = meta.get("scheduleId") != null ? ((Number) meta.get("scheduleId")).longValue() : null;

                        return new SearchHit(
                                postId,
                                scheduleId,
                                type,
                                distances.get(i),
                                memberNames
                        );
                    })
                    .sorted(Comparator.comparingDouble(SearchHit::distance))
                    .toList();
        } catch (Exception e) {
            // ChromaDB 쿼리 실패 시 로그 출력 후 빈 결과 반환
            System.err.println("ChromaDB 쿼리 실패: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public String answerWithRag(String query, Long clubId) {
        try {
            System.err.println("answerWithRag 시작: query=" + query + ", clubId=" + clubId);
            
            if (geminiEmbeddingClient.isEmpty() || geminiChatClient.isEmpty()) {
                System.err.println("Gemini 클라이언트가 없습니다.");
                return "AI 검색 기능을 사용할 수 없습니다. API 키가 설정되지 않았습니다.";
            }

            if (clubId == null) {
                System.err.println("clubId가 null입니다.");
                return "모임 정보가 없습니다.";
            }

            // 1. Embedding 캐시 확인
            System.err.println("1. Embedding 캐시 확인 중...");
            float[] embedding = embeddingCache.get(query);
            if (embedding == null) {
                System.err.println("Embedding 캐시에 없음. Gemini API 호출 중...");
                embedding = geminiEmbeddingClient.get().embed(query); // Gemini 호출 1회
                embeddingCache.put(query, embedding);
                System.err.println("Embedding 생성 완료. 길이: " + embedding.length);
            } else {
                System.err.println("Embedding 캐시에서 가져옴. 길이: " + embedding.length);
            }

            // 2. 벡터 검색 (clubId로 미리 필터링)
            System.err.println("2. 벡터 검색 시작...");
            List<SearchHit> hits = searchHits(embedding, clubId);
            System.err.println("벡터 검색 결과: " + hits.size() + "개");
            if (hits.isEmpty()) {
                System.err.println("검색 결과가 없습니다.");
                return "관련 기록이 없습니다.";
            }

            // 게시글과 일정 분리
            System.err.println("3. 게시글과 일정 분리 중...");
            List<Long> postIds = hits.stream()
                    .filter(hit -> hit.postId() != null)
                    .map(SearchHit::postId)
                    .toList();

            List<Long> scheduleIds = hits.stream()
                    .filter(hit -> hit.scheduleId() != null)
                    .map(SearchHit::scheduleId)
                    .toList();
            
            System.err.println("postIds: " + postIds.size() + "개, scheduleIds: " + scheduleIds.size() + "개");

            // 3. 질문에 포함된 닉네임 literal 필터 (게시글만 해당)
            if (!postIds.isEmpty()) {
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
            }

            // 4. postId → 참여 멤버 (DB 기준)
            Map<Long, List<String>> memberMap = new HashMap<>();
            if (!postIds.isEmpty()) {
                for (Object[] row :
                        postMemberTagRepository.findMemberNamesGroupedByPostIds(postIds)) {

                    Long postId = ((Number) row[0]).longValue();
                    String nickname = (String) row[1];

                    memberMap.computeIfAbsent(postId, k -> new ArrayList<>()).add(nickname);
                }
            }

            // 5. 게시글 조회 (clubId로 필터링)
            Map<Long, Posts> postMap = new HashMap<>();
            if (!postIds.isEmpty() && clubId != null) {
                postMap = postRepository.findAllById(postIds).stream()
                        .filter(p -> p != null && p.getClub() != null && 
                                p.getClub().getClubId() != null && 
                                p.getClub().getClubId().equals(clubId))
                        .collect(Collectors.toMap(
                                Posts::getPostId, 
                                p -> p,
                                (existing, replacement) -> existing // 중복 키 처리
                        ));
            }

            // 6. 일정 조회 (clubId로 필터링)
            Map<Long, Schedules> scheduleMap = new HashMap<>();
            if (!scheduleIds.isEmpty() && clubId != null) {
                scheduleMap = scheduleRepository.findAllById(scheduleIds).stream()
                        .filter(s -> s != null && s.getClubId() != null && 
                                s.getClubId().equals(clubId))
                        .collect(Collectors.toMap(
                                Schedules::getScheduleId, 
                                s -> s,
                                (existing, replacement) -> existing // 중복 키 처리
                        ));
            }

            // 필터링 후 ID 리스트 업데이트
            postIds = new ArrayList<>(postMap.keySet());
            scheduleIds = new ArrayList<>(scheduleMap.keySet());

            // 결과 확인
            if (postIds.isEmpty() && scheduleIds.isEmpty()) {
                return "관련 기록이 없습니다.";
            }

            // 7. Context 구성
            List<String> contextParts = new ArrayList<>();

            // 게시글 Context
            for (Long postId : postIds) {
                Posts p = postMap.get(postId);
                if (p != null) {
                    String writerName = (p.getWriter() != null && p.getWriter().getNickname() != null) 
                            ? p.getWriter().getNickname() 
                            : "알 수 없음";
                    String dateStr = (p.getCreatedAt() != null) 
                            ? p.getCreatedAt().toLocalDate().toString() 
                            : "";
                    
                    contextParts.add("""
            [게시글]
            제목: %s
            내용: %s
            장소: %s
            작성자: %s
            날짜: %s
            함께 간 사람: %s
            """
                            .formatted(
                                    p.getTitle() != null ? p.getTitle() : "",
                                    p.getContent() != null ? p.getContent() : "",
                                    p.getPlace() != null ? p.getPlace() : "",
                                    writerName,
                                    dateStr,
                                    String.join(", ",
                                            memberMap.getOrDefault(p.getPostId(), List.of()))
                            ));
                }
            }

            // 일정 Context
            for (Long scheduleId : scheduleIds) {
                Schedules s = scheduleMap.get(scheduleId);
                if (s != null) {
                    contextParts.add("""
            [일정]
            일정명: %s
            설명: %s
            일정 날짜: %s
            장소: %s
            참가비: %s
            """
                            .formatted(
                                    s.getScheduleName() != null ? s.getScheduleName() : "",
                                    s.getDescription() != null ? s.getDescription() : "",
                                    s.getEventDate() != null
                                            ? s.getEventDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                            : "",
                                    s.getLocation() != null ? s.getLocation() : "",
                                    s.getEntryFee() != null && s.getEntryFee().compareTo(java.math.BigDecimal.ZERO) > 0
                                            ? s.getEntryFee() + "원"
                                            : "없음"
                            ));
                }
            }

            String context = String.join("\n", contextParts);
            System.err.println("Context 길이: " + context.length() + "자");

            // 8. Gemini Generate (딱 1번)
            System.err.println("8. Gemini API 호출 중...");
            try {
                String prompt = RagAnswerPrompt.TEMPLATE.formatted(context, query);
                System.err.println("Prompt 생성 완료. 길이: " + prompt.length() + "자");
                String answer = geminiChatClient.get().generate(prompt);
                System.err.println("Gemini 응답 받음. 길이: " + answer.length() + "자");
                return answer;
            } catch (RuntimeException e) {
                System.err.println("Gemini API 호출 실패: " + e.getMessage());
                // 원인 예외 확인
                Throwable cause = e.getCause();
                if (cause != null) {
                    System.err.println("원인: " + cause.getClass().getSimpleName() + " - " + cause.getMessage());
                }
                e.printStackTrace();
                // 429 에러인 경우 특별한 메시지 반환
                if (e.getMessage() != null && e.getMessage().contains("API 호출 제한")) {
                    return "AI 검색 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.";
                }
                throw e; // 상위로 전파
            } catch (Exception e) {
                System.err.println("Gemini API 호출 실패 (예상치 못한 에러): " + e.getMessage());
                e.printStackTrace();
                throw e; // 상위로 전파
            }
        } catch (RuntimeException e) {
            // 에러 발생 시 로그 출력 후 안전한 메시지 반환
            System.err.println("answerWithRag 실패: " + e.getMessage());
            Throwable cause = e.getCause();
            if (cause != null) {
                System.err.println("원인: " + cause.getClass().getSimpleName() + " - " + cause.getMessage());
            }
            e.printStackTrace();
            // 429 에러인 경우 특별한 메시지 반환
            if (e.getMessage() != null && e.getMessage().contains("API 호출 제한")) {
                return "AI 검색 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.";
            }
            return "AI 검색 중 오류가 발생했습니다: " + (e.getMessage() != null ? e.getMessage() : "알 수 없는 오류");
        } catch (Exception e) {
            // 예상치 못한 에러
            System.err.println("answerWithRag 실패 (예상치 못한 에러): " + e.getMessage());
            e.printStackTrace();
            return "AI 검색 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    private List<Float> toList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float f : arr) list.add(f);
        return list;
    }


}
