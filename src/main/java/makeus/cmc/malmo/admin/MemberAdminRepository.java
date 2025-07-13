package makeus.cmc.malmo.admin;

import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberEntity;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MemberAdminRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findMemberByNicknameAndFirebaseTokenAndMemberRole(String nickname, String firebaseToken, MemberRole memberRole);
}
