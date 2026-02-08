package makeus.cmc.malmo.adaptor.out.persistence.repository.chat;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long>, ChatRoomRepositoryCustom {

    // 진행 중인 채팅방 목록 조회 (ALIVE 상태만)
    @Query("SELECT c FROM ChatRoomEntity c WHERE c.memberEntityId.value = ?1 AND c.chatRoomState = 'ALIVE' ORDER BY c.lastMessageSentTime DESC")
    List<ChatRoomEntity> findActiveChatRoomsByMemberEntityId(Long memberId);

    // 초기화 전 채팅방 조회 (BEFORE_INIT 상태, 1개만)
    @Query("SELECT c FROM ChatRoomEntity c WHERE c.memberEntityId.value = ?1 AND c.chatRoomState = 'BEFORE_INIT' ORDER BY c.createdAt DESC LIMIT 1")
    Optional<ChatRoomEntity> findBeforeInitChatRoomByMemberEntityId(Long memberId);
}
