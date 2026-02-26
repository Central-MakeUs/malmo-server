package makeus.cmc.malmo.adaptor.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.out.exception.OpenAiRequestException;
import makeus.cmc.malmo.application.port.out.CheckOpenAIHealth;
import makeus.cmc.malmo.application.port.out.chat.RequestChatApiPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static makeus.cmc.malmo.util.GlobalConstants.OPENAI_STATUS_URL;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiApiClient implements RequestChatApiPort, CheckOpenAIHealth {

    public static final String GPT_VERSION = "gpt-4.1";
    public static final double GPT_TEMPERATURE = 0.5;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate;

    @Override
    public Mono<String> requestStreamResponse(List<Map<String, String>> messages, Consumer<String> onData) {
        Map<String, Object> body = createStreamBody(messages);

        // WebClient로 스트리밍 데이터를 Flux<String> 형태로 요청
        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + openAiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("OpenAI API error response: {}", errorBody);
                                    return Mono.error(new RuntimeException("OpenAI API error: " + errorBody));
                                })
                )
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank()) // 비어있는 줄 필터링은 유지
                .takeWhile(data -> !data.equals("[DONE]"))
                .map(data -> {
                    try {
                        return extractStreamContent(data);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to parse stream content", e);
                    }
                })
                .filter(content -> !content.isEmpty())
                .doOnNext(onData)
                .collect(Collectors.joining(""))
                .doOnError(throwable -> log.error("Error during OpenAI stream processing", throwable));
    }

    @Override
    public CompletableFuture<String> requestResponse(List<Map<String, String>> messages) {
        Map<String, Object> body = createBody(messages);
        return sendRequest(body)
                .thenApply(this::extractContent); // 응답이 오면 extractContent 실행
    }

    @Override
    public CompletableFuture<String> requestJsonResponse(List<Map<String, String>> messages) {
        Map<String, Object> body = createBodyForJsonResponse(messages);
        return sendRequest(body)
                .thenApply(this::extractContent); // 응답이 오면 extractContent 실행
    }

    private CompletableFuture<String> sendRequest(Map<String, Object> body) {
        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + openAiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class) // 응답 바디를 Mono<String>으로 받음
                .toFuture(); // Mono를 CompletableFuture로 변환
    }

    private String extractContent(String data) {
        try {
            JsonNode node = objectMapper.readTree(data);
            return node.path("choices").get(0).path("message").path("content").asText();
        } catch (JsonProcessingException e) {
            log.error("Error processing OpenAI API response", e);
            throw new RuntimeException(e); // 예외를 던져 CompletableFuture가 exceptionally로 처리하게 함
        }
    }


    private String extractStreamContent(String data) throws JsonProcessingException {
        JsonNode node = new ObjectMapper().readTree(data);
        return node
                .path("choices").get(0)
                .path("delta")
                .path("content")
                .asText();
    }

    private Map<String, Object> createStreamBody(List<Map<String, String>> messages) {
        return Map.of(
                "model", GPT_VERSION,
                "messages", messages,
                "temperature", GPT_TEMPERATURE,
                "stream", true
        );
    }

    private Map<String, Object> createBody(List<Map<String, String>> messages) {
        return Map.of(
                "model", GPT_VERSION,
                "messages", messages,
                "temperature", GPT_TEMPERATURE,
                "stream", false
        );
    }

    private Map<String, Object> createBodyForJsonResponse(List<Map<String, String>> messages) {
        return Map.of(
                "model", GPT_VERSION,
                "response_format", Map.of("type", "json_object"),
                "messages", messages,
                "temperature", GPT_TEMPERATURE,
                "stream", false
        );
    }

    @Override
    public boolean checkHealth() {
        try {
            Map response = restTemplate.getForObject(OPENAI_STATUS_URL, Map.class);
            if (response == null) {
                log.warn("OpenAI HealthCheck: Empty response");
                return false;
            }

            Map status = (Map) response.get("status");
            String indicator = (String) status.get("indicator");
            String description = (String) status.get("description");

            if ("none".equalsIgnoreCase(indicator)) {
                log.info("OpenAI API is UP: {}", description);
                return true;
            } else {
                log.warn(" OpenAI API Issue: {} ({})", description, indicator);
                return false;
            }
        } catch (Exception e) {
            log.error("OpenAI HealthCheck failed", e);
            return false;
        }
    }
}

