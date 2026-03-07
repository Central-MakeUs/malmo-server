package makeus.cmc.malmo.service;

import makeus.cmc.malmo.adaptor.out.oidc.AppleNotificationValidator;
import makeus.cmc.malmo.adaptor.out.oidc.AppleNotificationValidator.AppleNotificationClaims;
import makeus.cmc.malmo.adaptor.out.redis.AppleNotificationJtiStore;
import makeus.cmc.malmo.application.helper.member.MemberCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.port.out.member.UnlinkApplePort;
import makeus.cmc.malmo.application.service.member.AppleNotificationService;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.EmailForwardingStatus;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppleNotificationService 단위 테스트")
class AppleNotificationServiceTest {

    @Mock
    private AppleNotificationValidator notificationValidator;

    @Mock
    private AppleNotificationJtiStore jtiStore;

    @Mock
    private MemberQueryHelper memberQueryHelper;

    @Mock
    private MemberCommandHelper memberCommandHelper;

    @Mock
    private UnlinkApplePort unlinkApplePort;

    @Captor
    private ArgumentCaptor<Member> memberCaptor;

    private AppleNotificationService appleNotificationService;

    private static final String SIGNED_PAYLOAD = "test-signed-payload";
    private static final String JTI = "test-jti-12345";
    private static final String SUB = "000123.abcdef1234567890.1234";

    @BeforeEach
    void setUp() {
        appleNotificationService = new AppleNotificationService(
                notificationValidator,
                jtiStore,
                memberQueryHelper,
                memberCommandHelper,
                unlinkApplePort
        );
    }

    private Member createTestMember(String providerId, String oauthToken) {
        return Member.from(
                1L,
                Provider.APPLE,
                providerId,
                MemberRole.MEMBER,
                MemberState.ALIVE,
                true,
                "firebase-token",
                "refresh-token",
                null,
                0.0f,
                0.0f,
                "nickname",
                "test@email.com",
                null, // emailForwardingStatus
                InviteCodeValue.of("INVITE123"),
                null,
                oauthToken,
                null,
                null, // relationshipStatus
                null, // personalityType
                null, // otherPersonalityType
                null,
                null,
                null
        );
    }

    private AppleNotificationClaims createClaims(String eventType, String sub, String jti) {
        return createClaims(eventType, sub, jti, null);
    }

    private AppleNotificationClaims createClaims(String eventType, String sub, String jti, String email) {
        return AppleNotificationClaims.builder()
                .jti(jti)
                .iat(System.currentTimeMillis())
                .eventType(eventType)
                .sub(sub)
                .eventTime(System.currentTimeMillis() / 1000)
                .email(email)
                .build();
    }

    @Nested
    @DisplayName("중복 jti 처리 테스트")
    class DuplicateJtiTest {

        @Test
        @DisplayName("중복 jti인 경우 회원 조회 없이 종료한다")
        void 중복_jti인_경우_회원_조회_없이_종료한다() {
            // given
            AppleNotificationClaims claims = createClaims("consent-revoked", SUB, JTI);
            given(notificationValidator.validateAndParse(SIGNED_PAYLOAD)).willReturn(claims);
            given(jtiStore.tryMarkAsProcessed(JTI)).willReturn(false);

            // when
            appleNotificationService.processNotification(SIGNED_PAYLOAD);

            // then
            verify(memberQueryHelper, never()).getMemberByProviderId(any(), any());
            verify(memberCommandHelper, never()).saveMember(any());
        }
    }

    @Nested
    @DisplayName("회원 미존재 처리 테스트")
    class MemberNotFoundTest {

        @Test
        @DisplayName("회원이 존재하지 않으면 저장 없이 종료한다")
        void 회원이_존재하지_않으면_저장_없이_종료한다() {
            // given
            AppleNotificationClaims claims = createClaims("consent-revoked", SUB, JTI);
            given(notificationValidator.validateAndParse(SIGNED_PAYLOAD)).willReturn(claims);
            given(jtiStore.tryMarkAsProcessed(JTI)).willReturn(true);
            given(memberQueryHelper.getMemberByProviderId(Provider.APPLE, SUB)).willReturn(Optional.empty());

            // when
            appleNotificationService.processNotification(SIGNED_PAYLOAD);

            // then
            verify(memberCommandHelper, never()).saveMember(any());
        }
    }

    @Nested
    @DisplayName("consent-revoked 이벤트 처리 테스트")
    class ConsentRevokedTest {

        @Test
        @DisplayName("consent-revoked 이벤트 수신 시 회원을 소프트 삭제한다")
        void consent_revoked_이벤트_수신_시_회원을_소프트_삭제한다() {
            // given
            Member member = createTestMember(SUB, null);
            AppleNotificationClaims claims = createClaims("consent-revoked", SUB, JTI);

            given(notificationValidator.validateAndParse(SIGNED_PAYLOAD)).willReturn(claims);
            given(jtiStore.tryMarkAsProcessed(JTI)).willReturn(true);
            given(memberQueryHelper.getMemberByProviderId(Provider.APPLE, SUB)).willReturn(Optional.of(member));
            given(memberCommandHelper.saveMember(any())).willReturn(member);

            // when
            appleNotificationService.processNotification(SIGNED_PAYLOAD);

            // then
            verify(memberCommandHelper).saveMember(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getMemberState()).isEqualTo(MemberState.DELETED);
            assertThat(savedMember.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("consent-revoked 이벤트 수신 시 oauthToken이 있으면 Apple revoke를 호출한다")
        void consent_revoked_이벤트_수신_시_oauthToken이_있으면_Apple_revoke를_호출한다() {
            // given
            String oauthToken = "apple-refresh-token";
            Member member = createTestMember(SUB, oauthToken);
            AppleNotificationClaims claims = createClaims("consent-revoked", SUB, JTI);

            given(notificationValidator.validateAndParse(SIGNED_PAYLOAD)).willReturn(claims);
            given(jtiStore.tryMarkAsProcessed(JTI)).willReturn(true);
            given(memberQueryHelper.getMemberByProviderId(Provider.APPLE, SUB)).willReturn(Optional.of(member));
            given(memberCommandHelper.saveMember(any())).willReturn(member);
            doNothing().when(unlinkApplePort).unlink(oauthToken);

            // when
            appleNotificationService.processNotification(SIGNED_PAYLOAD);

            // then
            verify(unlinkApplePort).unlink(oauthToken);
        }

        @Test
        @DisplayName("consent-revoked 이벤트 수신 시 oauthToken이 없으면 Apple revoke를 호출하지 않는다")
        void consent_revoked_이벤트_수신_시_oauthToken이_없으면_Apple_revoke를_호출하지_않는다() {
            // given
            Member member = createTestMember(SUB, null);
            AppleNotificationClaims claims = createClaims("consent-revoked", SUB, JTI);

            given(notificationValidator.validateAndParse(SIGNED_PAYLOAD)).willReturn(claims);
            given(jtiStore.tryMarkAsProcessed(JTI)).willReturn(true);
            given(memberQueryHelper.getMemberByProviderId(Provider.APPLE, SUB)).willReturn(Optional.of(member));
            given(memberCommandHelper.saveMember(any())).willReturn(member);

            // when
            appleNotificationService.processNotification(SIGNED_PAYLOAD);

            // then
            verify(unlinkApplePort, never()).unlink(any());
        }
    }

    @Nested
    @DisplayName("account-delete 이벤트 처리 테스트")
    class AccountDeleteTest {

        @Test
        @DisplayName("account-delete 이벤트 수신 시 회원을 소프트 삭제한다")
        void account_delete_이벤트_수신_시_회원을_소프트_삭제한다() {
            // given
            Member member = createTestMember(SUB, null);
            AppleNotificationClaims claims = createClaims("account-delete", SUB, JTI);

            given(notificationValidator.validateAndParse(SIGNED_PAYLOAD)).willReturn(claims);
            given(jtiStore.tryMarkAsProcessed(JTI)).willReturn(true);
            given(memberQueryHelper.getMemberByProviderId(Provider.APPLE, SUB)).willReturn(Optional.of(member));
            given(memberCommandHelper.saveMember(any())).willReturn(member);

            // when
            appleNotificationService.processNotification(SIGNED_PAYLOAD);

            // then
            verify(memberCommandHelper).saveMember(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getMemberState()).isEqualTo(MemberState.DELETED);
        }
    }

    @Nested
    @DisplayName("email 이벤트 처리 테스트")
    class EmailEventTest {

        @Test
        @DisplayName("email-enabled 이벤트 수신 시 새 이메일로 업데이트하고 상태를 ENABLED로 변경한다")
        void email_enabled_이벤트_수신_시_새_이메일로_업데이트하고_상태를_ENABLED로_변경한다() {
            // given
            String newEmail = "newemail@privaterelay.appleid.com";
            Member member = createTestMember(SUB, null);
            AppleNotificationClaims claims = createClaims("email-enabled", SUB, JTI, newEmail);

            given(notificationValidator.validateAndParse(SIGNED_PAYLOAD)).willReturn(claims);
            given(jtiStore.tryMarkAsProcessed(JTI)).willReturn(true);
            given(memberQueryHelper.getMemberByProviderId(Provider.APPLE, SUB)).willReturn(Optional.of(member));
            given(memberCommandHelper.saveMember(any())).willReturn(member);

            // when
            appleNotificationService.processNotification(SIGNED_PAYLOAD);

            // then - 이메일 업데이트 및 상태 변경 후 저장
            verify(memberCommandHelper).saveMember(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getEmail()).isEqualTo(newEmail);
            assertThat(savedMember.getEmailForwardingStatus()).isEqualTo(EmailForwardingStatus.ENABLED);
            assertThat(savedMember.getMemberState()).isEqualTo(MemberState.ALIVE); // 삭제되지 않음
        }

        @Test
        @DisplayName("email-enabled 이벤트 수신 시 이메일이 없어도 상태는 ENABLED로 변경한다")
        void email_enabled_이벤트_수신_시_이메일이_없어도_상태는_ENABLED로_변경한다() {
            // given
            Member member = createTestMember(SUB, null);
            AppleNotificationClaims claims = createClaims("email-enabled", SUB, JTI, null);

            given(notificationValidator.validateAndParse(SIGNED_PAYLOAD)).willReturn(claims);
            given(jtiStore.tryMarkAsProcessed(JTI)).willReturn(true);
            given(memberQueryHelper.getMemberByProviderId(Provider.APPLE, SUB)).willReturn(Optional.of(member));
            given(memberCommandHelper.saveMember(any())).willReturn(member);

            // when
            appleNotificationService.processNotification(SIGNED_PAYLOAD);

            // then - 상태는 변경됨
            verify(memberCommandHelper).saveMember(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getEmailForwardingStatus()).isEqualTo(EmailForwardingStatus.ENABLED);
        }

        @Test
        @DisplayName("email-disabled 이벤트 수신 시 상태를 DISABLED로 변경한다")
        void email_disabled_이벤트_수신_시_상태를_DISABLED로_변경한다() {
            // given
            Member member = createTestMember(SUB, null);
            AppleNotificationClaims claims = createClaims("email-disabled", SUB, JTI);

            given(notificationValidator.validateAndParse(SIGNED_PAYLOAD)).willReturn(claims);
            given(jtiStore.tryMarkAsProcessed(JTI)).willReturn(true);
            given(memberQueryHelper.getMemberByProviderId(Provider.APPLE, SUB)).willReturn(Optional.of(member));
            given(memberCommandHelper.saveMember(any())).willReturn(member);

            // when
            appleNotificationService.processNotification(SIGNED_PAYLOAD);

            // then - 상태 변경 확인
            verify(memberCommandHelper).saveMember(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getEmailForwardingStatus()).isEqualTo(EmailForwardingStatus.DISABLED);
            assertThat(savedMember.getMemberState()).isEqualTo(MemberState.ALIVE); // 삭제되지 않음
        }
    }

    @Nested
    @DisplayName("토큰/세션 무효화 테스트")
    class TokenInvalidationTest {

        @Test
        @DisplayName("consent-revoked 이벤트 수신 시 refreshToken과 firebaseToken을 무효화한다")
        void consent_revoked_이벤트_수신_시_토큰을_무효화한다() {
            // given
            Member member = createTestMember(SUB, null);
            AppleNotificationClaims claims = createClaims("consent-revoked", SUB, JTI);

            given(notificationValidator.validateAndParse(SIGNED_PAYLOAD)).willReturn(claims);
            given(jtiStore.tryMarkAsProcessed(JTI)).willReturn(true);
            given(memberQueryHelper.getMemberByProviderId(Provider.APPLE, SUB)).willReturn(Optional.of(member));
            given(memberCommandHelper.saveMember(any())).willReturn(member);

            // when
            appleNotificationService.processNotification(SIGNED_PAYLOAD);

            // then
            verify(memberCommandHelper).saveMember(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getRefreshToken()).isNull();
            assertThat(savedMember.getFirebaseToken()).isNull();
        }
    }
}

