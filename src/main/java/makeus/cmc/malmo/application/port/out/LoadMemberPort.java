package makeus.cmc.malmo.application.port.out;

import lombok.AllArgsConstructor;
import lombok.Data;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.model.member.Provider;

import java.time.LocalDate;
import java.util.Optional;

public interface LoadMemberPort {
    Optional<Member> loadMemberByProviderId(Provider providerJpa, String providerId);
    Optional<Member> loadMemberById(Long memberId);
    Optional<MemberResponseRepositoryDto> loadMemberDetailsById(Long memberId);

    @Data
    @AllArgsConstructor
    class MemberResponseRepositoryDto {
        private String memberState;
        private LocalDate startLoveDate;
        private Long loveTypeId;
        private String loveTypeTitle;
        private float avoidanceRate;
        private float anxietyRate;
        private String nickname;
        private String email;
    }
}
