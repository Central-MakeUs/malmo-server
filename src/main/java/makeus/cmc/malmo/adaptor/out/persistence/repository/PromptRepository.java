package makeus.cmc.malmo.adaptor.out.persistence.repository;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.PromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PromptRepository extends JpaRepository<PromptEntity, Long> {

    @Query("SELECT p FROM PromptEntity p WHERE p.isForMetadata = false ORDER BY p.level ASC LIMIT 1")
    Optional<PromptEntity> findMinLevelPromptNotForMetadata();

    @Query("SELECT p FROM PromptEntity p WHERE p.level = :level")
    Optional<PromptEntity> findByLevel(int level);
}
