package back.service.post.ai.gemini.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.ai.google.gemini.api-key")
public class GeminiEmbeddingClient {

    @Value("${spring.ai.google.gemini.api-key}")
    private String apiKey;

    private final WebClient webClient = WebClient.create(
            "https://generativelanguage.googleapis.com/v1beta"
    );

    public float[] embed(String text) {

        Map<String, Object> body = Map.of(
                "model", "models/text-embedding-004",
                "content", Map.of(
                        "parts", List.of(Map.of("text", text))
                )
        );

        try {
            Map<String, Object> response =
                    webClient.post()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/models/text-embedding-004:embedContent")
                                    .queryParam("key", apiKey)
                                    .build())
                            .bodyValue(body)
                            .retrieve()
                            .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS, clientResponse -> {
                                System.err.println("Gemini Embedding API 호출 제한에 도달했습니다.");
                                return Mono.error(new RuntimeException("API 호출 제한"));
                            })
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                            .block();

            if (response == null) {
                throw new IllegalStateException("Gemini Embedding API 응답이 null입니다.");
            }

            Map<String, Object> embedding =
                    (Map<String, Object>) response.get("embedding");

            List<Double> values =
                    (List<Double>) embedding.get("values");

            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i).floatValue();
            }
            return result;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new RuntimeException("API 호출 제한", ex);
            }
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException("Gemini Embedding API 호출 실패: " + e.getMessage(), e);
        }
    }
}
