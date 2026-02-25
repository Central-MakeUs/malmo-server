package makeus.cmc.malmo.application.helper.chat_room;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.chat.DeleteChatRoomPort;
import makeus.cmc.malmo.application.port.out.chat.SaveChatMessagePort;
import makeus.cmc.malmo.application.port.out.chat.SaveChatMessageSummaryPort;
import makeus.cmc.malmo.application.port.out.chat.SaveChatRoomPort;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatMessageSummary;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatRoomCommandHelper {

    private final SaveChatRoomPort saveChatRoomPort;
    private final DeleteChatRoomPort deleteChatRoomPort;

    private final SaveChatMessagePort saveChatMessagePort;
    private final SaveChatMessageSummaryPort saveChatMessageSummaryPort;

    public ChatRoom saveChatRoom(ChatRoom chatRoom) {
        return saveChatRoomPort.saveChatRoom(chatRoom);
    }

    public void updateChatRoomTitle(Long chatRoomId, String title) {
        saveChatRoomPort.updateChatRoomTitle(chatRoomId, title);
    }

    public void upgradeChatRoomLevel(Long chatRoomId, int level, int detailedLevel) {
        saveChatRoomPort.upgradeChatRoomLevel(chatRoomId, level, detailedLevel);
    }

    public void upgradeChatRoomDetailedLevel(Long chatRoomId, int detailedLevel) {
        saveChatRoomPort.upgradeChatRoomDetailedLevel(chatRoomId, detailedLevel);
    }

    public void updateChatRoomLastMessageSentTime(Long chatRoomId, LocalDateTime lastMessageSentTime) {
        saveChatRoomPort.updateChatRoomLastMessageSentTime(chatRoomId, lastMessageSentTime);
    }

    public void deleteChatRooms(List<ChatRoomId> chatRoomIds) {
        deleteChatRoomPort.deleteChatRooms(chatRoomIds);
    }

    public ChatMessage saveChatMessage(ChatMessage chatMessage) {
        return saveChatMessagePort.saveChatMessage(chatMessage);
    }

    public List<ChatMessage> saveChatMessages(List<ChatMessage> chatMessages) {
        return saveChatMessagePort.saveChatMessages(chatMessages);
    }

    public ChatMessageSummary saveChatMessageSummary(ChatMessageSummary chatMessageSummary) {
        return saveChatMessageSummaryPort.saveChatMessageSummary(chatMessageSummary);
    }
}
