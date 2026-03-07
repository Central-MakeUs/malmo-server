package makeus.cmc.malmo.application.port.out.chat;

import makeus.cmc.malmo.domain.model.chat.ChatRoom;

import java.time.LocalDateTime;

public interface SaveChatRoomPort {
    ChatRoom saveChatRoom(ChatRoom chatRoom);
    void updateChatRoomTitle(Long chatRoomId, String title);
    void upgradeChatRoomLevel(Long chatRoomId, int level, int detailedLevel);
    void upgradeChatRoomDetailedLevel(Long chatRoomId, int detailedLevel);
    void updateChatRoomLastMessageSentTime(Long chatRoomId, LocalDateTime lastMessageSentTime);
}
