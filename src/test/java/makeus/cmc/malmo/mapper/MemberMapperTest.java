package makeus.cmc.malmo.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.CoupleEntityId;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.MemberMapper;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.CoupleId;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.PartnerLoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.Provider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberMapper 테스트")
class MemberMapperTest {

    @InjectMocks
    private MemberMapper memberMapper;

    @Nested
    @DisplayName("Entity를 Domain으로 변환할 때")
    class ToDomainTest {

        @Test
        @DisplayName("모든 필드가 있는 MemberEntity를 Member로 변환한다")
        void toDomain() {
            // given
            MemberEntity entity = createCompleteEntity();

            // when
            Member domain = memberMapper.toDomain(entity);

            // then
            assertMember(domain, entity);
        }
    }

    @Nested
    @DisplayName("Domain을 Entity로 변환할 때")
    class ToEntityTest {

        @Test
        @DisplayName("모든 필드가 있는 Member를 MemberEntity로 변환한다")
        void toEntity() {
            // given
            Member domain = createCompleteMember();

            // when
            MemberEntity entity = memberMapper.toEntity(domain);

            // then
            assertMemberEntity(entity, domain);
        }
    }

    private void assertMember(Member domain, MemberEntity entity) {
        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getProvider()).isEqualTo(entity.getProvider());
        assertThat(domain.getProviderId()).isEqualTo(entity.getProviderId());
        assertThat(domain.getMemberRole()).isEqualTo(entity.getMemberRole());
        assertThat(domain.getMemberState()).isEqualTo(entity.getMemberState());
        assertThat(domain.isAlarmOn()).isEqualTo(entity.isAlarmOn());
        assertThat(domain.getFirebaseToken()).isEqualTo(entity.getFirebaseToken());
        assertThat(domain.getRefreshToken()).isEqualTo(entity.getRefreshToken());
        assertThat(domain.getLoveTypeCategory()).isEqualTo(entity.getLoveTypeCategory());
        assertThat(domain.getAvoidanceRate()).isEqualTo(entity.getAvoidanceRate());
        assertThat(domain.getAnxietyRate()).isEqualTo(entity.getAnxietyRate());
        assertThat(domain.getNickname()).isEqualTo(entity.getNickname());
        assertThat(domain.getEmail()).isEqualTo(entity.getEmail());
        assertThat(domain.getInviteCode().getValue()).isEqualTo(entity.getInviteCodeEntityValue().getValue());
        assertThat(domain.getStartLoveDate()).isEqualTo(entity.getStartLoveDate());
        assertThat(domain.getOauthToken()).isEqualTo(entity.getOauthToken());
        assertThat(domain.getCoupleId().getValue()).isEqualTo(entity.getCoupleEntityId().getValue());
        assertThat(domain.getMbti()).isEqualTo(entity.getMbti());
        assertThat(domain.getPartnerMbti()).isEqualTo(entity.getPartnerMbti());
        assertThat(domain.getPartnerLoveTypeCategory()).isEqualTo(entity.getPartnerLoveTypeCategory());
        assertThat(domain.getCreatedAt()).isEqualTo(entity.getCreatedAt());
        assertThat(domain.getModifiedAt()).isEqualTo(entity.getModifiedAt());
        assertThat(domain.getDeletedAt()).isEqualTo(entity.getDeletedAt());
    }

    private void assertMemberEntity(MemberEntity entity, Member domain) {
        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getProvider()).isEqualTo(domain.getProvider());
        assertThat(entity.getProviderId()).isEqualTo(domain.getProviderId());
        assertThat(entity.getMemberRole()).isEqualTo(domain.getMemberRole());
        assertThat(entity.getMemberState()).isEqualTo(domain.getMemberState());
        assertThat(entity.isAlarmOn()).isEqualTo(domain.isAlarmOn());
        assertThat(entity.getFirebaseToken()).isEqualTo(domain.getFirebaseToken());
        assertThat(entity.getRefreshToken()).isEqualTo(domain.getRefreshToken());
        assertThat(entity.getLoveTypeCategory()).isEqualTo(domain.getLoveTypeCategory());
        assertThat(entity.getAvoidanceRate()).isEqualTo(domain.getAvoidanceRate());
        assertThat(entity.getAnxietyRate()).isEqualTo(domain.getAnxietyRate());
        assertThat(entity.getNickname()).isEqualTo(domain.getNickname());
        assertThat(entity.getEmail()).isEqualTo(domain.getEmail());
        assertThat(entity.getInviteCodeEntityValue().getValue()).isEqualTo(domain.getInviteCode().getValue());
        assertThat(entity.getStartLoveDate()).isEqualTo(domain.getStartLoveDate());
        assertThat(entity.getOauthToken()).isEqualTo(domain.getOauthToken());
        assertThat(entity.getCoupleEntityId().getValue()).isEqualTo(domain.getCoupleId().getValue());
        assertThat(entity.getMbti()).isEqualTo(domain.getMbti());
        assertThat(entity.getPartnerMbti()).isEqualTo(domain.getPartnerMbti());
        assertThat(entity.getPartnerLoveTypeCategory()).isEqualTo(domain.getPartnerLoveTypeCategory());
        assertThat(entity.getCreatedAt()).isEqualTo(domain.getCreatedAt());
        assertThat(entity.getModifiedAt()).isEqualTo(domain.getModifiedAt());
        assertThat(entity.getDeletedAt()).isEqualTo(domain.getDeletedAt());
    }

    private MemberEntity createCompleteEntity() {
        LocalDateTime now = LocalDateTime.now();
        return MemberEntity.builder()
                .id(1L)
                .provider(Provider.KAKAO)
                .providerId("provider123")
                .memberRole(MemberRole.MEMBER)
                .memberState(MemberState.ALIVE)
                .isAlarmOn(true)
                .firebaseToken("firebase_token")
                .refreshToken("refresh_token")
                .loveTypeCategory(LoveTypeCategory.STABLE_TYPE)
                .avoidanceRate(0.5f)
                .anxietyRate(0.3f)
                .nickname("testuser")
                .email("test@example.com")
                .inviteCodeEntityValue(InviteCodeEntityValue.of("invite_code"))
                .startLoveDate(LocalDate.now())
                .oauthToken("oauth_token")
                .coupleEntityId(CoupleEntityId.of(100L))
                .mbti("INTJ")
                .partnerMbti("ENFP")
                .partnerLoveTypeCategory(PartnerLoveTypeCategory.UNKNOWN)
                .createdAt(now)
                .modifiedAt(now)
                .deletedAt(null)
                .build();
    }

    private Member createCompleteMember() {
        LocalDateTime now = LocalDateTime.now();
        return Member.from(
                1L,
                Provider.KAKAO,
                "provider123",
                MemberRole.MEMBER,
                MemberState.ALIVE,
                true,
                "firebase_token",
                "refresh_token",
                LoveTypeCategory.STABLE_TYPE,
                0.5f,
                0.3f,
                "testuser",
                "test@example.com",
                null, // emailForwardingStatus
                InviteCodeValue.of("invite_code"),
                LocalDate.now(),
                "oauth_token",
                CoupleId.of(100L),
                null, // relationshipStatus
                "INTJ",
                "ENFP",
                PartnerLoveTypeCategory.UNKNOWN,
                now,
                now,
                null
        );
    }
}
