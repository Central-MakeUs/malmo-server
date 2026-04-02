package makeus.cmc.malmo.adaptor.out.persistence.repository.chat;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.MemberChatRoomMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberChatRoomMetadataRepository extends JpaRepository<MemberChatRoomMetadataEntity, Long> {

    @Query("SELECT m FROM MemberChatRoomMetadataEntity m WHERE m.chatRoomId = :chatRoomId ORDER BY m.level, m.detailedLevel")
    List<MemberChatRoomMetadataEntity> findAllByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT m FROM MemberChatRoomMetadataEntity m WHERE m.chatRoomId = :chatRoomId AND m.level = :level AND m.detailedLevel = :detailedLevel")
    List<MemberChatRoomMetadataEntity> findByChatRoomIdAndLevelAndDetailedLevel(@Param("chatRoomId") Long chatRoomId, @Param("level") int level, @Param("detailedLevel") int detailedLevel);

    @Query("""
            SELECT DISTINCT m.memberId
            FROM MemberChatRoomMetadataEntity m
            JOIN ChatRoomEntity cr ON cr.id = m.chatRoomId
            WHERE m.createdAt >= :weekStartAt
              AND m.createdAt < :weekEndAtExclusive
              AND cr.chatRoomState <> makeus.cmc.malmo.domain.value.state.ChatRoomState.DELETED
              AND ((m.level = 1 AND m.detailedLevel = :lastLevelOneDetailedLevel) OR m.level >= 2)
            """)
    List<Long> findDistinctCandidateMemberIdsByCreatedAtBetween(
            @Param("weekStartAt") LocalDateTime weekStartAt,
            @Param("weekEndAtExclusive") LocalDateTime weekEndAtExclusive,
            @Param("lastLevelOneDetailedLevel") int lastLevelOneDetailedLevel
    );

    @Query("""
            SELECT DISTINCT cr.id
            FROM ChatRoomEntity cr
            WHERE cr.memberEntityId.value = :memberId
              AND cr.chatRoomState <> makeus.cmc.malmo.domain.value.state.ChatRoomState.DELETED
              AND EXISTS (
                    SELECT 1
                    FROM MemberChatRoomMetadataEntity finalMeta
                    WHERE finalMeta.chatRoomId = cr.id
                      AND finalMeta.level = 1
                      AND finalMeta.detailedLevel = :lastLevelOneDetailedLevel
              )
              AND (
                    EXISTS (
                        SELECT 1
                        FROM MemberChatRoomMetadataEntity weeklyFinalMeta
                        WHERE weeklyFinalMeta.chatRoomId = cr.id
                          AND weeklyFinalMeta.level = 1
                          AND weeklyFinalMeta.detailedLevel = :lastLevelOneDetailedLevel
                          AND weeklyFinalMeta.createdAt >= :weekStartAt
                          AND weeklyFinalMeta.createdAt < :weekEndAtExclusive
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM MemberChatRoomMetadataEntity progressedMeta
                        WHERE progressedMeta.chatRoomId = cr.id
                          AND progressedMeta.level >= 2
                          AND progressedMeta.createdAt >= :weekStartAt
                          AND progressedMeta.createdAt < :weekEndAtExclusive
                    )
              )
            """)
    List<Long> findEligibleChatRoomIdsByMemberIdAndWeek(
            @Param("memberId") Long memberId,
            @Param("weekStartAt") LocalDateTime weekStartAt,
            @Param("weekEndAtExclusive") LocalDateTime weekEndAtExclusive,
            @Param("lastLevelOneDetailedLevel") int lastLevelOneDetailedLevel
    );

    @Query("SELECT m FROM MemberChatRoomMetadataEntity m WHERE m.chatRoomId IN :chatRoomIds ORDER BY m.chatRoomId, m.level, m.detailedLevel")
    List<MemberChatRoomMetadataEntity> findAllByChatRoomIdInOrderByChatRoomIdAscLevelAscDetailedLevelAsc(@Param("chatRoomIds") List<Long> chatRoomIds);

    boolean existsByChatRoomIdAndMemberIdAndLevelAndDetailedLevel(Long chatRoomId, Long memberId, int level, int detailedLevel);
}
