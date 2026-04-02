package makeus.cmc.malmo.application.service.weekly_analysis_report;

import makeus.cmc.malmo.application.exception.WeeklyAnalysisReportNotFoundException;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisReportQueryHelper;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.GetWeeklyAnalysisReportUseCase;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReportContent;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyAnalysisReportQueryServiceTest {

    @Mock private WeeklyAnalysisReportQueryHelper weeklyAnalysisReportQueryHelper;

    private WeeklyAnalysisReportQueryService weeklyAnalysisReportQueryService;

    @BeforeEach
    void setUp() {
        weeklyAnalysisReportQueryService = new WeeklyAnalysisReportQueryService(weeklyAnalysisReportQueryHelper);
    }

    @Test
    void getWeeklyAnalysisReport_throwsWhenNotPublished() {
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(MemberId.of(1L), LocalDate.of(2026, 3, 23)))
                .thenReturn(Optional.of(
                        WeeklyAnalysisReport.from(
                                1L,
                                MemberId.of(1L),
                                LocalDate.of(2026, 3, 23),
                                LocalDate.of(2026, 3, 29),
                                WeeklyAnalysisReportStatus.GENERATING,
                                1,
                                1,
                                2,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                ));

        assertThatThrownBy(() -> weeklyAnalysisReportQueryService.getWeeklyAnalysisReport(
                new GetWeeklyAnalysisReportUseCase.GetWeeklyAnalysisReportCommand(1L, LocalDate.of(2026, 3, 23))
        )).isInstanceOf(WeeklyAnalysisReportNotFoundException.class);
    }

    @Test
    void getWeeklyAnalysisReport_returnsStructuredFieldsWithoutPayloadWrapper() {
        LocalDate weekStartDate = LocalDate.of(2026, 3, 23);
        LocalDate weekEndDate = LocalDate.of(2026, 3, 29);
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(MemberId.of(1L), weekStartDate))
                .thenReturn(Optional.of(
                        WeeklyAnalysisReport.from(
                                1L,
                                MemberId.of(1L),
                                weekStartDate,
                                weekEndDate,
                                WeeklyAnalysisReportStatus.PUBLISHED,
                                1,
                                1,
                                2,
                                sampleContent(weekStartDate, weekEndDate),
                                LocalDateTime.of(2026, 3, 30, 0, 2, 14),
                                null,
                                null,
                                null,
                                null
                        )
                ));

        GetWeeklyAnalysisReportUseCase.GetWeeklyAnalysisReportResponse response = weeklyAnalysisReportQueryService.getWeeklyAnalysisReport(
                new GetWeeklyAnalysisReportUseCase.GetWeeklyAnalysisReportCommand(1L, weekStartDate)
        );

        assertThat(response.getSchemaVersion()).isEqualTo("v1");
        assertThat(response.getPeriod().weekStartDate()).isEqualTo(weekStartDate);
        assertThat(response.getOverview().title()).isEqualTo("title");
        assertThat(response.getTopTopics()).hasSize(1);
        assertThat(response.getTopTopics().get(0).keyword()).isEqualTo("연락");
        assertThat(response.getConflict().score()).isEqualTo(50);
        assertThat(response.getBehaviorPattern().oneLineSummary()).isEqualTo("sum");
        assertThat(response.getSolution().content()).isEqualTo("content");
    }

    @Test
    void getWeeklyAnalysisReportList_returnsPublishedReportsOnly() {
        when(weeklyAnalysisReportQueryHelper.getPublishedWeeklyReports(MemberId.of(1L)))
                .thenReturn(java.util.List.of(
                        WeeklyAnalysisReport.from(
                                2L,
                                MemberId.of(1L),
                                LocalDate.of(2026, 3, 30),
                                LocalDate.of(2026, 4, 5),
                                WeeklyAnalysisReportStatus.PUBLISHED,
                                1,
                                1,
                                2,
                                sampleContent(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 5)),
                                LocalDateTime.of(2026, 4, 6, 0, 2, 14),
                                null,
                                null,
                                null,
                                null
                        ),
                        WeeklyAnalysisReport.from(
                                1L,
                                MemberId.of(1L),
                                LocalDate.of(2026, 3, 23),
                                LocalDate.of(2026, 3, 29),
                                WeeklyAnalysisReportStatus.PUBLISHED,
                                1,
                                1,
                                3,
                                sampleContent(LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 29)),
                                LocalDateTime.of(2026, 3, 30, 0, 2, 14),
                                null,
                                null,
                                null,
                                null
                        )
                ));

        var response = weeklyAnalysisReportQueryService.getWeeklyAnalysisReportList(
                new makeus.cmc.malmo.application.port.in.weekly_analysis_report.GetWeeklyAnalysisReportListUseCase.GetWeeklyAnalysisReportListCommand(1L)
        );

        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getReports()).hasSize(2);
        assertThat(response.getReports().get(0).getWeekStartDate()).isEqualTo(LocalDate.of(2026, 3, 30));
    }

    private WeeklyAnalysisReportContent sampleContent(LocalDate weekStartDate, LocalDate weekEndDate) {
        return new WeeklyAnalysisReportContent(
                "v1",
                new WeeklyAnalysisReportContent.Period(weekStartDate, weekEndDate, "Asia/Seoul"),
                new WeeklyAnalysisReportContent.Overview("title", "summary"),
                java.util.List.of(new WeeklyAnalysisReportContent.TopTopic("연락", 1, 0.5, "desc")),
                new WeeklyAnalysisReportContent.MoodByTime(
                        "EVENING",
                        new WeeklyAnalysisReportContent.MoodRatios(0.1, 0.2, 0.6, 0.1),
                        "desc"
                ),
                new WeeklyAnalysisReportContent.Conflict(50, "desc"),
                new WeeklyAnalysisReportContent.BehaviorPattern("sum", "trigger", "belief", "response"),
                new WeeklyAnalysisReportContent.Solution("title", "content")
        );
    }
}
