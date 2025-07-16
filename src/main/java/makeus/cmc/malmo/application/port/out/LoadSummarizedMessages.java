package makeus.cmc.malmo.application.port.out;

import makeus.cmc.malmo.domain.model.chat.ChatMessageSummary;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;

import java.util.List;

public interface LoadSummarizedMessages {

    List<ChatMessageSummary> loadSummarizedMessagesNotCurrent(ChatRoomId chatRoomId);
}
