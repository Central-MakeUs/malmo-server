package makeus.cmc.malmo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.out.AbstractOpenAiCompatibleApiClient;
import makeus.cmc.malmo.application.port.out.chat.RequestChatApiPort;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmStartupLogger {

    private final RequestChatApiPort requestChatApiPort;

    @EventListener(ApplicationReadyEvent.class)
    public void logActiveLlmConfiguration() {
        if (requestChatApiPort instanceof AbstractOpenAiCompatibleApiClient llmClient) {
            log.info(
                    "Active LLM configuration: provider={}, model={}, defaultReasoningEffort={}, scenarioReasoningEfforts={}",
                    llmClient.currentProviderName(),
                    llmClient.currentModel(),
                    llmClient.currentDefaultReasoningEffort(),
                    llmClient.currentScenarioReasoningEfforts()
            );
            return;
        }

        log.info("Active LLM configuration: clientClass={}", requestChatApiPort.getClass().getName());
    }
}
