package makeus.cmc.malmo.adaptor.out.persistence;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.MemberMemoryMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.MemberMemoryRepository;
import makeus.cmc.malmo.application.port.out.LoadMemberMemoryPort;
import makeus.cmc.malmo.domain.model.member.MemberMemory;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class MemberMemoryPersistenceAdapter implements LoadMemberMemoryPort {

    private final MemberMemoryMapper memberMemoryMapper;
    private final MemberMemoryRepository memberMemoryRepository;


    @Override
    public List<MemberMemory> loadMemberMemoryByMemberId(MemberId memberId) {
        return memberMemoryRepository.findByMemberEntityId_Value(memberId.getValue())
                .stream()
                .map(memberMemoryMapper::toDomain)
                .toList();
    }
}
