package makeus.cmc.malmo.adaptor.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.LoveTypePersonalityTypePromptMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.LoveTypePersonalityTypePromptRepository;
import makeus.cmc.malmo.application.port.out.LoadLoveTypePersonalityTypePromptPort;
import makeus.cmc.malmo.domain.model.love_type.LoveTypePersonalityTypePrompt;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoveTypePersonalityTypePromptPersistenceAdapter implements LoadLoveTypePersonalityTypePromptPort {

    private final LoveTypePersonalityTypePromptRepository loveTypePersonalityTypePromptRepository;
    private final LoveTypePersonalityTypePromptMapper loveTypePersonalityTypePromptMapper;

    @Override
    public Optional<LoveTypePersonalityTypePrompt> loadByPersonalityTypeAndLoveTypeCategory(
            String personalityType,
            LoveTypeCategory loveTypeCategory
    ) {
        return loveTypePersonalityTypePromptRepository
                .findByPersonalityTypeIgnoreCaseAndLoveTypeCategory(personalityType, loveTypeCategory)
                .map(loveTypePersonalityTypePromptMapper::toDomain);
    }
}
