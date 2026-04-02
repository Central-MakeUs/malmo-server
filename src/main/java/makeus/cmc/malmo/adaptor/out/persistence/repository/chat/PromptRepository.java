package makeus.cmc.malmo.adaptor.out.persistence.repository.chat;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.PromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PromptRepository extends JpaRepository<PromptEntity, Long> {

    @Query("SELECT p FROM PromptEntity p WHERE p.level = :level")
    Optional<PromptEntity> findByLevel(int level);

    @Query("SELECT p FROM PromptEntity p WHERE p.isForSystem = true")
    Optional<PromptEntity> findByIsForSystemTrue();

    @Query("SELECT p FROM PromptEntity p WHERE p.isForSummary = true")
    Optional<PromptEntity> findByIsForSummaryTrue();

    @Query("SELECT p FROM PromptEntity p WHERE p.isForCompletedResponse = true")
    Optional<PromptEntity> findByIsForCompletedResponseTrue();

    @Query("SELECT p FROM PromptEntity p WHERE p.isForTotalSummary = true")
    Optional<PromptEntity> findByIsForTotalSummaryTrue();

    @Query("SELECT p FROM PromptEntity p WHERE p.level = :level AND p.isForGuideline = true")
    Optional<PromptEntity> findByLevelAndIsForGuidelineTrue(@Param("level") int level);

    @Query("SELECT p FROM PromptEntity p WHERE p.isForAnswerMetadata = true")
    Optional<PromptEntity> findByIsForAnswerMetadataTrue();

    @Query("select p from PromptEntity p where p.level = ?1 and p.isForSummary = true")
    Optional<PromptEntity> findByLevelAndIsForSummaryTrue(int level);

    @Query("SELECT p FROM PromptEntity p WHERE p.isForTitleGeneration = true")
    Optional<PromptEntity> findByIsForTitleGenerationTrue();

    @Query("SELECT p FROM PromptEntity p WHERE p.isForWeeklyReport = true")
    Optional<PromptEntity> findByIsForWeeklyReportTrue();
}
