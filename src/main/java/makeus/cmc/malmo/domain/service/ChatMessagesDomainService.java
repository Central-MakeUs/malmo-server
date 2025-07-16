package makeus.cmc.malmo.domain.service;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.*;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatMessageSummary;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatMessagesDomainService {

    private final LoadCurrentMessagesPort loadMessagesPort;
    private final SaveChatMessagePort saveChatMessagePort;

    private final LoadChatMessageSummaryPort loadChatMessageSummaryPort;
    private final LoadUnsummarizedChatMessages loadUnsummarizedChatMessages;
    private final LoadSummarizedMessages loadSummarizedMessages;

    public List<LoadCurrentMessagesPort.ChatRoomMessageRepositoryDto> getChatMessagesDto(ChatRoomId chatRoomId, int page, int size) {
        return loadMessagesPort.loadMessagesDto(chatRoomId, page, size);
    }

    public List<ChatMessage> getChatMessages(ChatRoomId chatRoomId) {
        return loadMessagesPort.loadMessages(chatRoomId);
    }

    @Transactional
    public ChatMessage createUserTextMessage(ChatRoomId chatRoomId, String content) {
        ChatMessage chatMessage = ChatMessage.createUserTextMessage(chatRoomId, content);
        return saveChatMessagePort.saveChatMessage(chatMessage);
    }

    @Transactional
    public ChatMessage createAiTextMessage(ChatRoomId chatRoomId, String content) {
        ChatMessage chatMessage = ChatMessage.createAssistantTextMessage(chatRoomId, content);
        return saveChatMessagePort.saveChatMessage(chatMessage);
    }

    public List<ChatMessageSummary> getCurrentSummarizedMessagesByLevel(ChatRoomId chatRoomId, int level) {
        return loadChatMessageSummaryPort.loadChatMessageSummaries(chatRoomId, level);
    }

    public List<ChatMessage> getNotSummarizedChatMessages(ChatRoomId chatRoomId) {
        return loadUnsummarizedChatMessages.getUnsummarizedChatMessages(chatRoomId);
    }

    public List<ChatMessageSummary> getPreviousLevelsSummarizedMessages(ChatRoomId chatRoomId) {
        return loadSummarizedMessages.loadSummarizedMessagesNotCurrent(chatRoomId);
    }
}
