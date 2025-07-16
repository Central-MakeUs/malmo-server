package makeus.cmc.malmo.application.port.in;

import lombok.Builder;
import lombok.Data;

public interface SendChatMessageUseCase {
    SendChatMessageResponse processUserMessage(SendChatMessageCommand command);

    SendChatMessageResponse upgradeChatRoom(SendChatMessageCommand command);

    @Data
    @Builder
    class SendChatMessageCommand {
        private Long userId;
        private String message;
    }

    @Data
    @Builder
    class SendChatMessageResponse {
        private Long messageId;
    }
}
