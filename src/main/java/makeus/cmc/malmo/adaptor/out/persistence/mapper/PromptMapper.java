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
                entity.isForMetadata(),
                entity.isLastPromptForMetadata(),
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
                .isForMetadata(domain.isForMetadata())
                .isLastPromptForMetadata(domain.isLastPromptForMetadata())
                .createdAt(domain.getCreatedAt())
                .modifiedAt(domain.getModifiedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
