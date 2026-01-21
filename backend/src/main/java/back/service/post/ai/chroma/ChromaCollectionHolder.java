package back.service.post.ai.chroma;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChromaCollectionHolder {

    private final WebClient chromaWebClient;
    private String collectionId;

    @PostConstruct
    public void init() {
        try {
            ensureCollection();
        } catch (Exception e) {
            System.out.println("Chroma init skipped "+ e);
        }
    }


    public String getCollectionId() {
        return collectionId;
    }

    private void ensureCollection() {
        if (collectionId != null) {
            return;
        }

        List<Map<String, Object>> collections =
                chromaWebClient.get()
                        .uri("/tenants/default_tenant/databases/default_database/collections")
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<
                                                        List<Map<String, Object>>>() {})
                        .block();

        for (Map<String, Object> c : collections) {
            if ("posts".equals(c.get("name"))) {
                this.collectionId = (String) c.get("id");
                return;
            }
        }

        //없으면 생성
        Map<String, Object> created =
                chromaWebClient.post()
                        .uri("/tenants/default_tenant/databases/default_database/collections")
                        .bodyValue(Map.of("name", "posts"))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<
                                Map<String, Object>>() {})
                        .block();

        this.collectionId = (String) created.get("id");
    }

}
