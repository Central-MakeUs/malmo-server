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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatRoomPersistenceAdapter
        implements LoadMessagesPort, SaveChatRoomPort, LoadChatRoomPort, SaveChatMessagePort, DeleteChatRoomPort {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    public Page<ChatRoomMessageRepositoryDto> loadMessagesDto(ChatRoomId chatRoomId, Pageable pageable) {
        return chatMessageRepository.loadCurrentMessagesDto(chatRoomId.getValue(), pageable);
    }

    @Override
    public Page<ChatRoomMessageRepositoryDto> loadMessagesDtoAsc(ChatRoomId chatRoomId, Pageable pageable) {
        return chatMessageRepository.loadCurrentMessagesDtoAsc(chatRoomId.getValue(), pageable);
    }

    @Override
    public List<ChatMessage> loadChatRoomMessagesByLevel(ChatRoomId chatRoomId, int level) {
        return chatMessageRepository.findByChatRoomIdAndLevel(chatRoomId.getValue(), level)
                .stream()
                .map(chatMessageMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ChatRoom> loadCurrentChatRoomByMemberId(MemberId memberId) {
        return chatRoomRepository.findCurrentChatRoomByMemberEntityId(memberId.getValue())
                .map(chatRoomMapper::toDomain);
    }

    @Override
    public ChatRoom saveChatRoom(ChatRoom chatRoom) {
        ChatRoomEntity entity = chatRoomMapper.toEntity(chatRoom);
        ChatRoomEntity savedEntity = chatRoomRepository.save(entity);
        return chatRoomMapper.toDomain(savedEntity);
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
    public Optional<ChatRoom> loadPausedChatRoomByMemberId(MemberId memberId) {
        return chatRoomRepository.findPausedChatRoomByMemberEntityId(memberId.getValue())
                .map(chatRoomMapper::toDomain);
    }

    @Override
    public Page<ChatRoom> loadAliveChatRoomsByMemberId(MemberId memberId, String keyword, Pageable pageable) {
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
}
