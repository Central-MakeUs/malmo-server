package makeus.cmc.malmo.application.port.in.member;

import lombok.Builder;
import lombok.Data;
import makeus.cmc.malmo.domain.value.type.RelationshipStatus;

import java.util.List;

public interface SignUpUseCase {

    void signUp(SignUpCommand command);

    @Data
    @Builder
    class SignUpCommand {
        private Long memberId;
        private List<TermsCommand> terms;
        private String nickname;
        private Long loveTypeId;
        private RelationshipStatus relationshipStatus;
    }

    @Data
    @Builder
    class TermsCommand {
        private Long termsId;
        private Boolean isAgreed;
    }
}
