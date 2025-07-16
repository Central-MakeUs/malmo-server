package makeus.cmc.malmo.adaptor.out.persistence;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatMessageSummaryEntity;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.ChatMessageSummaryMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.ChatMessageSummaryRepository;
import makeus.cmc.malmo.application.port.out.LoadChatMessageSummaryPort;
import makeus.cmc.malmo.application.port.out.LoadSummarizedMessages;
import makeus.cmc.malmo.application.port.out.SaveChatMessageSummaryPort;
import makeus.cmc.malmo.domain.model.chat.ChatMessageSummary;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ChatMessageSummaryPersistenceAdapter
        implements LoadChatMessageSummaryPort, LoadSummarizedMessages, SaveChatMessageSummaryPort {

    private final ChatMessageSummaryRepository chatMessageSummaryRepository;
    private final ChatMessageSummaryMapper chatMessageSummaryMapper;

    @Override
    public List<ChatMessageSummary> loadChatMessageSummaries(ChatRoomId chatRoomId, int level) {
        return chatMessageSummaryRepository.findByChatRoomEntityId_ValueAndLevel(chatRoomId.getValue(), level)
                .stream()
                .map(chatMessageSummaryMapper::toDomain)
                .toList();
    }

    @Override
    public List<ChatMessageSummary> loadSummarizedMessagesNotCurrent(ChatRoomId chatRoomId) {
        return chatMessageSummaryRepository.findNotCurrentMessagesByChatRoomEntityId(chatRoomId.getValue())
                .stream()
                .map(chatMessageSummaryMapper::toDomain)
                .toList();
    }

    @Override
    public void saveChatMessageSummary(ChatMessageSummary chatMessageSummary) {
        ChatMessageSummaryEntity entity = chatMessageSummaryMapper.toEntity(chatMessageSummary);
        chatMessageSummaryRepository.save(entity);
    }
}
