package makeus.cmc.malmo.domain.service;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.LoadMemberPort;
import makeus.cmc.malmo.domain.exception.MemberNotFoundException;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberDomainService {

    private final LoadMemberPort loadMemberPort;

    public Member getMemberById(MemberId memberId) {
        return loadMemberPort.loadMemberById(memberId.getValue())
                .orElseThrow(MemberNotFoundException::new);
    }

    public Member createMember(Provider provider, String providerId, String email, InviteCodeValue inviteCode) {
        return Member.createMember(
                provider,
                providerId,
                MemberRole.MEMBER,
                MemberState.BEFORE_ONBOARDING,
                email,
                inviteCode
        );
    }

    //  D-day 정보는 다음과 같이 구분해서 활용
    //  - 단기연애 = ~ 100일
    //  - 중기연애 = 101일 ~ 1년 미만
    //  - 장기연애 = 1년 이상
    public String getMemberDDayState(LocalDate startLoveDate) {
        LocalDate today = LocalDate.now();
        long daysBetween = DAYS.between(startLoveDate, today);
        if (daysBetween < 100) {
            return "단기연애";
        } else if (daysBetween < 365) {
            return "중기연애";
        } else {
            return "장기연애";
        }
    }

}
