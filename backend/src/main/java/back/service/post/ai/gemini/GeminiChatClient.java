package back.service.post.ai.gemini;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiChatClient {

    private final @Qualifier("geminiWebClient") WebClient geminiWebClient;

    @Value("${spring.ai.google.gemini.model.chat}")
    private String chatModel;

    @Value("${spring.ai.google.gemini.api-key}")
    private String apiKey;

    public String generate(String prompt) {

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        Map<String, Object> response =
                geminiWebClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1beta/" + chatModel + ":generateContent")
                                .queryParam("key", apiKey)
                                .build()
                        )
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        // ===== 응답 파싱 =====
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.get("candidates");

        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini returned no candidates");
        }

        Map<String, Object> content =
                (Map<String, Object>) candidates.get(0).get("content");

        List<Map<String, String>> parts =
                (List<Map<String, String>>) content.get("parts");

        return parts.get(0).get("text");
    }


}
