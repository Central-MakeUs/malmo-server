package makeus.cmc.malmo.adaptor.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.PromptMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.chat.PromptRepository;
import makeus.cmc.malmo.application.port.out.chat.LoadPromptPort;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PromptPersistenceAdapter implements LoadPromptPort {

    private final PromptRepository promptRepository;
    private final PromptMapper promptMapper;

    @Override
    public Optional<Prompt> loadPromptByLevel(int level) {
        return promptRepository.findByLevel(level)
                .map(promptMapper::toDomain);
    }

    @Override
    public Optional<Prompt> loadSystemPrompt() {
        return promptRepository.findByIsForSystemTrue()
                .map(promptMapper::toDomain);
    }

    @Override
    @Deprecated
    public Optional<Prompt> loadSummaryPrompt() {
        return promptRepository.findByIsForSummaryTrue()
                .map(promptMapper::toDomain);
    }

    @Override
    public Optional<Prompt> loadCompletedResponsePrompt() {
        return promptRepository.findByIsForCompletedResponseTrue()
                .map(promptMapper::toDomain);
    }

    @Override
    public Optional<Prompt> loadTotalSummaryPrompt() {
        return promptRepository.findByIsForTotalSummaryTrue()
                .map(promptMapper::toDomain);
    }

    @Override
    public Optional<Prompt> loadGuidelinePrompt(int level) {
        return promptRepository.findByLevelAndIsForGuidelineTrue(level)
                .map(promptMapper::toDomain);
    }

    @Override
    public Optional<Prompt> loadAnswerMetadataPrompt() {
        return promptRepository.findByIsForAnswerMetadataTrue()
                .map(promptMapper::toDomain);
    }

    @Override
    public Optional<Prompt> loadSummaryPromptByLevel(int level) {
        return promptRepository.findByLevelAndIsForSummaryTrue(level)
                .map(promptMapper::toDomain);
    }

    @Override
    public Optional<Prompt> loadTitleGenerationPrompt() {
        return promptRepository.findByIsForTitleGenerationTrue()
                .map(promptMapper::toDomain);
    }

    @Override
    public Optional<Prompt> loadWeeklyReportPrompt() {
        return promptRepository.findByIsForWeeklyReportTrue()
                .map(promptMapper::toDomain);
    }
}
