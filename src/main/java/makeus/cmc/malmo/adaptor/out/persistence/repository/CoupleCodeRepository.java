package makeus.cmc.malmo.adaptor.out.persistence.repository;

import makeus.cmc.malmo.adaptor.out.persistence.entity.couple.CoupleCodeEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoupleCodeRepository extends JpaRepository<CoupleCodeEntity, Long> {

    Optional<CoupleCodeEntity> findByInviteCode(String inviteCode);

    Optional<CoupleCodeEntity> findByMemberEntityId(MemberEntityId memberEntityId);
}
