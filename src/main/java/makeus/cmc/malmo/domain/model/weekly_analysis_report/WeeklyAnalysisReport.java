package makeus.cmc.malmo.domain.model.weekly_analysis_report;

import lombok.Getter;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class WeeklyAnalysisReport {

    private Long id;
    private MemberId memberId;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private WeeklyAnalysisReportStatus status;
    private int sourceChatRoomCount;
    private int eligibleChatRoomCount;
    private int sourceUserMessageCount;
    private WeeklyAnalysisReportContent content;
    private LocalDateTime generatedAt;
    private String failedReason;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    private WeeklyAnalysisReport(
            Long id,
            MemberId memberId,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            WeeklyAnalysisReportStatus status,
            int sourceChatRoomCount,
            int eligibleChatRoomCount,
            int sourceUserMessageCount,
            WeeklyAnalysisReportContent content,
            LocalDateTime generatedAt,
            String failedReason,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt,
            LocalDateTime deletedAt
    ) {
        this.id = id;
        this.memberId = memberId;
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.status = status;
        this.sourceChatRoomCount = sourceChatRoomCount;
        this.eligibleChatRoomCount = eligibleChatRoomCount;
        this.sourceUserMessageCount = sourceUserMessageCount;
        this.content = content;
        this.generatedAt = generatedAt;
        this.failedReason = failedReason;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.deletedAt = deletedAt;
    }

    public static WeeklyAnalysisReport createPendingReport(
            MemberId memberId,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            int sourceChatRoomCount,
            int eligibleChatRoomCount,
            int sourceUserMessageCount
    ) {
        return new WeeklyAnalysisReport(
                null,
                memberId,
                weekStartDate,
                weekEndDate,
                WeeklyAnalysisReportStatus.PENDING,
                sourceChatRoomCount,
                eligibleChatRoomCount,
                sourceUserMessageCount,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static WeeklyAnalysisReport from(
            Long id,
            MemberId memberId,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            WeeklyAnalysisReportStatus status,
            int sourceChatRoomCount,
            int eligibleChatRoomCount,
            int sourceUserMessageCount,
            WeeklyAnalysisReportContent content,
            LocalDateTime generatedAt,
            String failedReason,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt,
            LocalDateTime deletedAt
    ) {
        return new WeeklyAnalysisReport(
                id,
                memberId,
                weekStartDate,
                weekEndDate,
                status,
                sourceChatRoomCount,
                eligibleChatRoomCount,
                sourceUserMessageCount,
                content,
                generatedAt,
                failedReason,
                createdAt,
                modifiedAt,
                deletedAt
        );
    }

    public void markGenerating() {
        this.status = WeeklyAnalysisReportStatus.GENERATING;
        this.content = null;
        this.failedReason = null;
    }

    public void markPublished(WeeklyAnalysisReportContent content, LocalDateTime generatedAt) {
        this.status = WeeklyAnalysisReportStatus.PUBLISHED;
        this.content = content;
        this.generatedAt = generatedAt;
        this.failedReason = null;
    }

    public void markFailed(String failedReason) {
        this.status = WeeklyAnalysisReportStatus.FAILED;
        this.content = null;
        this.failedReason = failedReason;
    }

}
