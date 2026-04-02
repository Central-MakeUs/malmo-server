package makeus.cmc.malmo.application.helper.weekly_analysis_report;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.weekly_analysis_report.LoadWeeklyAnalysisReportPort;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class WeeklyAnalysisReportQueryHelper {

    private final LoadWeeklyAnalysisReportPort loadWeeklyAnalysisReportPort;

    public Optional<WeeklyAnalysisReport> getWeeklyReport(Long weeklyAnalysisReportId) {
        return loadWeeklyAnalysisReportPort.loadById(weeklyAnalysisReportId);
    }

    public Optional<WeeklyAnalysisReport> getWeeklyReport(MemberId memberId, LocalDate weekStartDate) {
        return loadWeeklyAnalysisReportPort.loadByMemberIdAndWeekStartDate(memberId, weekStartDate);
    }

    public Optional<WeeklyAnalysisReport> getLatestWeeklyReport(MemberId memberId) {
        return loadWeeklyAnalysisReportPort.loadLatestByMemberId(memberId);
    }

    public Optional<WeeklyAnalysisReport> getLatestWeeklyReportByStatuses(
            MemberId memberId,
            Collection<WeeklyAnalysisReportStatus> statuses
    ) {
        return loadWeeklyAnalysisReportPort.loadLatestByMemberIdAndStatuses(memberId, statuses);
    }

    public List<WeeklyAnalysisReport> getPublishedWeeklyReports(MemberId memberId) {
        return loadWeeklyAnalysisReportPort.loadPublishedReportsByMemberId(memberId);
    }
}
