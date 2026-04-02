package makeus.cmc.malmo.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.mapper.WeeklyAnalysisReportMapper;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReportContent;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyAnalysisReportMapper 테스트")
class WeeklyAnalysisReportMapperTest {

    @InjectMocks
    private WeeklyAnalysisReportMapper weeklyAnalysisReportMapper;

    @Test
    @DisplayName("PUBLISHED 리포트를 Entity로 변환할 때 audit 필드와 content를 유지한다")
    void toEntity_preservesAuditFieldsForPublishedReport() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 2, 10, 0);
        LocalDateTime modifiedAt = LocalDateTime.of(2026, 4, 2, 11, 0);
        LocalDateTime generatedAt = LocalDateTime.of(2026, 4, 2, 12, 0);
        LocalDate weekStartDate = LocalDate.of(2026, 3, 23);
        LocalDate weekEndDate = LocalDate.of(2026, 3, 29);

        WeeklyAnalysisReport domain = WeeklyAnalysisReport.from(
                1L,
                MemberId.of(2L),
                weekStartDate,
                weekEndDate,
                WeeklyAnalysisReportStatus.PUBLISHED,
                3,
                2,
                8,
                sampleContent(weekStartDate, weekEndDate),
                generatedAt,
                null,
                createdAt,
                modifiedAt,
                null
        );

        var entity = weeklyAnalysisReportMapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getMemberId().getValue()).isEqualTo(domain.getMemberId().getValue());
        assertThat(entity.getStatus()).isEqualTo(domain.getStatus());
        assertThat(entity.getSchemaVersion()).isEqualTo("v1");
        assertThat(entity.getTimezone()).isEqualTo("Asia/Seoul");
        assertThat(entity.getOverview()).isNotNull();
        assertThat(entity.getTopTopics()).hasSize(2);
        assertThat(entity.getMoodByTime()).isNotNull();
        assertThat(entity.getConflict()).isNotNull();
        assertThat(entity.getBehaviorPattern()).isNotNull();
        assertThat(entity.getSolution()).isNotNull();
        assertThat(entity.getGeneratedAt()).isEqualTo(generatedAt);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getModifiedAt()).isEqualTo(modifiedAt);
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("FAILED 리포트를 Entity로 변환할 때 audit 필드와 null content를 유지한다")
    void toEntity_preservesAuditFieldsForFailedReport() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 1, 9, 0);
        LocalDateTime modifiedAt = LocalDateTime.of(2026, 4, 1, 10, 0);
        LocalDateTime deletedAt = LocalDateTime.of(2026, 4, 1, 11, 0);

        WeeklyAnalysisReport domain = WeeklyAnalysisReport.from(
                10L,
                MemberId.of(20L),
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 29),
                WeeklyAnalysisReportStatus.FAILED,
                1,
                1,
                4,
                null,
                null,
                "llm timeout",
                createdAt,
                modifiedAt,
                deletedAt
        );

        var entity = weeklyAnalysisReportMapper.toEntity(domain);

        assertThat(entity.getStatus()).isEqualTo(WeeklyAnalysisReportStatus.FAILED);
        assertThat(entity.getFailedReason()).isEqualTo("llm timeout");
        assertThat(entity.getSchemaVersion()).isNull();
        assertThat(entity.getTimezone()).isNull();
        assertThat(entity.getOverview()).isNull();
        assertThat(entity.getTopTopics()).isEmpty();
        assertThat(entity.getMoodByTime()).isNull();
        assertThat(entity.getConflict()).isNull();
        assertThat(entity.getBehaviorPattern()).isNull();
        assertThat(entity.getSolution()).isNull();
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getModifiedAt()).isEqualTo(modifiedAt);
        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }

    private WeeklyAnalysisReportContent sampleContent(LocalDate weekStartDate, LocalDate weekEndDate) {
        return new WeeklyAnalysisReportContent(
                "v1",
                new WeeklyAnalysisReportContent.Period(weekStartDate, weekEndDate, "Asia/Seoul"),
                new WeeklyAnalysisReportContent.Overview("title", "summary"),
                List.of(
                        new WeeklyAnalysisReportContent.TopTopic("연락", 1, 0.5, "desc"),
                        new WeeklyAnalysisReportContent.TopTopic("불안", 2, 0.3, "desc2")
                ),
                new WeeklyAnalysisReportContent.MoodByTime(
                        "EVENING",
                        new WeeklyAnalysisReportContent.MoodRatios(0.1, 0.2, 0.6, 0.1),
                        "desc"
                ),
                new WeeklyAnalysisReportContent.Conflict(50, "conflict"),
                new WeeklyAnalysisReportContent.BehaviorPattern("sum", "trigger", "belief", "response"),
                new WeeklyAnalysisReportContent.Solution("title", "content")
        );
    }
}
