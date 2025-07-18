package makeus.cmc.malmo.domain.service;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.LoadPromptPort;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class LoadPromptDomainService {
    public static final int SYSTEM_PROMPT_LEVEL = -2;
    public static final int SUMMARY_PROMPT = -1;
    private final LoadPromptPort loadPromptPort;

    public Prompt getSystemPrompt() {
        return loadPromptPort.loadPromptByLevel(SYSTEM_PROMPT_LEVEL)
                .orElseThrow(() -> new IllegalStateException("System prompt not found"));
    }

    public Prompt getSummaryPrompt() {
        return loadPromptPort.loadPromptByLevel(SUMMARY_PROMPT)
                .orElseThrow(() -> new IllegalStateException("Summary prompt not found"));
    }

    public Prompt getPromptByLevel(int level) {
        return loadPromptPort.loadPromptByLevel(level)
                .orElseThrow(() -> new IllegalStateException("Prompt not found for level: " + level));
    }
}
