package makeus.cmc.malmo.adaptor.out.persistence.repository;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatMessageSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageSummaryRepository extends JpaRepository<ChatMessageSummaryEntity, Long> {

    List<ChatMessageSummaryEntity> findByChatRoomEntityId_ValueAndLevel(Long chatRoomId, int level);

    @Query("SELECT c FROM ChatMessageSummaryEntity c WHERE c.chatRoomEntityId.value = :chatRoomId and c.isForCurrentLevel = false")
    List<ChatMessageSummaryEntity> findNotCurrentMessagesByChatRoomEntityId(Long chatRoomId);
}
