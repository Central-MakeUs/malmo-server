package makeus.cmc.malmo.domain.value.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PartnerLoveTypeCategory {
    STABLE_TYPE("안정형"),
    ANXIETY_TYPE("불안형"),
    AVOIDANCE_TYPE("회피형"),
    CONFUSION_TYPE("혼란형"),
    UNKNOWN("모르겠어요");

    private final String description;

    public static PartnerLoveTypeCategory fromLoveTypeCategory(LoveTypeCategory loveTypeCategory) {
        if (loveTypeCategory == null) {
            return UNKNOWN;
        }

        return valueOf(loveTypeCategory.name());
    }
}
