package makeus.cmc.malmo.adaptor.out.persistence.repository.chat;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.MemberChatRoomMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberChatRoomMetadataRepository extends JpaRepository<MemberChatRoomMetadataEntity, Long> {

    @Query("SELECT m FROM MemberChatRoomMetadataEntity m WHERE m.chatRoomId = :chatRoomId ORDER BY m.level, m.detailedLevel")
    List<MemberChatRoomMetadataEntity> findAllByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT m FROM MemberChatRoomMetadataEntity m WHERE m.chatRoomId = :chatRoomId AND m.level = :level AND m.detailedLevel = :detailedLevel")
    List<MemberChatRoomMetadataEntity> findByChatRoomIdAndLevelAndDetailedLevel(@Param("chatRoomId") Long chatRoomId, @Param("level") int level, @Param("detailedLevel") int detailedLevel);

    boolean existsByChatRoomIdAndMemberIdAndLevelAndDetailedLevel(Long chatRoomId, Long memberId, int level, int detailedLevel);
}
