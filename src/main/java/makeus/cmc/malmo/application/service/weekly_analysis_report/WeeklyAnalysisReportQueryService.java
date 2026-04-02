package makeus.cmc.malmo.application.service.weekly_analysis_report;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.aop.CheckValidMember;
import makeus.cmc.malmo.application.exception.WeeklyAnalysisReportNotFoundException;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisReportQueryHelper;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.GetWeeklyAnalysisReportListUseCase;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.GetWeeklyAnalysisReportUseCase;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReportContent;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class WeeklyAnalysisReportQueryService implements GetWeeklyAnalysisReportUseCase, GetWeeklyAnalysisReportListUseCase {

    private final WeeklyAnalysisReportQueryHelper weeklyAnalysisReportQueryHelper;

    @Override
    @CheckValidMember
    public GetWeeklyAnalysisReportResponse getWeeklyAnalysisReport(GetWeeklyAnalysisReportCommand command) {
        WeeklyAnalysisReport report = weeklyAnalysisReportQueryHelper
                .getWeeklyReport(MemberId.of(command.getUserId()), command.getWeekStartDate())
                .filter(savedReport -> savedReport.getStatus() == WeeklyAnalysisReportStatus.PUBLISHED)
                .orElseThrow(WeeklyAnalysisReportNotFoundException::new);
        WeeklyAnalysisReportContent content = report.getContent();
        if (content == null) {
            throw new IllegalStateException("Published weekly analysis report content is missing");
        }

        return new GetWeeklyAnalysisReportResponse(
                report.getWeekStartDate(),
                report.getWeekEndDate(),
                report.getStatus().name(),
                report.getGeneratedAt(),
                content.schemaVersion(),
                content.period(),
                content.overview(),
                content.topTopics(),
                content.moodByTime(),
                content.conflict(),
                content.behaviorPattern(),
                content.solution()
        );
    }

    @Override
    @CheckValidMember
    public GetWeeklyAnalysisReportListResponse getWeeklyAnalysisReportList(GetWeeklyAnalysisReportListCommand command) {
        List<GetWeeklyAnalysisReportListItemResponse> reports = weeklyAnalysisReportQueryHelper
                .getPublishedWeeklyReports(MemberId.of(command.getUserId()))
                .stream()
                .map(report -> new GetWeeklyAnalysisReportListItemResponse(
                        report.getWeekStartDate(),
                        report.getWeekEndDate(),
                        report.getStatus().name(),
                        report.getGeneratedAt()
                ))
                .toList();

        return new GetWeeklyAnalysisReportListResponse(reports, reports.size());
    }
}
