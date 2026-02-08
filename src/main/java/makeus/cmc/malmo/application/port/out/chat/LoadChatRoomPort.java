package makeus.cmc.malmo.application.port.out.chat;

import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface LoadChatRoomPort {
    // 진행 중인 채팅방 목록 조회 (복수)
    List<ChatRoom> loadActiveChatRoomsByMemberId(MemberId memberId);
    
    // 초기화 전 채팅방 조회 (BEFORE_INIT 상태)
    Optional<ChatRoom> loadBeforeInitChatRoomByMemberId(MemberId memberId);
    
    // ID로 채팅방 조회 (유지)
    Optional<ChatRoom> loadChatRoomById(ChatRoomId chatRoomId);
    
    // 삭제되지 않은 모든 채팅방 조회 (페이지네이션)
    Page<ChatRoom> loadChatRoomsByMemberId(MemberId memberId, String keyword, Pageable pageable);
    
    // 소유권 검증 (유지)
    boolean isMemberOwnerOfChatRooms(MemberId memberId, List<ChatRoomId> chatRoomIds);
}
