package makeus.cmc.malmo.application.service.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.out.chat.LlmReasoningScenario;
import makeus.cmc.malmo.application.port.out.chat.RequestChatApiPort;
import makeus.cmc.malmo.application.port.in.chat.SufficiencyCheckResult;
import makeus.cmc.malmo.domain.model.chat.DetailedPrompt;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import makeus.cmc.malmo.domain.value.type.SenderType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatProcessor {

    private final RequestChatApiPort requestChatApiPort;
    private final ObjectMapper objectMapper;

    public Mono<Void> streamChat(List<Map<String, String>> messages,
                                 LlmReasoningScenario scenario,
                                 Prompt systemPrompt,
                                 Prompt prompt,
                                 DetailedPrompt detailedPrompt,
                                 Consumer<String> onChunk,
                                 Consumer<String> onComplete,
                                 Consumer<String> onError) {

        messages.add(createMessageMap(SenderType.SYSTEM, systemPrompt.getContent()));
        messages.add(createMessageMap(SenderType.SYSTEM, prompt.getContent()));
        messages.add(createMessageMap(SenderType.SYSTEM, detailedPrompt.getContent()));

        log.info("Starting streamChat with messages: {}", messages);

        return requestChatApiPort.requestStreamResponse(messages, scenario, onChunk)
                .flatMap(fullAnswer -> {
                    // 스트림이 성공적으로 완료되고 전체 응답(fullAnswer)이 오면 onComplete 로직 실행
                    onComplete.accept(fullAnswer);
                    log.info("Stream completed with full answer: {}", fullAnswer);
                    return Mono.empty(); // 성공적으로 완료했음을 알리기 위해 비어있는 Mono 반환
                })
                .doOnError(throwable -> onError.accept(throwable.getMessage())) // 에러 발생 시 onError 콜백 실행
                .then(); // 최종적으로 Mono<Void>로 변환하여 작업의 완료를 알림
    }

    public CompletableFuture<String> requestSummaryAsync(List<Map<String, String>> messages,
                                                         Prompt systemPrompt,
                                                         Prompt prompt,
                                                         Prompt summaryPrompt) {

        messages.add(createMessageMap(SenderType.SYSTEM, systemPrompt.getContent()));
        messages.add(createMessageMap(SenderType.SYSTEM, prompt.getContent()));
        messages.add(createMessageMap(SenderType.SYSTEM, "[현재 단계 지시] " + summaryPrompt.getContent()));

        return requestChatApiPort.requestResponse(messages, LlmReasoningScenario.SUMMARY);
    }


    public CompletableFuture<String> requestMetaData(String question,
                                  String memberAnswer,
                                  Prompt metadataPrompt) {
        List<Map<String, String>> messages = List.of(
                createMessageMap(SenderType.SYSTEM, metadataPrompt.getContent()),
                createMessageMap(SenderType.ASSISTANT, "[질문] " + question),
                createMessageMap(SenderType.USER, "[답변] " + memberAnswer)
        );

        return requestChatApiPort.requestResponse(messages, LlmReasoningScenario.AUXILIARY_EXTRACTION);
    }

    public CompletableFuture<SufficiencyCheckResult> requestSufficiencyCheck(List<Map<String, String>> messages,
                                                                             DetailedPrompt validationPrompt) {
        messages.add(createMessageMap(SenderType.SYSTEM, validationPrompt.getContent()));

        log.info("Requesting sufficiency check with messages: {}", messages);

        return requestChatApiPort.requestJsonResponse(messages, LlmReasoningScenario.VALIDATION)
                .thenApply(jsonResponse -> {
                    try {
                        log.info("Received sufficiency check JSON: {}", jsonResponse);
                        return objectMapper.readValue(jsonResponse, SufficiencyCheckResult.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse sufficiency check JSON: {}", jsonResponse, e);
                        throw new RuntimeException("Failed to parse sufficiency check JSON", e);
                    }
                });
    }

    public CompletableFuture<String> requestDetailedSummary(List<Map<String, String>> messages,
                                                           DetailedPrompt summaryPrompt) {
        messages.add(createMessageMap(SenderType.SYSTEM, summaryPrompt.getContent()));
        return requestChatApiPort.requestResponse(messages, LlmReasoningScenario.SUMMARY);
    }

    /**
     * 4단계 자유 대화 요약 생성
     * @param messages 요약할 메시지 목록
     * @param summaryPrompt 요약 프롬프트
     * @return 생성된 요약 문자열
     */
    public CompletableFuture<String> requestConversationSummary(List<Map<String, String>> messages,
                                                                 Prompt summaryPrompt) {
        messages.add(createMessageMap(SenderType.SYSTEM, summaryPrompt.getContent()));
        return requestChatApiPort.requestResponse(messages, LlmReasoningScenario.SUMMARY);
    }

    /**
     * 제목 생성 요청
     * @return 생성된 제목 문자열
     */
    public CompletableFuture<String> requestTitleGeneration(List<Map<String, String>> messages, Prompt titlePrompt) {
        // OpenAI API 호출하여 제목 생성
        // 짧은 제목 (20자 이내) 생성하도록 프롬프트 구성
        
        List<Map<String, String>> promptMessages = new ArrayList<>(messages);
        promptMessages.add(createMessageMap(SenderType.SYSTEM, titlePrompt.getContent()));
        
        return requestChatApiPort.requestResponse(promptMessages, LlmReasoningScenario.AUXILIARY_EXTRACTION)
                .thenApply(title -> {
                    // 제목 길이 제한 (최대 50자)
                    String trimmedTitle = title.trim();
                    return trimmedTitle.length() > 50 ? trimmedTitle.substring(0, 50) : trimmedTitle;
                });
    }

    private Map<String, String> createMessageMap(SenderType senderType, String content) {
        return Map.of(
                "role", senderType.getApiName(),
                "content", content
        );
    }

}
