package makeus.cmc.malmo.adaptor.out.persistence.repository.chat;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatMessageEntity;
import makeus.cmc.malmo.domain.value.type.SenderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long>, ChatMessageRepositoryCustom {

    @Query("SELECT c FROM ChatMessageEntity c WHERE c.chatRoomEntityId.value = :chatRoomId AND c.level = :level")
    List<ChatMessageEntity> findByChatRoomIdAndLevel(Long chatRoomId, int level);

    @Query("SELECT c FROM ChatMessageEntity c WHERE c.chatRoomEntityId.value = :chatRoomId AND c.level = :level AND c.detailedLevel = :detailedLevel")
    List<ChatMessageEntity> findByChatRoomIdAndLevelAndDetailedLevel(Long chatRoomId, int level, int detailedLevel);

    @Query("SELECT c FROM ChatMessageEntity c WHERE c.chatRoomEntityId.value = :chatRoomId AND c.level = :level ORDER BY c.createdAt DESC")
    List<ChatMessageEntity> findByChatRoomIdAndLevelOrderByCreatedAtDesc(@Param("chatRoomId") Long chatRoomId, @Param("level") int level);

    @Query("SELECT COUNT(c) FROM ChatMessageEntity c WHERE c.chatRoomEntityId.value = :chatRoomId AND c.level = :level")
    long countByChatRoomIdAndLevel(@Param("chatRoomId") Long chatRoomId, @Param("level") int level);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ChatMessageEntity c WHERE c.chatRoomEntityId.value = :chatRoomId AND c.senderType = :senderType")
    boolean existsByChatRoomIdAndSenderType(@Param("chatRoomId") Long chatRoomId, @Param("senderType") SenderType senderType);
}
