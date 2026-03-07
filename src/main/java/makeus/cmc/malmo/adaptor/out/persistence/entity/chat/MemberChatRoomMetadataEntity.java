package makeus.cmc.malmo.adaptor.out.persistence.entity.chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import makeus.cmc.malmo.adaptor.out.persistence.entity.BaseTimeEntity;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "member_chat_room_metadata",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_chat_room_metadata_room_member_level",
                columnNames = {"chatRoomId", "memberId", "level", "detailedLevel"}))
public class MemberChatRoomMetadataEntity extends BaseTimeEntity {

    @Column(name = "memberChatRoomMetadataId")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chatRoomId;

    private Long memberId;

    private int level;

    private int detailedLevel;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;
}
