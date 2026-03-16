package makeus.cmc.malmo.adaptor.out.persistence.repository;

import makeus.cmc.malmo.adaptor.out.persistence.entity.LoveTypeMbtiPromptEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.LoveTypeMbtiPromptEntityId;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoveTypeMbtiPromptRepository extends JpaRepository<LoveTypeMbtiPromptEntity, LoveTypeMbtiPromptEntityId> {
    Optional<LoveTypeMbtiPromptEntity> findByMbtiIgnoreCaseAndLoveTypeCategory(String mbti, LoveTypeCategory loveTypeCategory);
}
