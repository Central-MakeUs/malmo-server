package makeus.cmc.malmo.adaptor.out.persistence;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.PromptMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.PromptRepository;
import makeus.cmc.malmo.application.port.out.LoadPromptPort;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PromptPersistenceAdapter implements LoadPromptPort {

    private final PromptRepository promptRepository;
    private final PromptMapper promptMapper;

    @Override
    public Optional<Prompt> loadPromptMinLevelPrompt() {
        return promptRepository.findMinLevelPromptNotForMetadata()
                .map(promptMapper::toDomain);
    }

    @Override
    public Optional<Prompt> loadPromptByLevel(int level) {
        return promptRepository.findByLevel(level)
                .map(promptMapper::toDomain);
    }
}
