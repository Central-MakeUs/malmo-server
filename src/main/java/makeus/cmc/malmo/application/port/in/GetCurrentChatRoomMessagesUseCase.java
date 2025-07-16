package makeus.cmc.malmo.application.port.in;

import lombok.Builder;
import lombok.Data;
import makeus.cmc.malmo.domain.value.state.ChatRoomState;
import makeus.cmc.malmo.domain.value.type.SenderType;

import java.time.LocalDateTime;
import java.util.List;

public interface GetCurrentChatRoomMessagesUseCase {

    GetCurrentChatRoomMessagesResponse getCurrentChatRoomMessages(GetCurrentChatRoomMessagesCommand command);

    @Data
    @Builder
    class GetCurrentChatRoomMessagesCommand {
        private Long userId;
        private int page;
        private int size;
    }

    @Data
    @Builder
    class GetCurrentChatRoomMessagesResponse {
        private List<ChatRoomMessageDto> messages;
    }

    @Data
    @Builder
    class ChatRoomMessageDto {
        private Long messageId;
        private SenderType senderType;
        private String content;
        private LocalDateTime createdAt;
        private boolean isSaved;
    }
}
