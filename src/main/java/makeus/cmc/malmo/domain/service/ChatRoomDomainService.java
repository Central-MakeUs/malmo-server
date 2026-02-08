package makeus.cmc.malmo.domain.service;

import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.SenderType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ChatRoomDomainService {

    public ChatRoom createChatRoom(MemberId memberId) {
        return ChatRoom.createChatRoom(memberId);
    }

    public ChatMessage createUserMessage(ChatRoomId chatRoomId, int level, int detailedLevel, String content) {
        return ChatMessage.createUserTextMessage(chatRoomId, level, detailedLevel, content);
    }

    public ChatMessage createAiMessage(ChatRoomId chatRoomId, int level, int detailedLevel, String content) {
        return ChatMessage.createAssistantTextMessage(chatRoomId, level, detailedLevel, content);
    }

    public ChatMessage createAiMessage(ChatRoomId chatRoomId, int level, int detailedLevel, String content, LocalDateTime createdAt) {
        return ChatMessage.createAssistantTextMessage(chatRoomId, level, detailedLevel, content, createdAt);
    }

    public ChatMessage createSystemMessage(ChatRoomId chatRoomId, int level, int detailedLevel, String content) {
        return ChatMessage.createSystemTextMessage(chatRoomId, level, detailedLevel, content);
    }
}
