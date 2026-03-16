package makeus.cmc.malmo.application.helper.love_type;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.LoadLoveTypeMbtiPromptPort;
import makeus.cmc.malmo.domain.model.love_type.LoveTypeMbtiPrompt;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoveTypeMbtiPromptQueryHelper {

    private final LoadLoveTypeMbtiPromptPort loadLoveTypeMbtiPromptPort;

    public Optional<LoveTypeMbtiPrompt> findByMbtiAndLoveTypeCategory(String mbti, LoveTypeCategory loveTypeCategory) {
        return loadLoveTypeMbtiPromptPort.loadByMbtiAndLoveTypeCategory(mbti, loveTypeCategory);
    }
}
