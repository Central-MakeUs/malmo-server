package makeus.cmc.malmo.application.port.in;

import lombok.Builder;
import lombok.Data;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

import java.util.List;

public interface GetLoveTypePersonalityTypeResultUseCase {

    LoveTypePersonalityTypeResultResponse getResult(GetLoveTypePersonalityTypeResultCommand command);

    @Data
    @Builder
    class GetLoveTypePersonalityTypeResultCommand {
        private String personalityType;
        private LoveTypeCategory loveTypeCategory;
    }

    @Data
    @Builder
    class LoveTypePersonalityTypeResultResponse {
        private String personalityType;
        private LoveTypeCategory loveTypeCategory;
        private String summary;
        private List<String> keywords;
        private List<TitleDescriptionItem> strengths;
        private List<TitleDescriptionItem> weaknesses;
        private List<TitleDescriptionItem> patterns;
        private List<TitleDescriptionItem> loveTypeFeatures;
        private List<TitleDescriptionItem> datingGuides;
        private List<PersonalityTypeDescriptionItem> bestMatches;
        private List<PersonalityTypeDescriptionItem> worstMatches;
    }

    @Data
    @Builder
    class TitleDescriptionItem {
        private String title;
        private String description;
    }

    @Data
    @Builder
    class PersonalityTypeDescriptionItem {
        private String personalityType;
        private String description;
    }
}
