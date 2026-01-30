package makeus.cmc.malmo.adaptor.out.persistence.entity.member;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import makeus.cmc.malmo.adaptor.out.persistence.entity.BaseTimeEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.CoupleEntityId;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.EmailForwardingStatus;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;
import makeus.cmc.malmo.domain.value.type.RelationshipStatus;

import java.time.LocalDate;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class MemberEntity extends BaseTimeEntity {

    @Column(name = "memberId")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(value = EnumType.STRING)
    private Provider provider;

    private String providerId;

    @Enumerated(value = EnumType.STRING)
    private MemberRole memberRole;

    @Enumerated(value = EnumType.STRING)
    private MemberState memberState;

    private boolean isAlarmOn;

    private String firebaseToken;

    private String refreshToken;

    @Enumerated(value = EnumType.STRING)
    private LoveTypeCategory loveTypeCategory;

    private float avoidanceRate;

    private float anxietyRate;

    private String nickname;

    private String email;

    @Builder.Default
    @Enumerated(value = EnumType.STRING)
    private EmailForwardingStatus emailForwardingStatus = EmailForwardingStatus.ENABLED;

    @Embedded
    private InviteCodeEntityValue inviteCodeEntityValue;

    private LocalDate startLoveDate;

    private String oauthToken;

    @Embedded
    private CoupleEntityId coupleEntityId;

    @Enumerated(value = EnumType.STRING)
    private RelationshipStatus relationshipStatus;

    private String personalityType;

    private String otherPersonalityType;
}
