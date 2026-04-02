package makeus.cmc.malmo.application.port.out.weekly_analysis_report;

import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;

public interface SaveWeeklyAnalysisReportPort {

    WeeklyAnalysisReport saveWeeklyAnalysisReport(WeeklyAnalysisReport weeklyAnalysisReport);

    WeeklyAnalysisReport saveAndFlushWeeklyAnalysisReport(WeeklyAnalysisReport weeklyAnalysisReport);

    boolean markAsGeneratingIfUpdatable(Long weeklyAnalysisReportId);
}
