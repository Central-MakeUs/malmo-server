package makeus.cmc.malmo.application.port.in;

import lombok.Builder;
import lombok.Data;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

import java.util.List;

public interface GetLoveTypeMbtiResultUseCase {

    LoveTypeMbtiResultResponse getResult(GetLoveTypeMbtiResultCommand command);

    @Data
    @Builder
    class GetLoveTypeMbtiResultCommand {
        private String mbti;
        private LoveTypeCategory loveTypeCategory;
    }

    @Data
    @Builder
    class LoveTypeMbtiResultResponse {
        private String mbti;
        private LoveTypeCategory loveTypeCategory;
        private String summary;
        private List<String> keywords;
        private List<TitleDescriptionItem> strengths;
        private List<TitleDescriptionItem> weaknesses;
        private List<TitleDescriptionItem> patterns;
        private List<TitleDescriptionItem> loveTypeFeatures;
        private List<String> datingGuides;
        private List<MbtiDescriptionItem> bestMatches;
        private List<MbtiDescriptionItem> worstMatches;
    }

    @Data
    @Builder
    class TitleDescriptionItem {
        private String title;
        private String description;
    }

    @Data
    @Builder
    class MbtiDescriptionItem {
        private String mbti;
        private String description;
    }
}
