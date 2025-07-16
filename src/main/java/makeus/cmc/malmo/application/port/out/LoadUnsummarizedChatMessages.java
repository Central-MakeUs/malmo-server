package makeus.cmc.malmo.application.port.out;

import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;

import java.util.List;

public interface LoadUnsummarizedChatMessages {
    List<ChatMessage> getUnsummarizedChatMessages(ChatRoomId chatRoomId);
}
