package makeus.cmc.malmo.adaptor.out.persistence.repository.chat;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatMessageEntity;
import makeus.cmc.malmo.domain.value.type.SenderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    @Query("""
            SELECT DISTINCT cr.memberEntityId.value
            FROM ChatMessageEntity cm
            JOIN ChatRoomEntity cr ON cr.id = cm.chatRoomEntityId.value
            WHERE cm.senderType = :senderType
              AND cm.createdAt >= :weekStartAt
              AND cm.createdAt < :weekEndAtExclusive
              AND cm.deletedAt IS NULL
              AND cr.chatRoomState <> makeus.cmc.malmo.domain.value.state.ChatRoomState.DELETED
            """)
    List<Long> findDistinctMemberIdsBySenderTypeAndCreatedAtBetween(
            @Param("senderType") SenderType senderType,
            @Param("weekStartAt") LocalDateTime weekStartAt,
            @Param("weekEndAtExclusive") LocalDateTime weekEndAtExclusive
    );

    @Query("""
            SELECT COUNT(cm)
            FROM ChatMessageEntity cm
            JOIN ChatRoomEntity cr ON cr.id = cm.chatRoomEntityId.value
            WHERE cr.memberEntityId.value = :memberId
              AND cm.senderType = :senderType
              AND cm.createdAt >= :weekStartAt
              AND cm.createdAt < :weekEndAtExclusive
              AND cm.deletedAt IS NULL
              AND cr.chatRoomState <> makeus.cmc.malmo.domain.value.state.ChatRoomState.DELETED
            """)
    long countByMemberIdAndSenderTypeAndCreatedAtBetween(
            @Param("memberId") Long memberId,
            @Param("senderType") SenderType senderType,
            @Param("weekStartAt") LocalDateTime weekStartAt,
            @Param("weekEndAtExclusive") LocalDateTime weekEndAtExclusive
    );

    @Query("""
            SELECT cm
            FROM ChatMessageEntity cm
            JOIN ChatRoomEntity cr ON cr.id = cm.chatRoomEntityId.value
            WHERE cr.memberEntityId.value = :memberId
              AND cm.senderType = :senderType
              AND cm.createdAt >= :weekStartAt
              AND cm.createdAt < :weekEndAtExclusive
              AND cm.deletedAt IS NULL
              AND cr.chatRoomState <> makeus.cmc.malmo.domain.value.state.ChatRoomState.DELETED
            ORDER BY cm.createdAt ASC
            """)
    List<ChatMessageEntity> findByMemberIdAndSenderTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
            @Param("memberId") Long memberId,
            @Param("senderType") SenderType senderType,
            @Param("weekStartAt") LocalDateTime weekStartAt,
            @Param("weekEndAtExclusive") LocalDateTime weekEndAtExclusive
    );

    @Query("""
            SELECT cm
            FROM ChatMessageEntity cm
            WHERE cm.chatRoomEntityId.value IN :chatRoomIds
              AND cm.senderType = :senderType
              AND cm.createdAt >= :weekStartAt
              AND cm.createdAt < :weekEndAtExclusive
              AND cm.deletedAt IS NULL
            ORDER BY cm.createdAt DESC
            """)
    List<ChatMessageEntity> findByChatRoomIdsAndSenderTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("chatRoomIds") List<Long> chatRoomIds,
            @Param("senderType") SenderType senderType,
            @Param("weekStartAt") LocalDateTime weekStartAt,
            @Param("weekEndAtExclusive") LocalDateTime weekEndAtExclusive
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ChatMessageEntity c SET c.createdAt = :createdAt WHERE c.id = :id")
    int updateCreatedAtById(@Param("id") Long messageId, @Param("createdAt") LocalDateTime createdAt);
}
