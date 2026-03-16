package makeus.cmc.malmo.domain.model.love_type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class LoveTypePersonalityTypePrompt {
    private String personalityType;
    private LoveTypeCategory loveTypeCategory;
    private String prompts;

    public static LoveTypePersonalityTypePrompt from(
            String personalityType,
            LoveTypeCategory loveTypeCategory,
            String prompts
    ) {
        return LoveTypePersonalityTypePrompt.builder()
                .personalityType(normalizePersonalityType(personalityType))
                .loveTypeCategory(loveTypeCategory)
                .prompts(prompts)
                .build();
    }

    private static String normalizePersonalityType(String personalityType) {
        return personalityType == null ? null : personalityType.toUpperCase();
    }
}
