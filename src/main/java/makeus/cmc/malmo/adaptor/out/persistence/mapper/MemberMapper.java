package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.CoupleEntityId;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.CoupleId;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MemberMapper {

    public Member toDomain(MemberEntity entity) {
        return Member.from(
                entity.getId(),
                entity.getProvider(),
                entity.getProviderId(),
                entity.getMemberRole(),
                entity.getMemberState(),
                entity.isAlarmOn(),
                entity.getFirebaseToken(),
                entity.getRefreshToken(),
                entity.getLoveTypeCategory(),
                entity.getAvoidanceRate(),
                entity.getAnxietyRate(),
                entity.getNickname(),
                entity.getEmail(),
                entity.getEmailForwardingStatus(),
                entity.getInviteCodeEntityValue() != null ? InviteCodeValue.of(entity.getInviteCodeEntityValue().getValue()) : null,
                entity.getStartLoveDate(),
                entity.getOauthToken(),
                entity.getCoupleEntityId() != null ? CoupleId.of(entity.getCoupleEntityId().getValue()) : null,
                entity.getRelationshipStatus(),
                entity.getMbti(),
                entity.getPartnerMbti(),
                entity.getPartnerLoveTypeCategory(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                entity.getDeletedAt()
        );
    }

    public MemberEntity toEntity(Member domain) {
        return MemberEntity.builder()
                .id(domain.getId())
                .provider(domain.getProvider())
                .providerId(domain.getProviderId())
                .memberRole(domain.getMemberRole())
                .memberState(domain.getMemberState())
                .isAlarmOn(domain.isAlarmOn())
                .firebaseToken(domain.getFirebaseToken())
                .refreshToken(domain.getRefreshToken())
                .loveTypeCategory(domain.getLoveTypeCategory())
                .avoidanceRate(domain.getAvoidanceRate())
                .anxietyRate(domain.getAnxietyRate())
                .email(domain.getEmail())
                .emailForwardingStatus(domain.getEmailForwardingStatus())
                .nickname(domain.getNickname())
                .inviteCodeEntityValue(
                        domain.getInviteCode() != null ? InviteCodeEntityValue.of(domain.getInviteCode().getValue()) : null
                )
                .startLoveDate(domain.getStartLoveDate())
                .oauthToken(domain.getOauthToken())
                .coupleEntityId(domain.getCoupleId() != null ? CoupleEntityId.of(domain.getCoupleId().getValue()) : null)
                .relationshipStatus(domain.getRelationshipStatus())
                .mbti(domain.getMbti())
                .partnerMbti(domain.getPartnerMbti())
                .partnerLoveTypeCategory(domain.getPartnerLoveTypeCategory())
                .createdAt(domain.getCreatedAt())
                .modifiedAt(domain.getModifiedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
