package makeus.cmc.malmo.application.port.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.type.SenderType;

import java.time.LocalDateTime;
import java.util.List;

public interface LoadCurrentMessagesPort {
    List<ChatMessage> loadMessages(ChatRoomId chatRoomId);
    List<ChatRoomMessageRepositoryDto> loadMessagesDto(ChatRoomId chatRoomId, int page, int size);

    @Data
    @AllArgsConstructor
    class ChatRoomMessageRepositoryDto {
        private Long messageId;
        private SenderType senderType;
        private String content;
        private LocalDateTime createdAt;
        private boolean isSaved;
    }
}
