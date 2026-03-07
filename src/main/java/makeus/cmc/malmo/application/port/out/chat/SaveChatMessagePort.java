package makeus.cmc.malmo.application.port.out.chat;

import makeus.cmc.malmo.domain.model.chat.ChatMessage;

import java.util.List;
import java.time.LocalDateTime;

public interface SaveChatMessagePort {
    ChatMessage saveChatMessage(ChatMessage chatMessage);
    
    List<ChatMessage> saveChatMessages(List<ChatMessage> chatMessages);

    void updateChatMessageCreatedAt(Long messageId, LocalDateTime createdAt);
}
