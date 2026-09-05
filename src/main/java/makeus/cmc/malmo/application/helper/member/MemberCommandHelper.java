package makeus.cmc.malmo.application.helper.member;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.member.SaveMemberPort;
import makeus.cmc.malmo.domain.model.member.Member;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberCommandHelper {

    private final SaveMemberPort saveMemberPort;

    public Member saveMember(Member member) {
        return saveMemberPort.saveMember(member);
    }

    public Member saveMemberIfAbsent(Member member) {
        return saveMemberPort.saveMemberIfAbsent(member);
    }
}
