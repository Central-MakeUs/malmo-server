package makeus.cmc.malmo.application.port.out.member;

import makeus.cmc.malmo.domain.model.member.Member;

public interface SaveMemberPort {
    Member saveMember(Member member);

    Member saveMemberIfAbsent(Member member);
}
