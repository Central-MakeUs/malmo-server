package makeus.cmc.malmo.application.port.out.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.SenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoadMessagesPort {
    Optional<ChatMessage> loadMessageById(Long messageId);
    Page<ChatRoomMessageRepositoryDto> loadMessagesDto(ChatRoomId chatRoomId, MemberId memberId, Pageable pageable);
    Page<ChatRoomMessageRepositoryDto> loadMessagesDtoAsc(ChatRoomId chatRoomId, MemberId memberId, Pageable pageable);
    List<ChatMessage> loadChatRoomMessagesByLevel(ChatRoomId chatRoomId, int level);

    List<ChatMessage> loadChatRoomLevelAndDetailedLevelMessages(ChatRoomId chatRoomId, int level, int detailedLevel);

    List<ChatMessage> loadRecentMessagesByLevel(ChatRoomId chatRoomId, int level, int limit);

    long countMessagesByLevel(ChatRoomId chatRoomId, int level);

    boolean hasUserMessages(ChatRoomId chatRoomId);

    @Data
    @AllArgsConstructor
    class ChatRoomMessageRepositoryDto {
        private Long messageId;
        private SenderType senderType;
        private String content;
        private LocalDateTime createdAt;
        private Long bookmarkId;
    }
}
