package makeus.cmc.malmo.application.port.out;

import makeus.cmc.malmo.domain.model.member.MemberMemory;

public interface SaveMemberMemoryPort {

    void saveMemberMemory(MemberMemory memberMemory);
}
