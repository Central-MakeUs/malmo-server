package makeus.cmc.malmo.application.port.out.weekly_analysis_report;

import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadWeeklyAnalysisReportPort {

    Optional<WeeklyAnalysisReport> loadById(Long weeklyAnalysisReportId);

    Optional<WeeklyAnalysisReport> loadByMemberIdAndWeekStartDate(MemberId memberId, LocalDate weekStartDate);

    Optional<WeeklyAnalysisReport> loadLatestByMemberId(MemberId memberId);

    Optional<WeeklyAnalysisReport> loadLatestByMemberIdAndStatuses(
            MemberId memberId,
            Collection<WeeklyAnalysisReportStatus> statuses
    );

    List<WeeklyAnalysisReport> loadPublishedReportsByMemberId(MemberId memberId);
}
