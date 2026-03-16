package makeus.cmc.malmo.application.port.in.member;

import lombok.Builder;
import lombok.Data;
import makeus.cmc.malmo.domain.value.type.PartnerLoveTypeCategory;

public interface GetPartnerUseCase {

    PartnerMemberResponseDto getPartnerInfo(PartnerInfoCommand command);

    @Data
    @Builder
    class PartnerInfoCommand {
        private Long userId;
    }

    @Data
    @Builder
    class PartnerMemberResponseDto {
        private String mbti;
        private PartnerLoveTypeCategory loveTypeCategory;
        private String description;
    }
}
