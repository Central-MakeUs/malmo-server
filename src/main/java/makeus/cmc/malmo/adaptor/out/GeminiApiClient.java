package makeus.cmc.malmo.adaptor.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.out.CheckOpenAIHealth;
import makeus.cmc.malmo.application.port.out.chat.RequestChatApiPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient implements RequestChatApiPort, CheckOpenAIHealth {

    public static final String GEMINI_MODEL = "gemini-3-flash-preview";
    public static final double GEMINI_TEMPERATURE = 0.5;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Override
    public Mono<String> requestStreamResponse(List<Map<String, String>> messages, Consumer<String> onData) {
        Map<String, Object> body = buildGeminiBody(messages, false);

        return webClient.post()
                .uri("/models/" + GEMINI_MODEL + ":streamGenerateContent?alt=sse")
                .header("x-goog-api-key", geminiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Gemini API error response: {}", errorBody);
                                    return Mono.error(new RuntimeException("Gemini API error: " + errorBody));
                                })
                )
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank())
                .flatMap(data -> {
                    try {
                        return Mono.just(extractStreamContent(data));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Failed to parse stream content", e));
                    }
                })
                .filter(content -> !content.isEmpty())
                .doOnNext(onData)
                .collect(Collectors.joining(""))
                .doOnError(throwable -> log.error("Error during Gemini stream processing", throwable));
    }

    @Override
    public CompletableFuture<String> requestResponse(List<Map<String, String>> messages) {
        Map<String, Object> body = buildGeminiBody(messages, false);
        return sendRequest(body)
                .thenApply(this::extractContent);
    }

    @Override
    public CompletableFuture<String> requestJsonResponse(List<Map<String, String>> messages) {
        Map<String, Object> body = buildGeminiBody(messages, true);
        return sendRequest(body)
                .thenApply(this::extractContent);
    }

    private CompletableFuture<String> sendRequest(Map<String, Object> body) {
        return webClient.post()
                .uri("/models/" + GEMINI_MODEL + ":generateContent")
                .header("x-goog-api-key", geminiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .toFuture();
    }

    private String extractContent(String data) {
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode parts = node.path("candidates").get(0).path("content").path("parts");
            for (JsonNode part : parts) {
                if (!part.path("thought").asBoolean(false)) {
                    return part.path("text").asText("");
                }
            }
            log.warn("No non-thought part found in Gemini response: {}", data);
            return "";
        } catch (JsonProcessingException e) {
            log.error("Error processing Gemini API response", e);
            throw new RuntimeException(e);
        }
    }

    private String extractStreamContent(String data) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(data);
        JsonNode candidatesNode = node.path("candidates");
        if (candidatesNode.isMissingNode() || candidatesNode.isEmpty()) {
            return "";
        }
        JsonNode partsNode = candidatesNode.get(0).path("content").path("parts");
        if (partsNode.isMissingNode() || partsNode.isEmpty()) {
            return "";
        }
        for (JsonNode part : partsNode) {
            if (!part.path("thought").asBoolean(false)) {
                return part.path("text").asText("");
            }
        }
        return "";
    }

    /**
     * Builds Gemini API request body from OpenAI-style message list.
     * System messages are extracted to system_instruction; assistant roles are renamed to model.
     */
    private Map<String, Object> buildGeminiBody(List<Map<String, String>> messages, boolean jsonMode) {
        String systemText = messages.stream()
                .filter(m -> "system".equals(m.get("role")))
                .map(m -> m.get("content"))
                .collect(Collectors.joining("\n\n"));

        List<Map<String, Object>> contents = messages.stream()
                .filter(m -> !"system".equals(m.get("role")))
                .map(m -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("role", "assistant".equals(m.get("role")) ? "model" : m.get("role"));
                    entry.put("parts", List.of(Map.of("text", m.get("content"))));
                    return entry;
                })
                .collect(Collectors.toList());

        Map<String, Object> thinkingConfig = new HashMap<>();
        thinkingConfig.put("thinkingBudget", 0);

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", GEMINI_TEMPERATURE);
        generationConfig.put("thinkingConfig", thinkingConfig);
        if (jsonMode) {
            generationConfig.put("responseMimeType", "application/json");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);
        body.put("generationConfig", generationConfig);
        if (!systemText.isEmpty()) {
            body.put("system_instruction", Map.of(
                    "parts", List.of(Map.of("text", systemText))
            ));
        }
        return body;
    }

    @Override
    public boolean checkHealth() {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + "?key=" + geminiApiKey;
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("name")) {
                log.info("Gemini API is UP: model={}", response.get("name"));
                return true;
            }
            log.warn("Gemini HealthCheck: Unexpected response: {}", response);
            return false;
        } catch (Exception e) {
            log.error("Gemini HealthCheck failed", e);
            return false;
        }
    }
}
