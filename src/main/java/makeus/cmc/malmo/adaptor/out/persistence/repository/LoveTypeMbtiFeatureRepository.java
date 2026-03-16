package makeus.cmc.malmo.adaptor.out.persistence.repository;

import makeus.cmc.malmo.adaptor.out.persistence.entity.LoveTypeMbtiFeatureEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.LoveTypeMbtiFeatureEntityId;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoveTypeMbtiFeatureRepository extends JpaRepository<LoveTypeMbtiFeatureEntity, LoveTypeMbtiFeatureEntityId> {
    Optional<LoveTypeMbtiFeatureEntity> findByMbtiIgnoreCaseAndLoveTypeCategory(String mbti, LoveTypeCategory loveTypeCategory);
}
