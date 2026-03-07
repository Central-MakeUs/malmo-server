package makeus.cmc.malmo.adaptor.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatMessageEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatMessageSummaryEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatRoomEntity;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.ChatMessageMapper;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.ChatMessageSummaryMapper;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.ChatRoomMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.chat.ChatMessageRepository;
import makeus.cmc.malmo.adaptor.out.persistence.repository.chat.ChatMessageSummaryRepository;
import makeus.cmc.malmo.adaptor.out.persistence.repository.chat.ChatRoomRepository;
import makeus.cmc.malmo.application.port.out.chat.*;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatMessageSummary;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.SenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatRoomPersistenceAdapter
        implements LoadMessagesPort, SaveChatRoomPort, LoadChatRoomPort, SaveChatMessagePort, DeleteChatRoomPort, SaveChatMessageSummaryPort {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageSummaryRepository chatMessageSummaryRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMessageSummaryMapper chatMessageSummaryMapper;

    @Override
    public Optional<ChatMessage> loadMessageById(Long messageId) {
        return chatMessageRepository.findById(messageId)
                .map(chatMessageMapper::toDomain);
    }

    @Override
    public Page<ChatRoomMessageRepositoryDto> loadMessagesDto(ChatRoomId chatRoomId, MemberId memberId, Pageable pageable) {
        return chatMessageRepository.loadCurrentMessagesDto(chatRoomId.getValue(), memberId.getValue(), pageable);
    }

    @Override
    public Page<ChatRoomMessageRepositoryDto> loadMessagesDtoAsc(ChatRoomId chatRoomId, MemberId memberId, Pageable pageable) {
        return chatMessageRepository.loadCurrentMessagesDtoAsc(chatRoomId.getValue(), memberId.getValue(), pageable);
    }

    @Override
    public List<ChatMessage> loadChatRoomMessagesByLevel(ChatRoomId chatRoomId, int level) {
        return chatMessageRepository.findByChatRoomIdAndLevel(chatRoomId.getValue(), level)
                .stream()
                .map(chatMessageMapper::toDomain)
                .toList();
    }

    @Override
    public List<ChatMessage> loadChatRoomLevelAndDetailedLevelMessages(ChatRoomId chatRoomId, int level, int detailedLevel) {
        return chatMessageRepository.findByChatRoomIdAndLevelAndDetailedLevel(chatRoomId.getValue(), level, detailedLevel)
                .stream()
                .map(chatMessageMapper::toDomain)
                .toList();
    }

    @Override
    public List<ChatMessage> loadRecentMessagesByLevel(ChatRoomId chatRoomId, int level, int limit) {
        List<ChatMessageEntity> entities = chatMessageRepository.findByChatRoomIdAndLevelOrderByCreatedAtDesc(
                chatRoomId.getValue(), level);
        return entities.stream()
                .limit(limit)
                .map(chatMessageMapper::toDomain)
                .toList();
    }

    @Override
    public long countMessagesByLevel(ChatRoomId chatRoomId, int level) {
        return chatMessageRepository.countByChatRoomIdAndLevel(chatRoomId.getValue(), level);
    }

    @Override
    public boolean hasUserMessages(ChatRoomId chatRoomId) {
        return chatMessageRepository.existsByChatRoomIdAndSenderType(chatRoomId.getValue(), SenderType.USER);
    }

    @Override
    public List<ChatRoom> loadActiveChatRoomsByMemberId(MemberId memberId) {
        return chatRoomRepository.findActiveChatRoomsByMemberEntityId(memberId.getValue())
                .stream()
                .map(chatRoomMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ChatRoom> loadBeforeInitChatRoomByMemberId(MemberId memberId) {
        return chatRoomRepository.findBeforeInitChatRoomByMemberEntityId(memberId.getValue())
                .map(chatRoomMapper::toDomain);
    }

    @Override
    public ChatRoom saveChatRoom(ChatRoom chatRoom) {
        ChatRoomEntity entity = chatRoomMapper.toEntity(chatRoom);
        ChatRoomEntity savedEntity = chatRoomRepository.save(entity);
        return chatRoomMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public void updateChatRoomTitle(Long chatRoomId, String title) {
        chatRoomRepository.updateTitle(chatRoomId, title);
    }

    @Override
    @Transactional
    public void upgradeChatRoomLevel(Long chatRoomId, int level, int detailedLevel) {
        chatRoomRepository.upgradeLevel(chatRoomId, level, detailedLevel);
    }

    @Override
    @Transactional
    public void upgradeChatRoomDetailedLevel(Long chatRoomId, int detailedLevel) {
        chatRoomRepository.upgradeDetailedLevel(chatRoomId, detailedLevel);
    }

    @Override
    @Transactional
    public void updateChatRoomLastMessageSentTime(Long chatRoomId, LocalDateTime lastMessageSentTime) {
        chatRoomRepository.updateLastMessageSentTime(chatRoomId, lastMessageSentTime);
    }

    @Override
    public ChatMessage saveChatMessage(ChatMessage chatMessage) {
        ChatMessageEntity entity = chatMessageMapper.toEntity(chatMessage);
        ChatMessageEntity savedEntity = chatMessageRepository.save(entity);
        return chatMessageMapper.toDomain(savedEntity);
    }

    @Override
    public List<ChatMessage> saveChatMessages(List<ChatMessage> chatMessages) {
        if (chatMessages == null || chatMessages.isEmpty()) {
            return List.of();
        }
        List<ChatMessageEntity> entities = chatMessages.stream()
                .map(chatMessageMapper::toEntity)
                .toList();
        List<ChatMessageEntity> savedEntities = chatMessageRepository.saveAll(entities);
        return savedEntities.stream()
                .map(chatMessageMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void updateChatMessageCreatedAt(Long messageId, LocalDateTime createdAt) {
        if (messageId == null) {
            return;
        }

        chatMessageRepository.updateCreatedAtById(messageId, createdAt);
    }

    @Override
    public Optional<ChatRoom> loadChatRoomById(ChatRoomId chatRoomId) {
        return chatRoomRepository.findById(chatRoomId.getValue())
                .map(chatRoomMapper::toDomain);
    }

    @Override
    public Page<ChatRoom> loadChatRoomsByMemberId(MemberId memberId, String keyword, Pageable pageable) {
        Page<ChatRoomEntity> chatRoomEntities = chatRoomRepository.loadChatRoomListByMemberId(memberId.getValue(), keyword, pageable);
        return new PageImpl<>(chatRoomEntities.stream().map(chatRoomMapper::toDomain).toList(),
                pageable,
                chatRoomEntities.getTotalElements());
    }

    @Override
    public boolean isMemberOwnerOfChatRooms(MemberId memberId, List<ChatRoomId> chatRoomIds) {
        return chatRoomRepository.isMemberOwnerOfChatRooms(
                memberId.getValue(),
                chatRoomIds.stream().map(ChatRoomId::getValue).toList());
    }

    @Override
    public void deleteChatRooms(List<ChatRoomId> chatRoomIds) {
        chatRoomRepository.deleteChatRooms(
                chatRoomIds.stream().map(ChatRoomId::getValue).toList()
        );
    }

    @Override
    public ChatMessageSummary saveChatMessageSummary(ChatMessageSummary chatMessageSummary) {
        ChatMessageSummaryEntity entity = chatMessageSummaryMapper.toEntity(chatMessageSummary);
        ChatMessageSummaryEntity savedEntity = chatMessageSummaryRepository.save(entity);
        return chatMessageSummaryMapper.toDomain(savedEntity);
    }
}
