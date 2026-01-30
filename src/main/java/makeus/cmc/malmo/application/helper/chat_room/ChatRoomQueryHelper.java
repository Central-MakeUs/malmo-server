package makeus.cmc.malmo.application.helper.chat_room;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.exception.ChatRoomNotFoundException;
import makeus.cmc.malmo.application.exception.MemberAccessDeniedException;
import makeus.cmc.malmo.application.exception.NotValidChatRoomException;
import makeus.cmc.malmo.application.port.out.chat.LoadChatRoomMetadataPort;
import makeus.cmc.malmo.application.port.out.chat.LoadChatRoomPort;
import makeus.cmc.malmo.application.port.out.chat.LoadMessagesPort;
import makeus.cmc.malmo.application.port.out.chat.LoadSummarizedMessages;
import makeus.cmc.malmo.application.port.out.member.LoadMemberMemoryPort;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatMessageSummary;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.member.MemberMemory;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatRoomQueryHelper {

    private final LoadChatRoomPort loadChatRoomPort;
    private final LoadChatRoomMetadataPort loadChatRoomMetadataPort;

    private final LoadMemberMemoryPort loadMemberMemoryPort;

    private final LoadMessagesPort loadMessagesPort;
    private final LoadSummarizedMessages loadSummarizedMessages;

    // 진행 중인 채팅방 목록 조회
    public List<ChatRoom> getActiveChatRoomsByMemberId(MemberId memberId) {
        return loadChatRoomPort.loadActiveChatRoomsByMemberId(memberId);
    }

    public LoadChatRoomMetadataPort.ChatRoomMetadataDto getChatRoomMetadata(MemberId memberId) {
        return loadChatRoomMetadataPort.loadChatRoomMetadata(memberId)
                .orElse(new LoadChatRoomMetadataPort.ChatRoomMetadataDto(null, null));
    }

    public ChatRoom getChatRoomByIdOrThrow(ChatRoomId chatRoomId) {
        return loadChatRoomPort.loadChatRoomById(chatRoomId)
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    public Page<ChatRoom> getChatRoomsByMemberId(MemberId memberId, String keyword, Pageable pageable) {
        return loadChatRoomPort.loadChatRoomsByMemberId(memberId, keyword, pageable);
    }

    public void validateChatRoomOwnership(MemberId memberId, ChatRoomId chatRoomId) {
        // 채팅방이 존재하는지 확인하고, 존재하지 않으면 예외 발생
        ChatRoom chatRoom = loadChatRoomPort.loadChatRoomById(chatRoomId)
                .orElseThrow(ChatRoomNotFoundException::new);

        // 채팅방의 소유자와 요청한 멤버가 일치하는지 확인
        if (!chatRoom.isOwner(memberId)) {
            throw new MemberAccessDeniedException("채팅방에 접근할 권한이 없습니다.");
        }
    }

    public void validateChatRoomsOwnership(MemberId memberId, List<ChatRoomId> chatRoomIds) {
        boolean valid = loadChatRoomPort.isMemberOwnerOfChatRooms(memberId, chatRoomIds);

        if (!valid) {
            throw new MemberAccessDeniedException("채팅방에 접근할 권한이 없습니다.");
        }
    }

    // 채팅방 유효성 검증 (특정 채팅방 ID 기반)
    public void validateChatRoomActive(ChatRoomId chatRoomId) {
        ChatRoom chatRoom = loadChatRoomPort.loadChatRoomById(chatRoomId)
                .orElseThrow(ChatRoomNotFoundException::new);
        
        if (!chatRoom.isChatRoomValid()) {
            throw new NotValidChatRoomException();
        }
    }

    /*
     채팅방 메시지 Query Methods
     */

    public Page<LoadMessagesPort.ChatRoomMessageRepositoryDto> getChatMessagesDtoDesc(ChatRoomId chatRoomId, MemberId memberId, Pageable pageable) {
        return loadMessagesPort.loadMessagesDto(chatRoomId, memberId, pageable);
    }

    public Page<LoadMessagesPort.ChatRoomMessageRepositoryDto> getChatMessagesDtoAsc(ChatRoomId chatRoomId, MemberId memberId, Pageable pageable) {
        return loadMessagesPort.loadMessagesDtoAsc(chatRoomId, memberId, pageable);
    }

    public List<ChatMessageSummary> getSummarizedMessages(ChatRoomId chatRoomId) {
        return loadSummarizedMessages.loadSummarizedMessages(chatRoomId);
    }

    public List<ChatMessage> getChatRoomLevelMessages(ChatRoomId chatRoomId, int level) {
        return loadMessagesPort.loadChatRoomMessagesByLevel(chatRoomId, level);
    }

    public List<ChatMessage> getChatRoomLevelAndDetailedLevelMessages(ChatRoomId chatRoomId, int level, int detailedLevel) {
        return loadMessagesPort.loadChatRoomLevelAndDetailedLevelMessages(chatRoomId, level, detailedLevel);
    }

    public List<MemberMemory> getMemberMemoriesByMemberId(MemberId memberId) {
        return loadMemberMemoryPort.loadMemberMemoryByMemberId(memberId);
    }

    public List<ChatMessage> getRecentMessages(ChatRoomId chatRoomId, int level, int limit) {
        return loadMessagesPort.loadRecentMessagesByLevel(chatRoomId, level, limit);
    }

    public long countMessagesByLevel(ChatRoomId chatRoomId, int level) {
        return loadMessagesPort.countMessagesByLevel(chatRoomId, level);
    }

    public Optional<ChatMessageSummary> getLatestSummaryByLevel(ChatRoomId chatRoomId, int level) {
        return loadSummarizedMessages.loadLatestSummaryByLevel(chatRoomId, level);
    }

    public boolean hasUserMessages(ChatRoomId chatRoomId) {
        return loadMessagesPort.hasUserMessages(chatRoomId);
    }
}
