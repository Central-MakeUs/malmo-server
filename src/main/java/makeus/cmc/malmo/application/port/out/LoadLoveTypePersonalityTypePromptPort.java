package makeus.cmc.malmo.application.port.out;

import makeus.cmc.malmo.domain.model.love_type.LoveTypePersonalityTypePrompt;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

import java.util.Optional;

public interface LoadLoveTypePersonalityTypePromptPort {
    Optional<LoveTypePersonalityTypePrompt> loadByPersonalityTypeAndLoveTypeCategory(
            String personalityType,
            LoveTypeCategory loveTypeCategory
    );
}
