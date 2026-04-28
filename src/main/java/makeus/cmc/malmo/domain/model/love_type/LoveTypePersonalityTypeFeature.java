package makeus.cmc.malmo.domain.model.love_type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class LoveTypePersonalityTypeFeature {
    private String personalityType;
    private LoveTypeCategory loveTypeCategory;
    private String summary;
    private String keyword1;
    private String keyword2;
    private String keyword3;
    private String strength1;
    private String strength2;
    private String strength3;
    private String weakness;
    private String strengthDesc1;
    private String strengthDesc2;
    private String strengthDesc3;
    private String weaknessDesc;
    private String patternTitle1;
    private String patternTitle2;
    private String patternTitle3;
    private String patternTitle4;
    private String pattern1;
    private String pattern2;
    private String pattern3;
    private String pattern4;
    private String loveTypeFeatureTitle1;
    private String loveTypeFeatureTitle2;
    private String loveTypeFeatureTitle3;
    private String loveTypeFeatureTitle4;
    private String loveTypeFeature1;
    private String loveTypeFeature2;
    private String loveTypeFeature3;
    private String loveTypeFeature4;
    private String datingGuide1;
    private String datingGuide2;
    private String datingGuide3;
    private String bestPersonalityType1;
    private String bestDesc1;
    private String bestPersonalityType2;
    private String bestDesc2;
    private String worstPersonalityType1;
    private String worstDesc1;
    private String worstPersonalityType2;
    private String worstDesc2;

    public static LoveTypePersonalityTypeFeature from(
            String personalityType,
            LoveTypeCategory loveTypeCategory,
            String summary,
            String keyword1,
            String keyword2,
            String keyword3,
            String strength1,
            String strength2,
            String strength3,
            String weakness,
            String strengthDesc1,
            String strengthDesc2,
            String strengthDesc3,
            String weaknessDesc,
            String patternTitle1,
            String patternTitle2,
            String patternTitle3,
            String patternTitle4,
            String pattern1,
            String pattern2,
            String pattern3,
            String pattern4,
            String loveTypeFeatureTitle1,
            String loveTypeFeatureTitle2,
            String loveTypeFeatureTitle3,
            String loveTypeFeatureTitle4,
            String loveTypeFeature1,
            String loveTypeFeature2,
            String loveTypeFeature3,
            String loveTypeFeature4,
            String datingGuide1,
            String datingGuide2,
            String datingGuide3,
            String bestPersonalityType1,
            String bestDesc1,
            String bestPersonalityType2,
            String bestDesc2,
            String worstPersonalityType1,
            String worstDesc1,
            String worstPersonalityType2,
            String worstDesc2
    ) {
        return LoveTypePersonalityTypeFeature.builder()
                .personalityType(normalizePersonalityType(personalityType))
                .loveTypeCategory(loveTypeCategory)
                .summary(summary)
                .keyword1(keyword1)
                .keyword2(keyword2)
                .keyword3(keyword3)
                .strength1(strength1)
                .strength2(strength2)
                .strength3(strength3)
                .weakness(weakness)
                .strengthDesc1(strengthDesc1)
                .strengthDesc2(strengthDesc2)
                .strengthDesc3(strengthDesc3)
                .weaknessDesc(weaknessDesc)
                .patternTitle1(patternTitle1)
                .patternTitle2(patternTitle2)
                .patternTitle3(patternTitle3)
                .patternTitle4(patternTitle4)
                .pattern1(pattern1)
                .pattern2(pattern2)
                .pattern3(pattern3)
                .pattern4(pattern4)
                .loveTypeFeatureTitle1(loveTypeFeatureTitle1)
                .loveTypeFeatureTitle2(loveTypeFeatureTitle2)
                .loveTypeFeatureTitle3(loveTypeFeatureTitle3)
                .loveTypeFeatureTitle4(loveTypeFeatureTitle4)
                .loveTypeFeature1(loveTypeFeature1)
                .loveTypeFeature2(loveTypeFeature2)
                .loveTypeFeature3(loveTypeFeature3)
                .loveTypeFeature4(loveTypeFeature4)
                .datingGuide1(datingGuide1)
                .datingGuide2(datingGuide2)
                .datingGuide3(datingGuide3)
                .bestPersonalityType1(normalizePersonalityType(bestPersonalityType1))
                .bestDesc1(bestDesc1)
                .bestPersonalityType2(normalizePersonalityType(bestPersonalityType2))
                .bestDesc2(bestDesc2)
                .worstPersonalityType1(normalizePersonalityType(worstPersonalityType1))
                .worstDesc1(worstDesc1)
                .worstPersonalityType2(normalizePersonalityType(worstPersonalityType2))
                .worstDesc2(worstDesc2)
                .build();
    }

    private static String normalizePersonalityType(String personalityType) {
        return personalityType == null ? null : personalityType.toUpperCase();
    }
}
