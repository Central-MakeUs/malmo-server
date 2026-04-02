package makeus.cmc.malmo.application.helper.weekly_analysis_report;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.weekly_analysis_report.SaveWeeklyAnalysisReportPort;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class WeeklyAnalysisReportCommandHelper {

    private final SaveWeeklyAnalysisReportPort saveWeeklyAnalysisReportPort;

    public WeeklyAnalysisReport saveWeeklyAnalysisReport(WeeklyAnalysisReport weeklyAnalysisReport) {
        return saveWeeklyAnalysisReportPort.saveWeeklyAnalysisReport(weeklyAnalysisReport);
    }

    public WeeklyAnalysisReport saveAndFlushWeeklyAnalysisReport(WeeklyAnalysisReport weeklyAnalysisReport) {
        return saveWeeklyAnalysisReportPort.saveAndFlushWeeklyAnalysisReport(weeklyAnalysisReport);
    }

    public boolean markAsGeneratingIfUpdatable(Long weeklyAnalysisReportId) {
        return saveWeeklyAnalysisReportPort.markAsGeneratingIfUpdatable(weeklyAnalysisReportId);
    }
}
