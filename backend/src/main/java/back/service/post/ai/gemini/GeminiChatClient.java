package back.service.post.ai.gemini;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.ai.google.gemini.api-key")
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

        try {
            Map<String, Object> response =
                    geminiWebClient.post()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/v1beta/" + chatModel + ":generateContent")
                                    .queryParam("key", apiKey)
                                    .build()
                            )
                            .bodyValue(body)
                            .retrieve()
                            .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS, clientResponse -> {
                                System.err.println("Gemini API 호출 제한에 도달했습니다.");
                                return Mono.error(new RuntimeException("API 호출 제한"));
                            })
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                            .block();

            if (response == null) {
                throw new IllegalStateException("Gemini API 응답이 null입니다.");
            }

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
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new RuntimeException("API 호출 제한", ex);
            }
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException("Gemini API 호출 실패: " + e.getMessage(), e);
        }
    }


}
