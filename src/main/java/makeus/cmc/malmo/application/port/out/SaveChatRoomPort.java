package makeus.cmc.malmo.application.port.out;

import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;

public interface SaveChatRoomPort {
    ChatRoom saveChatRoom(ChatRoom chatRoom);

    void updateAllMessagesSummarizedIsTrue(ChatRoomId chatRoomId);
}
