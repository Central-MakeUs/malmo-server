package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatRoomEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatRoomMapper {

    public ChatRoom toDomain(ChatRoomEntity entity) {
        return ChatRoom.from(
                entity.getId(),
                entity.getMemberEntityId() != null ? MemberId.of(entity.getMemberEntityId().getValue()) : null,
                entity.getChatRoomState(),
                entity.isCurrentPromptForMetadata(),
                entity.getLevel(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                entity.getDeletedAt()
        );
    }

    public ChatRoomEntity toEntity(ChatRoom domain) {
        if (domain == null) {
            return null;
        }

        return ChatRoomEntity.builder()
                .id(domain.getId())
                .memberEntityId(domain.getMemberId() != null ? MemberEntityId.of(domain.getMemberId().getValue()) : null)
                .chatRoomState(domain.getChatRoomState())
                .isCurrentPromptForMetadata(domain.isCurrentPromptForMetadata())
                .level(domain.getLevel())
                .createdAt(domain.getCreatedAt())
                .modifiedAt(domain.getModifiedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
