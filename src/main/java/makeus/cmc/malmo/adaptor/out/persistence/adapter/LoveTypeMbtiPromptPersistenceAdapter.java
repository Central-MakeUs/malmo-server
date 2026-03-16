package makeus.cmc.malmo.adaptor.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.LoveTypeMbtiPromptMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.LoveTypeMbtiPromptRepository;
import makeus.cmc.malmo.application.port.out.LoadLoveTypeMbtiPromptPort;
import makeus.cmc.malmo.domain.model.love_type.LoveTypeMbtiPrompt;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoveTypeMbtiPromptPersistenceAdapter implements LoadLoveTypeMbtiPromptPort {

    private final LoveTypeMbtiPromptRepository loveTypeMbtiPromptRepository;
    private final LoveTypeMbtiPromptMapper loveTypeMbtiPromptMapper;

    @Override
    public Optional<LoveTypeMbtiPrompt> loadByMbtiAndLoveTypeCategory(String mbti, LoveTypeCategory loveTypeCategory) {
        return loveTypeMbtiPromptRepository.findByMbtiIgnoreCaseAndLoveTypeCategory(mbti, loveTypeCategory)
                .map(loveTypeMbtiPromptMapper::toDomain);
    }
}
