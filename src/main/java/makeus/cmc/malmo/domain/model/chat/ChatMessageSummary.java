package makeus.cmc.malmo.domain.model.chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import makeus.cmc.malmo.adaptor.out.persistence.entity.BaseTimeEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.ChatRoomEntityId;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class ChatMessageSummary {
    private Long id;
    private ChatRoomId chatRoomId;
    private String content;
    private int level;
    private boolean isForCurrentLevel;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    public static ChatMessageSummary from(Long id, ChatRoomId chatRoomId, String content, int level,
                                                      boolean isForCurrentLevel, LocalDateTime createdAt,
                                                      LocalDateTime modifiedAt, LocalDateTime deletedAt) {
        return ChatMessageSummary.builder()
                .id(id)
                .chatRoomId(chatRoomId)
                .content(content)
                .level(level)
                .isForCurrentLevel(isForCurrentLevel)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public static ChatMessageSummary createChatMessageSummary(ChatRoomId chatRoomId, String content, int level, boolean isForCurrentLevel) {
        return ChatMessageSummary.builder()
                .chatRoomId(chatRoomId)
                .content(content)
                .level(level)
                .isForCurrentLevel(isForCurrentLevel)
                .build();
    }
}
