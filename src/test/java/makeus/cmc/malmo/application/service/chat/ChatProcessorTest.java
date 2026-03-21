package makeus.cmc.malmo.application.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import makeus.cmc.malmo.application.port.in.chat.SufficiencyCheckResult;
import makeus.cmc.malmo.application.port.out.chat.LlmReasoningScenario;
import makeus.cmc.malmo.application.port.out.chat.RequestChatApiPort;
import makeus.cmc.malmo.domain.model.chat.DetailedPrompt;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatProcessorTest {

    @Mock
    private RequestChatApiPort requestChatApiPort;

    private ChatProcessor chatProcessor;

    @BeforeEach
    void setUp() {
        chatProcessor = new ChatProcessor(requestChatApiPort, new ObjectMapper());
    }

    @Test
    void streamChat_usesProvidedScenario() {
        when(requestChatApiPort.requestStreamResponse(anyList(), eq(LlmReasoningScenario.STRUCTURED_CHAT), any()))
                .thenReturn(Mono.just("full answer"));

        List<Map<String, String>> messages = new ArrayList<>();
        chatProcessor.streamChat(
                        messages,
                        LlmReasoningScenario.STRUCTURED_CHAT,
                        Prompt.from(1L, 1, "system", false, false, false, false, false, false, false, null, null, null),
                        Prompt.from(2L, 1, "guideline", false, false, true, false, false, false, false, null, null, null),
                        DetailedPrompt.create(1, 1, "detail", false, false, null, false, true),
                        chunk -> { },
                        full -> { },
                        error -> { }
                )
                .block();

        verify(requestChatApiPort).requestStreamResponse(anyList(), eq(LlmReasoningScenario.STRUCTURED_CHAT), any());
    }

    @Test
    void requestMetaData_usesAuxiliaryExtractionScenario() {
        when(requestChatApiPort.requestResponse(anyList(), eq(LlmReasoningScenario.AUXILIARY_EXTRACTION)))
                .thenReturn(CompletableFuture.completedFuture("memo"));

        String result = chatProcessor.requestMetaData("question", "answer",
                        Prompt.from(1L, 999, "metadata", false, false, false, false, false, true, false, null, null, null))
                .join();

        assertThat(result).isEqualTo("memo");
        verify(requestChatApiPort).requestResponse(anyList(), eq(LlmReasoningScenario.AUXILIARY_EXTRACTION));
    }

    @Test
    void requestSufficiencyCheck_usesValidationScenario() {
        when(requestChatApiPort.requestJsonResponse(anyList(), eq(LlmReasoningScenario.VALIDATION)))
                .thenReturn(CompletableFuture.completedFuture("""
                        {"completed":true,"summary":"ok","advice":null}
                        """));

        SufficiencyCheckResult result = chatProcessor.requestSufficiencyCheck(
                new ArrayList<>(),
                DetailedPrompt.create(1, 1, "validation", true, false, null, false, false)
        ).join();

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getSummary()).isEqualTo("ok");
        verify(requestChatApiPort).requestJsonResponse(anyList(), eq(LlmReasoningScenario.VALIDATION));
    }

    @Test
    void requestConversationSummary_usesSummaryScenario() {
        when(requestChatApiPort.requestResponse(anyList(), eq(LlmReasoningScenario.SUMMARY)))
                .thenReturn(CompletableFuture.completedFuture("summary"));

        String result = chatProcessor.requestConversationSummary(
                new ArrayList<>(),
                Prompt.from(1L, 4, "summary prompt", false, true, false, false, false, false, false, null, null, null)
        ).join();

        assertThat(result).isEqualTo("summary");
        verify(requestChatApiPort).requestResponse(anyList(), eq(LlmReasoningScenario.SUMMARY));
    }

    @Test
    void requestTitleGeneration_usesAuxiliaryExtractionScenario() {
        when(requestChatApiPort.requestResponse(anyList(), eq(LlmReasoningScenario.AUXILIARY_EXTRACTION)))
                .thenReturn(CompletableFuture.completedFuture("title"));

        String result = chatProcessor.requestTitleGeneration(
                new ArrayList<>(),
                Prompt.from(1L, 999, "title", false, false, false, false, false, false, true, null, null, null)
        ).join();

        assertThat(result).isEqualTo("title");
        verify(requestChatApiPort).requestResponse(anyList(), eq(LlmReasoningScenario.AUXILIARY_EXTRACTION));
    }
}
