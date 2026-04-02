package makeus.cmc.malmo.application.port.out.chat;

import makeus.cmc.malmo.domain.model.chat.Prompt;

import java.util.Optional;

public interface LoadPromptPort {
    Optional<Prompt> loadPromptByLevel(int level);
    
    Optional<Prompt> loadSystemPrompt();
    
    @Deprecated
    Optional<Prompt> loadSummaryPrompt();
    
    Optional<Prompt> loadCompletedResponsePrompt();
    
    Optional<Prompt> loadTotalSummaryPrompt();
    
    Optional<Prompt> loadGuidelinePrompt(int level);
    
    Optional<Prompt> loadAnswerMetadataPrompt();

    Optional<Prompt> loadSummaryPromptByLevel(int level);

    Optional<Prompt> loadTitleGenerationPrompt();

    Optional<Prompt> loadWeeklyReportPrompt();
}
