package makeus.cmc.malmo.adaptor.out.persistence.repository;

import makeus.cmc.malmo.adaptor.out.persistence.entity.terms.TermsEntity;
import makeus.cmc.malmo.domain.value.type.TermsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TermsRepository extends JpaRepository<TermsEntity, Long> {
    @Query(value =
            "SELECT t.* FROM terms_entity t " +
                    "INNER JOIN (" +
                    "  SELECT terms_type, MAX(version) as max_version " +
                    "  FROM terms_entity " +
                    "  GROUP BY terms_type" +
                    ") tm ON t.terms_type = tm.terms_type AND t.version = tm.max_version",
            nativeQuery = true)
    List<TermsEntity> findLatestTermsForAllTypes();

    boolean existsByTermsTypeAndVersion(TermsType termsType, float version);
}
