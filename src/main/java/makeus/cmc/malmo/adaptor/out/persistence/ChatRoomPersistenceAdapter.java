package makeus.cmc.malmo.adaptor.out.persistence;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatMessageEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatRoomEntity;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.ChatMessageMapper;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.ChatRoomMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.ChatMessageRepository;
import makeus.cmc.malmo.adaptor.out.persistence.repository.ChatRoomRepository;
import makeus.cmc.malmo.application.port.out.*;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatRoomPersistenceAdapter
        implements LoadCurrentMessagesPort, SaveChatRoomPort, LoadChatRoomPort, SaveChatMessagePort,
        LoadUnsummarizedChatMessages {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    public List<ChatMessage> loadMessages(ChatRoomId chatRoomId) {
        return chatMessageRepository.findByChatRoomId(chatRoomId.getValue())
                .stream()
                .map(chatMessageMapper::toDomain)
                .toList();
    }

    @Override
    public List<ChatRoomMessageRepositoryDto> loadMessagesDto(ChatRoomId chatRoomId, int page, int size) {
        return chatMessageRepository.loadCurrentMessagesDto(chatRoomId.getValue(), page, size);
    }

    @Override
    public Optional<ChatRoom> loadCurrentChatRoomByMemberId(MemberId memberId) {
        return chatRoomRepository.findCurrentChatRoomByMemberEntityId(memberId.getValue())
                .map(chatRoomMapper::toDomain);
    }

    @Override
    public Optional<ChatRoom> loadMaxLevelChatRoomByMemberId(MemberId memberId) {
        return chatRoomRepository.findMaxLevelChatRoomByMemberEntityId(memberId.getValue())
                .map(chatRoomMapper::toDomain);
    }

    @Override
    public ChatRoom saveChatRoom(ChatRoom chatRoom) {
        ChatRoomEntity entity = chatRoomMapper.toEntity(chatRoom);
        ChatRoomEntity savedEntity = chatRoomRepository.save(entity);
        return chatRoomMapper.toDomain(savedEntity);
    }

    @Override
    public void updateAllMessagesSummarizedIsTrue(ChatRoomId chatRoomId) {
        chatMessageRepository.updateChatMessageSummarizedAllTrue(chatRoomId.getValue());
    }

    @Override
    public ChatMessage saveChatMessage(ChatMessage chatMessage) {
        ChatMessageEntity entity = chatMessageMapper.toEntity(chatMessage);
        ChatMessageEntity savedEntity = chatMessageRepository.save(entity);
        return chatMessageMapper.toDomain(savedEntity);
    }


    @Override
    public Optional<ChatRoom> loadChatRoomById(ChatRoomId chatRoomId) {
        return chatRoomRepository.findById(chatRoomId.getValue())
                .map(chatRoomMapper::toDomain);
    }

    @Override
    public List<ChatMessage> getUnsummarizedChatMessages(ChatRoomId chatRoomId) {
        return chatMessageRepository.findUnsummarizedChatMessagesByChatRoomId(chatRoomId.getValue())
                .stream()
                .map(chatMessageMapper::toDomain)
                .toList();
    }
}
