package makeus.cmc.malmo.adaptor.out.persistence.repository;

import makeus.cmc.malmo.adaptor.out.persistence.entity.LoveTypePersonalityTypeFeatureEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.LoveTypePersonalityTypeFeatureEntityId;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoveTypePersonalityTypeFeatureRepository
        extends JpaRepository<LoveTypePersonalityTypeFeatureEntity, LoveTypePersonalityTypeFeatureEntityId> {
    Optional<LoveTypePersonalityTypeFeatureEntity> findByPersonalityTypeIgnoreCaseAndLoveTypeCategory(
            String personalityType,
            LoveTypeCategory loveTypeCategory
    );
}
