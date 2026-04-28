package makeus.cmc.malmo.application.port.in.member;

import lombok.Builder;
import lombok.Data;
import makeus.cmc.malmo.domain.value.type.PartnerLoveTypeCategory;

public interface CreatePartnerProfileUseCase {

    PartnerProfileResponseDto createPartnerProfile(CreatePartnerProfileCommand command);

    @Data
    @Builder
    class CreatePartnerProfileCommand {
        private Long memberId;
        private String personalityType;
        private PartnerLoveTypeCategory loveTypeCategory;
    }

    @Data
    @Builder
    class PartnerProfileResponseDto {
        private String personalityType;
        private PartnerLoveTypeCategory loveTypeCategory;
        private String description;
    }
}
