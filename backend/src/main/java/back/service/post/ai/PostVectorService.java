package back.service.post.ai;

import back.domain.post.Posts;
import back.repository.club.ClubMemberRepository;
import back.repository.post.PostMemberTagRepository;
import back.service.post.ai.chroma.ChromaCollectionHolder;
import back.service.post.ai.gemini.embedding.EmbeddingTextBuilder;
import back.service.post.ai.gemini.embedding.GeminiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostVectorService {

    private final WebClient chromaWebClient;
    private final GeminiEmbeddingClient geminiEmbeddingClient;
    private final ChromaCollectionHolder chromaCollectionHolder;
    private final PostMemberTagRepository postMemberTagRepository;
    private final ClubMemberRepository clubMemberRepository;
    public void savePost(Posts post) {
        List<String> memberNames =
                postMemberTagRepository.findByPostId(post.getPostId())
                        .stream()
                        .map(tag -> clubMemberRepository
                                .findNicknameByMemberId(tag.getMemberId()))
                        .toList();


        String embeddingText = EmbeddingTextBuilder.build(post,memberNames);
        float[] embedding = geminiEmbeddingClient.embed(embeddingText);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("postId", post.getPostId());
        metadata.put("clubId", post.getClub().getClubId());
        metadata.put("writerName", post.getWriter().getNickname());
        metadata.put("writerId", post.getWriter().getUserId());
        if (!memberNames.isEmpty()) {
            metadata.put("memberNames", String.join(",", memberNames));
        }

        // place는 null 아닐 때만
        if (post.getPlace() != null && !post.getPlace().isBlank()) {
            metadata.put("place", post.getPlace());
        }

        // memberIds → 문자열로 변환해서 저장
        List<String> memberIds =
                postMemberTagRepository.findByPostId(post.getPostId())
                        .stream()
                        .map(tag -> String.valueOf(tag.getMemberId()))
                        .toList();

        if (!memberIds.isEmpty()) {
            metadata.put("memberIds", String.join(",", memberIds));
        }

        chromaWebClient.post()
                .uri(
                        "/tenants/default_tenant/databases/default_database/collections/{id}/upsert",
                        chromaCollectionHolder.getCollectionId()
                )
                .bodyValue(Map.of(
                        "ids", List.of("post-" + post.getPostId()),
                        "embeddings", List.of(toList(embedding)),
                        "documents", List.of(post.getContent()),
                        "metadatas", List.of(metadata)
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }




    private List<Float> toList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float v : arr) list.add(v);
        return list;
    }
}
