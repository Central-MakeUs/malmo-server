package makeus.cmc.malmo.application.port.in.weekly_analysis_report;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReportContent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface GetWeeklyAnalysisReportUseCase {

    GetWeeklyAnalysisReportResponse getWeeklyAnalysisReport(GetWeeklyAnalysisReportCommand command);

    @Getter
    @RequiredArgsConstructor
    class GetWeeklyAnalysisReportCommand {
        private final Long userId;
        private final LocalDate weekStartDate;
    }

    @Getter
    @RequiredArgsConstructor
    class GetWeeklyAnalysisReportResponse {
        private final LocalDate weekStartDate;
        private final LocalDate weekEndDate;
        private final String status;
        private final LocalDateTime generatedAt;
        private final String schemaVersion;
        private final WeeklyAnalysisReportContent.Period period;
        private final WeeklyAnalysisReportContent.Overview overview;
        private final List<WeeklyAnalysisReportContent.TopTopic> topTopics;
        private final WeeklyAnalysisReportContent.MoodByTime moodByTime;
        private final WeeklyAnalysisReportContent.Conflict conflict;
        private final WeeklyAnalysisReportContent.BehaviorPattern behaviorPattern;
        private final WeeklyAnalysisReportContent.Solution solution;
    }
}
