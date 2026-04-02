package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.PromptEntity;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import org.springframework.stereotype.Component;

@Component
public class PromptMapper {

    public Prompt toDomain(PromptEntity entity) {
        if (entity == null) {
            return null;
        }

        return Prompt.from(
                entity.getId(),
                entity.getLevel(),
                entity.getContent(),
                entity.isForSystem(),
                entity.isForSummary(),
                entity.isForCompletedResponse(),
                entity.isForTotalSummary(),
                entity.isForGuideline(),
                entity.isForAnswerMetadata(),
                entity.isForTitleGeneration(),
                entity.isForWeeklyReport(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                entity.getDeletedAt()
        );
    }

    public PromptEntity toEntity(Prompt domain) {
        if (domain == null) {
            return null;
        }

        return PromptEntity.builder()
                .id(domain.getId())
                .level(domain.getLevel())
                .content(domain.getContent())
                .isForSystem(domain.isForSystem())
                .isForSummary(domain.isForSummary())
                .isForCompletedResponse(domain.isForCompletedResponse())
                .isForTotalSummary(domain.isForTotalSummary())
                .isForGuideline(domain.isForGuideline())
                .isForAnswerMetadata(domain.isForAnswerMetadata())
                .isForTitleGeneration(domain.isForTitleGeneration())
                .isForWeeklyReport(domain.isForWeeklyReport())
                .createdAt(domain.getCreatedAt())
                .modifiedAt(domain.getModifiedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
