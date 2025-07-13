package makeus.cmc.malmo.domain.model.love_type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class LoveType {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String imageUrl;
    private LoveTypeCategory loveTypeCategory;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    public static LoveTypeCategory findLoveTypeCategory(float avoidanceRate, float anxietyRate) {
        float avoidanceRateResult = avoidanceRate / 18.0f;
        float anxietyRateResult = anxietyRate / 18.0f;

        if (avoidanceRateResult < 2.33 && anxietyRateResult < 2.61) {
            return LoveTypeCategory.STABLE_TYPE;
        } else if (avoidanceRateResult >= 2.33 && anxietyRateResult < 2.61) {
            return LoveTypeCategory.AVOIDANCE_TYPE;
        } else if (avoidanceRateResult < 2.33 && anxietyRateResult >= 2.61) {
            return LoveTypeCategory.ANXIETY_TYPE;
        } else {
            return LoveTypeCategory.CONFUSION_TYPE;
        }
    }

    public static LoveType from(Long id, String title, String summary, String content, String imageUrl,
                                LoveTypeCategory loveTypeCategory, LocalDateTime createdAt, LocalDateTime modifiedAt,
                                LocalDateTime deletedAt) {
        return LoveType.builder()
                .id(id)
                .title(title)
                .summary(summary)
                .content(content)
                .imageUrl(imageUrl)
                .loveTypeCategory(loveTypeCategory)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public void updateByAdmin(String title, String summary, String content, String imageUrl) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.imageUrl = imageUrl;
    }
}