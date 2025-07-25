package makeus.cmc.malmo.application.port.in;

import lombok.Builder;
import lombok.Data;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

import java.time.LocalDate;

public interface GetMemberUseCase {

    MemberResponseDto getMemberInfo(MemberInfoCommand command);

    @Data
    @Builder
    class MemberInfoCommand {
        private Long userId;
    }

    @Data
    @Builder
    class MemberResponseDto {
        private MemberState memberState;
        private LocalDate startLoveDate;

        private LoveTypeCategory loveTypeCategory;

        private int totalChatRoomCount;
        private int totalCoupleQuestionCount;

        private float avoidanceRate;
        private float anxietyRate;
        private String nickname;
        private String email;
    }
}
