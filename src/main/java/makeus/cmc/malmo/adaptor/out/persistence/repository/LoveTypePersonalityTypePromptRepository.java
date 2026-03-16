package makeus.cmc.malmo.adaptor.out.persistence.repository;

import makeus.cmc.malmo.adaptor.out.persistence.entity.LoveTypePersonalityTypePromptEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.LoveTypePersonalityTypePromptEntityId;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoveTypePersonalityTypePromptRepository
        extends JpaRepository<LoveTypePersonalityTypePromptEntity, LoveTypePersonalityTypePromptEntityId> {
    Optional<LoveTypePersonalityTypePromptEntity> findByPersonalityTypeIgnoreCaseAndLoveTypeCategory(
            String personalityType,
            LoveTypeCategory loveTypeCategory
    );
}
