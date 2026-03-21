package makeus.cmc.malmo.adaptor.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.out.chat.LlmReasoningScenario;
import makeus.cmc.malmo.application.port.out.chat.RequestChatApiPort;
import makeus.cmc.malmo.config.properties.ReasoningEffortProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractOpenAiCompatibleApiClient implements RequestChatApiPort {

    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;

    protected AbstractOpenAiCompatibleApiClient(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    protected abstract String getProviderName();

    protected abstract String getApiKey();

    protected abstract String getModel();

    protected abstract ReasoningEffortProperties getReasoningEffortProperties();

    public final String currentProviderName() {
        return getProviderName();
    }

    public final String currentModel() {
        return getModel();
    }

    public final String currentDefaultReasoningEffort() {
        ReasoningEffortProperties properties = getReasoningEffortProperties();
        return properties != null ? properties.getDefault() : null;
    }

    public final Map<LlmReasoningScenario, String> currentScenarioReasoningEfforts() {
        ReasoningEffortProperties properties = getReasoningEffortProperties();
        if (properties == null || properties.getScenarios() == null) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(properties.getScenarios()));
    }

    public final String currentReasoningEffort(LlmReasoningScenario scenario) {
        return resolveReasoningEffort(scenario);
    }

    @Override
    public Mono<String> requestStreamResponse(List<Map<String, String>> messages,
                                              LlmReasoningScenario scenario,
                                              Consumer<String> onData) {
        Map<String, Object> body = createBody(messages, scenario, true, false);

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("{} API error response: {}", getProviderName(), errorBody);
                                    return Mono.error(new RuntimeException(getProviderName() + " API error: " + errorBody));
                                })
                )
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank())
                .takeWhile(data -> !data.equals("[DONE]") && !data.equals("data: [DONE]"))
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
                .doOnError(throwable -> log.error("Error during {} stream processing", getProviderName(), throwable));
    }

    @Override
    public CompletableFuture<String> requestResponse(List<Map<String, String>> messages,
                                                     LlmReasoningScenario scenario) {
        return sendRequest(createBody(messages, scenario, false, false))
                .thenApply(this::extractContent);
    }

    @Override
    public CompletableFuture<String> requestJsonResponse(List<Map<String, String>> messages,
                                                         LlmReasoningScenario scenario) {
        return sendRequest(createBody(messages, scenario, false, true))
                .thenApply(this::extractContent);
    }

    private CompletableFuture<String> sendRequest(Map<String, Object> body) {
        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .toFuture();
    }

    private String extractContent(String data) {
        try {
            JsonNode node = objectMapper.readTree(data);
            return node.path("choices").get(0).path("message").path("content").asText();
        } catch (JsonProcessingException e) {
            log.error("Error processing {} API response", getProviderName(), e);
            throw new RuntimeException(e);
        }
    }

    private String extractStreamContent(String data) throws JsonProcessingException {
        String normalized = data.startsWith("data: ") ? data.substring(6) : data;
        JsonNode node = objectMapper.readTree(normalized);
        return node
                .path("choices").get(0)
                .path("delta")
                .path("content")
                .asText();
    }

    protected final Map<String, Object> createBody(List<Map<String, String>> messages,
                                                   LlmReasoningScenario scenario,
                                                   boolean stream,
                                                   boolean jsonResponse) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", getModel());

        String reasoningEffort = resolveReasoningEffort(scenario);
        if (StringUtils.hasText(reasoningEffort)) {
            body.put("reasoning_effort", reasoningEffort);
        }

        if (jsonResponse) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        body.put("messages", messages);
        body.put("stream", stream);
        return body;
    }

    private String resolveReasoningEffort(LlmReasoningScenario scenario) {
        ReasoningEffortProperties properties = getReasoningEffortProperties();
        if (properties == null) {
            return null;
        }

        String scenarioEffort = properties.getScenarioEffort(scenario);
        if (StringUtils.hasText(scenarioEffort)) {
            return scenarioEffort;
        }

        String defaultEffort = properties.getDefault();
        return StringUtils.hasText(defaultEffort) ? defaultEffort : null;
    }
}
