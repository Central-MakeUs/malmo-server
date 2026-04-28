package makeus.cmc.malmo.application.helper.love_type;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.LoadLoveTypePersonalityTypePromptPort;
import makeus.cmc.malmo.domain.model.love_type.LoveTypePersonalityTypePrompt;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoveTypePersonalityTypePromptQueryHelper {

    private final LoadLoveTypePersonalityTypePromptPort loadLoveTypePersonalityTypePromptPort;

    public Optional<LoveTypePersonalityTypePrompt> findByPersonalityTypeAndLoveTypeCategory(
            String personalityType,
            LoveTypeCategory loveTypeCategory
    ) {
        return loadLoveTypePersonalityTypePromptPort
                .loadByPersonalityTypeAndLoveTypeCategory(personalityType, loveTypeCategory);
    }
}
