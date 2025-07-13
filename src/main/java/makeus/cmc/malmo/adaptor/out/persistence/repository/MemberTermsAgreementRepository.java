package makeus.cmc.malmo.adaptor.out.persistence.repository;

import makeus.cmc.malmo.adaptor.out.persistence.entity.terms.MemberTermsAgreementEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.TermsEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreementEntity, Long> {
    Optional<MemberTermsAgreementEntity> findByMemberEntityIdAndTermsEntityId(MemberEntityId memberEntityId, TermsEntityId termsEntityId);

    boolean existsByTermsEntityId(TermsEntityId termsEntityId);
}
