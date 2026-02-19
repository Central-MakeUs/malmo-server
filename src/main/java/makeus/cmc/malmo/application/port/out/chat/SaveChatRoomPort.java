package makeus.cmc.malmo.application.port.out.chat;

import makeus.cmc.malmo.domain.model.chat.ChatRoom;

public interface SaveChatRoomPort {
    ChatRoom saveChatRoom(ChatRoom chatRoom);
    void updateChatRoomTitle(Long chatRoomId, String title);
}
