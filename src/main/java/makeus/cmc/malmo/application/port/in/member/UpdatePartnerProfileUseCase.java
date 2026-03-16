package makeus.cmc.malmo.application.port.in.member;

import lombok.Builder;
import lombok.Data;
import makeus.cmc.malmo.domain.value.type.PartnerLoveTypeCategory;

public interface UpdatePartnerProfileUseCase {

    CreatePartnerProfileUseCase.PartnerProfileResponseDto updatePartnerProfile(UpdatePartnerProfileCommand command);

    @Data
    @Builder
    class UpdatePartnerProfileCommand {
        private Long memberId;
        private String personalityType;
        private boolean personalityTypeProvided;
        private PartnerLoveTypeCategory loveTypeCategory;
        private boolean loveTypeCategoryProvided;
    }
}
