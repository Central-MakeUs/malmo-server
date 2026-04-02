package makeus.cmc.malmo.adaptor.out.persistence.repository.chat;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.DetailedPromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DetailedPromptRepository extends JpaRepository<DetailedPromptEntity, Long> {

    @Query("SELECT dp FROM DetailedPromptEntity dp WHERE dp.level = :level AND dp.detailedLevel = :detailedLevel")
    Optional<DetailedPromptEntity> findByLevelAndDetailedLevel(@Param("level") int level, @Param("detailedLevel") int detailedLevel);

    @Query("SELECT dp FROM DetailedPromptEntity dp WHERE dp.level = :level AND dp.detailedLevel = :detailedLevel AND dp.isForValidation = true")
    Optional<DetailedPromptEntity> findByLevelAndDetailedLevelAndIsForValidation(@Param("level") int level, @Param("detailedLevel") int detailedLevel);

    @Query("SELECT dp FROM DetailedPromptEntity dp WHERE dp.level = :level AND dp.detailedLevel = :detailedLevel AND dp.isForSummary = true")
    Optional<DetailedPromptEntity> findByLevelAndDetailedLevelAndIsForSummary(@Param("level") int level, @Param("detailedLevel") int detailedLevel);

    @Query("SELECT dp FROM DetailedPromptEntity dp WHERE dp.level = :level AND dp.detailedLevel = :detailedLevel AND dp.isLastDetailedPrompt = true")
    Optional<DetailedPromptEntity> findByLevelAndDetailedLevelAndIsLastDetailedPrompt(@Param("level") int level, @Param("detailedLevel") int detailedLevel);

    @Query("SELECT dp FROM DetailedPromptEntity dp WHERE dp.level = :level ORDER BY dp.detailedLevel")
    List<DetailedPromptEntity> findByLevelOrderByDetailedLevel(@Param("level") int level);

    @Query("SELECT dp FROM DetailedPromptEntity dp WHERE dp.level = :level AND dp.detailedLevel = :detailedLevel AND dp.isForGuideline = true")
    Optional<DetailedPromptEntity> findByLevelAndDetailedLevelAndIsForGuidelineTrue(@Param("level") int level, @Param("detailedLevel") int detailedLevel);

    @Query("SELECT MAX(dp.detailedLevel) FROM DetailedPromptEntity dp WHERE dp.level = :level AND dp.isLastDetailedPrompt = true")
    Optional<Integer> findLastDetailedLevelByLevel(@Param("level") int level);
}
