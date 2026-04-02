package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import makeus.cmc.malmo.adaptor.out.persistence.entity.weekly_analysis_report.WeeklyAnalysisReportEntity;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Component;

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

        return WeeklyAnalysisReportEntity.of(
                domain.getId(),
                domain.getMemberId() == null ? null : MemberEntityId.of(domain.getMemberId().getValue()),
                domain.getWeekStartDate(),
                domain.getWeekEndDate(),
                domain.getStatus(),
                domain.getSourceChatRoomCount(),
                domain.getEligibleChatRoomCount(),
                domain.getSourceUserMessageCount(),
                domain.getContent(),
                domain.getGeneratedAt(),
                domain.getFailedReason()
        );
    }
}
