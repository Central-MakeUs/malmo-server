package makeus.cmc.malmo.application.port.in.weekly_analysis_report;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;

public interface GenerateWeeklyAnalysisReportUseCase {

    CompletableFuture<Void> generateWeeklyAnalysisReport(GenerateWeeklyAnalysisReportCommand command);

    @Getter
    @RequiredArgsConstructor
    class GenerateWeeklyAnalysisReportCommand {
        private final Long weeklyAnalysisReportId;
    }
}
