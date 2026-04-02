package makeus.cmc.malmo.application.port.in.weekly_analysis_report;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface GetWeeklyAnalysisReportListUseCase {

    GetWeeklyAnalysisReportListResponse getWeeklyAnalysisReportList(GetWeeklyAnalysisReportListCommand command);

    @Getter
    @RequiredArgsConstructor
    class GetWeeklyAnalysisReportListCommand {
        private final Long userId;
    }

    @Getter
    @RequiredArgsConstructor
    class GetWeeklyAnalysisReportListResponse {
        private final List<GetWeeklyAnalysisReportListItemResponse> reports;
        private final long totalCount;
    }

    @Getter
    @RequiredArgsConstructor
    class GetWeeklyAnalysisReportListItemResponse {
        private final LocalDate weekStartDate;
        private final LocalDate weekEndDate;
        private final String status;
        private final LocalDateTime generatedAt;
    }
}
