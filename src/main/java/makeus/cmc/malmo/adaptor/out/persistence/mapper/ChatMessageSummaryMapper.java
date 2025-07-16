package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatMessageSummaryEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.ChatRoomEntityId;
import makeus.cmc.malmo.domain.model.chat.ChatMessageSummary;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageSummaryMapper {

    public ChatMessageSummary toDomain(ChatMessageSummaryEntity entity) {
        if (entity == null) {
            return null;
        }
        return ChatMessageSummary.from(
                entity.getId(),
                entity.getChatRoomEntityId() != null ?
                        ChatRoomId.of(entity.getChatRoomEntityId().getValue()) : null,
                entity.getContent(),
                entity.getLevel(),
                entity.isForCurrentLevel(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                entity.getDeletedAt()
        );
    }

    public ChatMessageSummaryEntity toEntity(ChatMessageSummary domain) {
        if (domain == null) {
            return null;
        }
        return ChatMessageSummaryEntity.builder()
                .id(domain.getId())
                .chatRoomEntityId(domain.getChatRoomId() != null ?
                        ChatRoomEntityId.of(domain.getChatRoomId().getValue()) : null)
                .content(domain.getContent())
                .level(domain.getLevel())
                .isForCurrentLevel(domain.isForCurrentLevel())
                .createdAt(domain.getCreatedAt())
                .modifiedAt(domain.getModifiedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
