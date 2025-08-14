package makeus.cmc.malmo.adaptor.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberMemoryEntity;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.MemberMemoryMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.member.MemberMemoryRepository;
import makeus.cmc.malmo.application.port.out.member.LoadMemberMemoryPort;
import makeus.cmc.malmo.application.port.out.member.SaveMemberMemoryPort;
import makeus.cmc.malmo.domain.model.member.MemberMemory;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class MemberMemoryPersistenceAdapter implements LoadMemberMemoryPort, SaveMemberMemoryPort {

    private final MemberMemoryMapper memberMemoryMapper;
    private final MemberMemoryRepository memberMemoryRepository;


    @Override
    public List<MemberMemory> loadMemberMemoryByMemberId(MemberId memberId) {
        return memberMemoryRepository.findByMemberEntityId_Value(memberId.getValue())
                .stream()
                .map(memberMemoryMapper::toDomain)
                .toList();
    }

    @Override
    public void saveMemberMemory(MemberMemory memberMemory) {
        MemberMemoryEntity entity = memberMemoryMapper.toEntity(memberMemory);
        memberMemoryRepository.save(entity);
    }

    @Override
    public void deleteAllMemory(MemberId memberId) {
        memberMemoryRepository.updateMemberMemoryStateToDeleted(memberId.getValue());
    }
}
