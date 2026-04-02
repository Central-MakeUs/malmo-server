package makeus.cmc.malmo.adaptor.out.persistence.repository.weekly_analysis_report;

import makeus.cmc.malmo.adaptor.out.persistence.entity.weekly_analysis_report.WeeklyAnalysisReportEntity;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WeeklyAnalysisReportRepository extends JpaRepository<WeeklyAnalysisReportEntity, Long> {

    Optional<WeeklyAnalysisReportEntity> findByMemberId_ValueAndWeekStartDate(Long memberId, LocalDate weekStartDate);

    Optional<WeeklyAnalysisReportEntity> findTopByMemberId_ValueOrderByWeekStartDateDesc(Long memberId);

    Optional<WeeklyAnalysisReportEntity> findTopByMemberId_ValueAndStatusInOrderByWeekStartDateDesc(
            Long memberId,
            Collection<WeeklyAnalysisReportStatus> statuses
    );

    List<WeeklyAnalysisReportEntity> findAllByMemberId_ValueAndStatusOrderByWeekStartDateDesc(Long memberId, WeeklyAnalysisReportStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE WeeklyAnalysisReportEntity report
            SET report.status = :nextStatus,
                report.failedReason = null
            WHERE report.id = :weeklyAnalysisReportId
              AND report.status IN :currentStatuses
            """)
    int updateStatusIfCurrentStatusIn(
            @Param("weeklyAnalysisReportId") Long weeklyAnalysisReportId,
            @Param("currentStatuses") Collection<WeeklyAnalysisReportStatus> currentStatuses,
            @Param("nextStatus") WeeklyAnalysisReportStatus nextStatus
    );
}
