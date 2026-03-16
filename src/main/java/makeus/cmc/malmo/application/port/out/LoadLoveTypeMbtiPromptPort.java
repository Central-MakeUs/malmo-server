package makeus.cmc.malmo.application.port.out;

import makeus.cmc.malmo.domain.model.love_type.LoveTypeMbtiPrompt;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

import java.util.Optional;

public interface LoadLoveTypeMbtiPromptPort {
    Optional<LoveTypeMbtiPrompt> loadByMbtiAndLoveTypeCategory(String mbti, LoveTypeCategory loveTypeCategory);
}
