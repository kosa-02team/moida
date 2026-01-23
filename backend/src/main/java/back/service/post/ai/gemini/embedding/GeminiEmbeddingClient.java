package back.service.post.ai.gemini.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

        Map<String, Object> response =
                webClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/models/text-embedding-004:embedContent")
                                .queryParam("key", apiKey)
                                .build())
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

        Map<String, Object> embedding =
                (Map<String, Object>) response.get("embedding");

        List<Double> values =
                (List<Double>) embedding.get("values");

        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }
}
