package makeus.cmc.malmo.application.port.in.weekly_analysis_report;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

public interface TriggerWeeklyAnalysisReportUseCase {

    TriggerWeeklyAnalysisReportResponse triggerWeeklyAnalysisReports(TriggerWeeklyAnalysisReportCommand command);

    @Getter
    @RequiredArgsConstructor
    class TriggerWeeklyAnalysisReportCommand {
        private final LocalDate weekStartDate;
    }

    @Getter
    @RequiredArgsConstructor
    class TriggerWeeklyAnalysisReportResponse {
        private final LocalDate weekStartDate;
        private final LocalDate weekEndDate;
        private final int candidateMemberCount;
        private final int reservedCount;
        private final int republishedFailedCount;
        private final int skippedExistingCount;
        private final int skippedIneligibleCount;
    }
}
