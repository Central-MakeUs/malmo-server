package makeus.cmc.malmo.domain.model.love_type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class LoveTypeMbtiPrompt {
    private String mbti;
    private LoveTypeCategory loveTypeCategory;
    private String prompts;

    public static LoveTypeMbtiPrompt from(String mbti, LoveTypeCategory loveTypeCategory, String prompts) {
        return LoveTypeMbtiPrompt.builder()
                .mbti(normalizeMbti(mbti))
                .loveTypeCategory(loveTypeCategory)
                .prompts(prompts)
                .build();
    }

    private static String normalizeMbti(String mbti) {
        return mbti == null ? null : mbti.toUpperCase();
    }
}
