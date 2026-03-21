package makeus.cmc.malmo.application.port.out.chat;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface RequestChatApiPort {
    Mono<String> requestStreamResponse(List<Map<String, String>> messages,
                                       LlmReasoningScenario scenario,
                                       Consumer<String> onData);

    CompletableFuture<String> requestResponse(List<Map<String, String>> messages,
                                              LlmReasoningScenario scenario);

    CompletableFuture<String> requestJsonResponse(List<Map<String, String>> messages,
                                                  LlmReasoningScenario scenario);
}
