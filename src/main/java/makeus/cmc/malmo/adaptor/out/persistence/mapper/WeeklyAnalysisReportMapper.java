package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import makeus.cmc.malmo.adaptor.out.persistence.entity.weekly_analysis_report.WeeklyAnalysisReportEntity;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReportContent;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class WeeklyAnalysisReportMapper {

    public WeeklyAnalysisReport toDomain(WeeklyAnalysisReportEntity entity) {
        if (entity == null) {
            return null;
        }

        return WeeklyAnalysisReport.from(
                entity.getId(),
                entity.getMemberId() == null ? null : MemberId.of(entity.getMemberId().getValue()),
                entity.getWeekStartDate(),
                entity.getWeekEndDate(),
                entity.getStatus(),
                entity.getSourceChatRoomCount(),
                entity.getEligibleChatRoomCount(),
                entity.getSourceUserMessageCount(),
                entity.toContent(),
                entity.getGeneratedAt(),
                entity.getFailedReason(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                entity.getDeletedAt()
        );
    }

    public WeeklyAnalysisReportEntity toEntity(WeeklyAnalysisReport domain) {
        if (domain == null) {
            return null;
        }

        WeeklyAnalysisReportContent content = domain.getContent();

        return WeeklyAnalysisReportEntity.builder()
                .id(domain.getId())
                .memberId(domain.getMemberId() == null ? null : MemberEntityId.of(domain.getMemberId().getValue()))
                .weekStartDate(domain.getWeekStartDate())
                .weekEndDate(domain.getWeekEndDate())
                .status(domain.getStatus())
                .sourceChatRoomCount(domain.getSourceChatRoomCount())
                .eligibleChatRoomCount(domain.getEligibleChatRoomCount())
                .sourceUserMessageCount(domain.getSourceUserMessageCount())
                .schemaVersion(content == null ? null : content.schemaVersion())
                .timezone(content == null ? null : content.period().timezone())
                .overview(content == null ? null : WeeklyAnalysisReportEntity.OverviewEmbeddable.from(content.overview()))
                .topTopics(content == null
                        ? new ArrayList<>()
                        : content.topTopics().stream()
                                .map(WeeklyAnalysisReportEntity.TopTopicEmbeddable::from)
                                .collect(java.util.stream.Collectors.toCollection(ArrayList::new)))
                .moodByTime(content == null ? null : WeeklyAnalysisReportEntity.MoodByTimeEmbeddable.from(content.moodByTime()))
                .conflict(content == null ? null : WeeklyAnalysisReportEntity.ConflictEmbeddable.from(content.conflict()))
                .behaviorPattern(content == null ? null : WeeklyAnalysisReportEntity.BehaviorPatternEmbeddable.from(content.behaviorPattern()))
                .solution(content == null ? null : WeeklyAnalysisReportEntity.SolutionEmbeddable.from(content.solution()))
                .generatedAt(domain.getGeneratedAt())
                .failedReason(domain.getFailedReason())
                .createdAt(domain.getCreatedAt())
                .modifiedAt(domain.getModifiedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
