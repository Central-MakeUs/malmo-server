package makeus.cmc.malmo.application.service.helper.member;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.SaveMemberPort;
import makeus.cmc.malmo.domain.model.member.Member;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberCommandHelper {

    private final SaveMemberPort saveMemberPort;

    public Member saveMember(Member member) {
        return saveMemberPort.saveMember(member);
    }
}
