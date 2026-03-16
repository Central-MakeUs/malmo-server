package makeus.cmc.malmo.application.port.in.member;

import lombok.Builder;
import lombok.Data;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.RelationshipStatus;

public interface UpdateMemberUseCase {

    UpdateMemberResponseDto updateMember(UpdateMemberCommand command);

    @Data
    @Builder
    class UpdateMemberCommand {
        private Long memberId;
        private String nickname;
        private RelationshipStatus relationshipStatus;
        private String mbti;
        private LoveTypeCategory loveTypeCategory;
    }

    @Data
    @Builder
    class UpdateMemberResponseDto {
        private String nickname;
        private RelationshipStatus relationshipStatus;
        private String mbti;
        private LoveTypeCategory loveTypeCategory;
    }
}
