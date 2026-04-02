package makeus.cmc.malmo.application.service.weekly_analysis_report;

import com.fasterxml.jackson.databind.ObjectMapper;
import makeus.cmc.malmo.application.helper.chat_room.PromptQueryHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.helper.notification.MemberNotificationCommandHelper;
import makeus.cmc.malmo.application.helper.outbox.OutboxHelper;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisReportCommandHelper;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisReportQueryHelper;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisReportReservationHelper;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisSourceQueryHelper;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisWeekCalculator;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.TriggerWeeklyAnalysisReportUseCase;
import makeus.cmc.malmo.application.port.out.chat.RequestChatApiPort;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.MemberChatRoomMetadata;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyAnalysisReportServiceTest {

    @Mock private WeeklyAnalysisWeekCalculator weeklyAnalysisWeekCalculator;
    @Mock private WeeklyAnalysisSourceQueryHelper weeklyAnalysisSourceQueryHelper;
    @Mock private WeeklyAnalysisReportQueryHelper weeklyAnalysisReportQueryHelper;
    @Mock private WeeklyAnalysisReportCommandHelper weeklyAnalysisReportCommandHelper;
    @Mock private WeeklyAnalysisReportReservationHelper weeklyAnalysisReportReservationHelper;
    @Mock private OutboxHelper outboxHelper;
    @Mock private PromptQueryHelper promptQueryHelper;
    @Mock private MemberQueryHelper memberQueryHelper;
    @Mock private MemberNotificationCommandHelper memberNotificationCommandHelper;
    @Mock private RequestChatApiPort requestChatApiPort;

    private WeeklyAnalysisReportService weeklyAnalysisReportService;

    @BeforeEach
    void setUp() {
        weeklyAnalysisReportService = new WeeklyAnalysisReportService(
                weeklyAnalysisWeekCalculator,
                weeklyAnalysisSourceQueryHelper,
                weeklyAnalysisReportQueryHelper,
                weeklyAnalysisReportCommandHelper,
                weeklyAnalysisReportReservationHelper,
                outboxHelper,
                promptQueryHelper,
                memberQueryHelper,
                memberNotificationCommandHelper,
                requestChatApiPort,
                new ObjectMapper()
        );
    }

    @Test
    void scheduleWeeklyAnalysisReports_createsPendingReportAndPublishesOutbox() {
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = new WeeklyAnalysisWeekCalculator.WeekPeriod(
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 29),
                LocalDateTime.of(2026, 3, 23, 0, 0),
                LocalDateTime.of(2026, 3, 30, 0, 0),
                WeeklyAnalysisWeekCalculator.WEEKLY_REPORT_ZONE_ID
        );
        when(weeklyAnalysisWeekCalculator.getPreviousClosedWeek()).thenReturn(weekPeriod);
        when(weeklyAnalysisSourceQueryHelper.getCandidateMemberIds(weekPeriod)).thenReturn(List.of(1L));
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(MemberId.of(1L), weekPeriod.getWeekStartDate())).thenReturn(Optional.empty());
        when(weeklyAnalysisSourceQueryHelper.countWeeklyUserMessages(MemberId.of(1L), weekPeriod)).thenReturn(3L);
        when(weeklyAnalysisSourceQueryHelper.getEligibleChatRoomIds(MemberId.of(1L), weekPeriod)).thenReturn(List.of(10L, 11L));
        when(weeklyAnalysisReportReservationHelper.reservePendingReport(any())).thenReturn(
                WeeklyAnalysisReport.from(
                        99L,
                        MemberId.of(1L),
                        weekPeriod.getWeekStartDate(),
                        weekPeriod.getWeekEndDate(),
                        WeeklyAnalysisReportStatus.PENDING,
                        2,
                        2,
                        3,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        weeklyAnalysisReportService.scheduleWeeklyAnalysisReports();

        ArgumentCaptor<WeeklyAnalysisReport> reportCaptor = ArgumentCaptor.forClass(WeeklyAnalysisReport.class);
        verify(weeklyAnalysisReportReservationHelper).reservePendingReport(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getWeekStartDate()).isEqualTo(LocalDate.of(2026, 3, 23));
        assertThat(reportCaptor.getValue().getEligibleChatRoomCount()).isEqualTo(2);
        assertThat(reportCaptor.getValue().getSourceUserMessageCount()).isEqualTo(3);
    }

    @Test
    void scheduleWeeklyAnalysisReports_skipsWhenReportAlreadyExists() {
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = new WeeklyAnalysisWeekCalculator.WeekPeriod(
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 29),
                LocalDateTime.of(2026, 3, 23, 0, 0),
                LocalDateTime.of(2026, 3, 30, 0, 0),
                WeeklyAnalysisWeekCalculator.WEEKLY_REPORT_ZONE_ID
        );
        when(weeklyAnalysisWeekCalculator.getPreviousClosedWeek()).thenReturn(weekPeriod);
        when(weeklyAnalysisSourceQueryHelper.getCandidateMemberIds(weekPeriod)).thenReturn(List.of(1L));
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(MemberId.of(1L), weekPeriod.getWeekStartDate())).thenReturn(
                Optional.of(
                        WeeklyAnalysisReport.from(
                                7L,
                                MemberId.of(1L),
                                weekPeriod.getWeekStartDate(),
                                weekPeriod.getWeekEndDate(),
                                WeeklyAnalysisReportStatus.PENDING,
                                1,
                                1,
                                1,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                )
        );

        weeklyAnalysisReportService.scheduleWeeklyAnalysisReports();

        verify(weeklyAnalysisReportReservationHelper, never()).reservePendingReport(any());
    }

    @Test
    void scheduleWeeklyAnalysisReports_republishesFailedReport() {
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = new WeeklyAnalysisWeekCalculator.WeekPeriod(
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 29),
                LocalDateTime.of(2026, 3, 23, 0, 0),
                LocalDateTime.of(2026, 3, 30, 0, 0),
                WeeklyAnalysisWeekCalculator.WEEKLY_REPORT_ZONE_ID
        );
        when(weeklyAnalysisWeekCalculator.getPreviousClosedWeek()).thenReturn(weekPeriod);
        when(weeklyAnalysisSourceQueryHelper.getCandidateMemberIds(weekPeriod)).thenReturn(List.of(1L));
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(MemberId.of(1L), weekPeriod.getWeekStartDate())).thenReturn(
                Optional.of(
                        WeeklyAnalysisReport.from(
                                15L,
                                MemberId.of(1L),
                                weekPeriod.getWeekStartDate(),
                                weekPeriod.getWeekEndDate(),
                                WeeklyAnalysisReportStatus.FAILED,
                                1,
                                1,
                                1,
                                null,
                                null,
                                "failed",
                                null,
                                null,
                                null
                        )
                )
        );

        weeklyAnalysisReportService.scheduleWeeklyAnalysisReports();

        verify(weeklyAnalysisReportReservationHelper, never()).reservePendingReport(any());
        verify(outboxHelper).publish(any(), any());
    }

    @Test
    void generateWeeklyAnalysisReport_skipsWhenStateTransitionRejected() {
        when(weeklyAnalysisReportCommandHelper.markAsGeneratingIfUpdatable(55L)).thenReturn(false);

        weeklyAnalysisReportService.generateWeeklyAnalysisReport(
                new makeus.cmc.malmo.application.port.in.weekly_analysis_report.GenerateWeeklyAnalysisReportUseCase.GenerateWeeklyAnalysisReportCommand(55L)
        ).join();

        verify(weeklyAnalysisReportQueryHelper, never()).getWeeklyReport(55L);
        verify(requestChatApiPort, never()).requestJsonResponse(any(), any());
    }

    @Test
    void generateWeeklyAnalysisReport_publishesStructuredContent() {
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = new WeeklyAnalysisWeekCalculator.WeekPeriod(
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 29),
                LocalDateTime.of(2026, 3, 23, 0, 0),
                LocalDateTime.of(2026, 3, 30, 0, 0),
                WeeklyAnalysisWeekCalculator.WEEKLY_REPORT_ZONE_ID
        );
        WeeklyAnalysisReport generatingReport = WeeklyAnalysisReport.from(
                55L,
                MemberId.of(1L),
                weekPeriod.getWeekStartDate(),
                weekPeriod.getWeekEndDate(),
                WeeklyAnalysisReportStatus.GENERATING,
                1,
                1,
                3,
                null,
                null,
                null,
                null,
                null,
                null
        );
        List<MemberChatRoomMetadata> metadata = List.of(
                MemberChatRoomMetadata.from(
                        1L,
                        ChatRoomId.of(100L),
                        MemberId.of(1L),
                        1,
                        3,
                        "title",
                        "summary",
                        LocalDateTime.of(2026, 3, 25, 12, 0)
                )
        );
        List<ChatMessage> weeklyUserMessages = List.of(
                ChatMessage.from(
                        1L,
                        ChatRoomId.of(100L),
                        1,
                        3,
                        "message-1",
                        makeus.cmc.malmo.domain.value.type.SenderType.USER,
                        LocalDateTime.of(2026, 3, 25, 20, 0),
                        null,
                        null
                ),
                ChatMessage.from(
                        2L,
                        ChatRoomId.of(100L),
                        1,
                        3,
                        "message-2",
                        makeus.cmc.malmo.domain.value.type.SenderType.USER,
                        LocalDateTime.of(2026, 3, 26, 1, 0),
                        null,
                        null
                )
        );
        when(weeklyAnalysisReportCommandHelper.markAsGeneratingIfUpdatable(55L)).thenReturn(true);
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(55L)).thenReturn(Optional.of(generatingReport));
        when(weeklyAnalysisWeekCalculator.fromWeekStartDate(weekPeriod.getWeekStartDate())).thenReturn(weekPeriod);
        when(weeklyAnalysisSourceQueryHelper.getEligibleChatRoomIds(MemberId.of(1L), weekPeriod)).thenReturn(List.of(100L));
        when(weeklyAnalysisSourceQueryHelper.getAllMetadataByChatRoomIds(List.of(100L))).thenReturn(metadata);
        when(weeklyAnalysisSourceQueryHelper.getWeeklyUserMessages(MemberId.of(1L), weekPeriod)).thenReturn(weeklyUserMessages);
        when(promptQueryHelper.getWeeklyReportPrompt()).thenReturn(
                Prompt.from(1L, 0, "weekly prompt", false, false, false, false, false, false, false, true, null, null, null)
        );
        when(memberQueryHelper.getMemberInfoOrThrow(MemberId.of(1L))).thenReturn(
                MemberQueryHelper.MemberInfoDto.builder()
                        .nickname("weekly")
                        .relationshipStatus(makeus.cmc.malmo.domain.value.type.RelationshipStatus.IN_RELATIONSHIP)
                        .loveTypeCategory(makeus.cmc.malmo.domain.value.type.LoveTypeCategory.STABLE_TYPE)
                        .personalityType("INTJ")
                        .build()
        );
        when(requestChatApiPort.requestJsonResponse(any(), eq(makeus.cmc.malmo.application.port.out.chat.LlmReasoningScenario.SUMMARY)))
                .thenReturn(CompletableFuture.completedFuture("""
                        {
                          "overview": {"title": "title", "summary": "summary"},
                          "topTopics": [
                            {"keyword": "연락", "rank": 1, "weight": 0.55, "description": "desc"},
                            {"keyword": "불안", "rank": 2, "weight": 0.25, "description": "desc2"},
                            {"keyword": "확인", "rank": 3, "weight": 0.20, "description": "desc3"},
                            {"keyword": "초과", "rank": 4, "weight": 0.10, "description": "trimmed"}
                          ],
                          "conflict": {"score": 130, "description": "conflict"},
                          "behaviorPattern": {
                            "oneLineSummary": "pattern",
                            "triggerSituation": "trigger",
                            "belief": "belief",
                            "responseType": "response"
                          },
                          "solution": {"title": "solution", "content": "content"}
                        }
                        """));

        weeklyAnalysisReportService.generateWeeklyAnalysisReport(
                new makeus.cmc.malmo.application.port.in.weekly_analysis_report.GenerateWeeklyAnalysisReportUseCase.GenerateWeeklyAnalysisReportCommand(55L)
        ).join();

        ArgumentCaptor<WeeklyAnalysisReport> reportCaptor = ArgumentCaptor.forClass(WeeklyAnalysisReport.class);
        verify(weeklyAnalysisReportCommandHelper).saveWeeklyAnalysisReport(reportCaptor.capture());
        WeeklyAnalysisReport savedReport = reportCaptor.getValue();
        assertThat(savedReport.getStatus()).isEqualTo(WeeklyAnalysisReportStatus.PUBLISHED);
        assertThat(savedReport.getContent().schemaVersion()).isEqualTo("v1");
        assertThat(savedReport.getContent().topTopics()).hasSize(3);
        assertThat(savedReport.getContent().topTopics().get(0).keyword()).isEqualTo("연락");
        assertThat(savedReport.getContent().conflict().score()).isEqualTo(100);
        assertThat(savedReport.getContent().moodByTime().dominantPeriod()).isEqualTo("LATE_NIGHT");
        assertThat(savedReport.getContent().moodByTime().ratios().evening()).isEqualTo(0.5);
        assertThat(savedReport.getContent().moodByTime().ratios().lateNight()).isEqualTo(0.5);
    }

    @Test
    void scheduleWeeklyAnalysisReports_continuesWhenDuplicateReservationIsIgnored() {
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = new WeeklyAnalysisWeekCalculator.WeekPeriod(
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 29),
                LocalDateTime.of(2026, 3, 23, 0, 0),
                LocalDateTime.of(2026, 3, 30, 0, 0),
                WeeklyAnalysisWeekCalculator.WEEKLY_REPORT_ZONE_ID
        );
        when(weeklyAnalysisWeekCalculator.getPreviousClosedWeek()).thenReturn(weekPeriod);
        when(weeklyAnalysisSourceQueryHelper.getCandidateMemberIds(weekPeriod)).thenReturn(List.of(1L, 2L));
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(MemberId.of(1L), weekPeriod.getWeekStartDate())).thenReturn(Optional.empty());
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(MemberId.of(2L), weekPeriod.getWeekStartDate())).thenReturn(Optional.empty());
        when(weeklyAnalysisSourceQueryHelper.countWeeklyUserMessages(MemberId.of(1L), weekPeriod)).thenReturn(2L);
        when(weeklyAnalysisSourceQueryHelper.countWeeklyUserMessages(MemberId.of(2L), weekPeriod)).thenReturn(2L);
        when(weeklyAnalysisSourceQueryHelper.getEligibleChatRoomIds(MemberId.of(1L), weekPeriod)).thenReturn(List.of(10L));
        when(weeklyAnalysisSourceQueryHelper.getEligibleChatRoomIds(MemberId.of(2L), weekPeriod)).thenReturn(List.of(20L));
        when(weeklyAnalysisReportReservationHelper.reservePendingReport(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenReturn(
                        WeeklyAnalysisReport.from(
                                101L,
                                MemberId.of(2L),
                                weekPeriod.getWeekStartDate(),
                                weekPeriod.getWeekEndDate(),
                                WeeklyAnalysisReportStatus.PENDING,
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
                );

        weeklyAnalysisReportService.scheduleWeeklyAnalysisReports();

        verify(weeklyAnalysisReportReservationHelper, times(2)).reservePendingReport(any());
    }

    @Test
    void triggerWeeklyAnalysisReports_returnsReservationSummaryForExplicitWeek() {
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = new WeeklyAnalysisWeekCalculator.WeekPeriod(
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 29),
                LocalDateTime.of(2026, 3, 23, 0, 0),
                LocalDateTime.of(2026, 3, 30, 0, 0),
                WeeklyAnalysisWeekCalculator.WEEKLY_REPORT_ZONE_ID
        );
        when(weeklyAnalysisWeekCalculator.fromWeekStartDate(LocalDate.of(2026, 3, 23))).thenReturn(weekPeriod);
        when(weeklyAnalysisSourceQueryHelper.getCandidateMemberIds(weekPeriod)).thenReturn(List.of(1L, 2L, 3L, 4L));
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(MemberId.of(1L), weekPeriod.getWeekStartDate())).thenReturn(Optional.empty());
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(MemberId.of(2L), weekPeriod.getWeekStartDate())).thenReturn(
                Optional.of(
                        WeeklyAnalysisReport.from(
                                20L,
                                MemberId.of(2L),
                                weekPeriod.getWeekStartDate(),
                                weekPeriod.getWeekEndDate(),
                                WeeklyAnalysisReportStatus.FAILED,
                                1,
                                1,
                                1,
                                null,
                                null,
                                "failed",
                                null,
                                null,
                                null
                        )
                )
        );
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(MemberId.of(3L), weekPeriod.getWeekStartDate())).thenReturn(
                Optional.of(
                        WeeklyAnalysisReport.from(
                                21L,
                                MemberId.of(3L),
                                weekPeriod.getWeekStartDate(),
                                weekPeriod.getWeekEndDate(),
                                WeeklyAnalysisReportStatus.PUBLISHED,
                                1,
                                1,
                                1,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                )
        );
        when(weeklyAnalysisReportQueryHelper.getWeeklyReport(MemberId.of(4L), weekPeriod.getWeekStartDate())).thenReturn(Optional.empty());
        when(weeklyAnalysisSourceQueryHelper.countWeeklyUserMessages(MemberId.of(1L), weekPeriod)).thenReturn(3L);
        when(weeklyAnalysisSourceQueryHelper.getEligibleChatRoomIds(MemberId.of(1L), weekPeriod)).thenReturn(List.of(10L, 11L));
        when(weeklyAnalysisSourceQueryHelper.countWeeklyUserMessages(MemberId.of(4L), weekPeriod)).thenReturn(0L);
        when(weeklyAnalysisSourceQueryHelper.getEligibleChatRoomIds(MemberId.of(4L), weekPeriod)).thenReturn(List.of());

        TriggerWeeklyAnalysisReportUseCase.TriggerWeeklyAnalysisReportResponse response =
                weeklyAnalysisReportService.triggerWeeklyAnalysisReports(
                        new TriggerWeeklyAnalysisReportUseCase.TriggerWeeklyAnalysisReportCommand(LocalDate.of(2026, 3, 23))
                );

        assertThat(response.getWeekStartDate()).isEqualTo(LocalDate.of(2026, 3, 23));
        assertThat(response.getWeekEndDate()).isEqualTo(LocalDate.of(2026, 3, 29));
        assertThat(response.getCandidateMemberCount()).isEqualTo(4);
        assertThat(response.getReservedCount()).isEqualTo(1);
        assertThat(response.getRepublishedFailedCount()).isEqualTo(1);
        assertThat(response.getSkippedExistingCount()).isEqualTo(1);
        assertThat(response.getSkippedIneligibleCount()).isEqualTo(1);
        verify(weeklyAnalysisReportReservationHelper).reservePendingReport(any());
        verify(outboxHelper).publish(any(), any());
    }

    @Test
    void triggerWeeklyAnalysisReports_throwsWhenWeekStartDateIsNotMonday() {
        assertThatThrownBy(() -> weeklyAnalysisReportService.triggerWeeklyAnalysisReports(
                new TriggerWeeklyAnalysisReportUseCase.TriggerWeeklyAnalysisReportCommand(LocalDate.of(2026, 3, 24))
        )).isInstanceOf(makeus.cmc.malmo.application.exception.InvalidWeeklyAnalysisWeekException.class);
    }
}
