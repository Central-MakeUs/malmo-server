package makeus.cmc.malmo.adaptor.out.persistence.repository;

import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberMemoryRepository extends JpaRepository<MemberMemoryEntity, Long> {

    List<MemberMemoryEntity> findByMemberEntityId_Value(Long memberId);
}
