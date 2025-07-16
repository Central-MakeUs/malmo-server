package makeus.cmc.malmo.service;

import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.application.port.in.SignInUseCase;
import makeus.cmc.malmo.application.port.out.*;
import makeus.cmc.malmo.application.service.SignInService;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.service.InviteCodeDomainService;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.Provider;
import makeus.cmc.malmo.domain.service.MemberDomainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignInService 단위 테스트")
class SignInServiceTest {

    @Mock
    private MemberDomainService memberDomainService;

    @Mock
    private LoadMemberPort loadMemberPort;

    @Mock
    private SaveMemberPort saveMemberPort;

    @Mock
    private GenerateTokenPort generateTokenPort;

    @Mock
    private kakaoIdTokenPort kakaoIdTokenPort;

    @Mock
    private AppleIdTokenPort appleIdTokenPort;

    @Mock
    private FetchFromOAuthProviderPort fetchFromOAuthProviderPort;

    @Mock
    private InviteCodeDomainService inviteCodeDomainService;

    @InjectMocks
    private SignInService signInService;

    @Nested
    @DisplayName("카카오 로그인 기능")
    class KakaoSignInFeature {

        @Test
        @DisplayName("성공: 기존 카카오 사용자 로그인이 성공한다")
        void givenExistingKakaoUser_whenSignInKakao_thenReturnSignInResponse() {
            // Given
            String idToken = "kakaoIdToken123";
            String accessToken = "kakaoAccessToken123";
            String providerId = "kakaoProviderId123";
            Long memberId = 1L;
            String grantType = "Bearer";
            String newAccessToken = "newAccessToken123";
            String newRefreshToken = "newRefreshToken123";

            SignInUseCase.SignInKakaoCommand command = SignInUseCase.SignInKakaoCommand.builder()
                    .idToken(idToken)
                    .accessToken(accessToken)
                    .build();

            Member existingMember = mock(Member.class);
            given(existingMember.getId()).willReturn(memberId);
            given(existingMember.getMemberRole()).willReturn(MemberRole.MEMBER);
            given(existingMember.getMemberState()).willReturn(MemberState.ALIVE);

            TokenInfo tokenInfo = mock(TokenInfo.class);
            given(tokenInfo.getGrantType()).willReturn(grantType);
            given(tokenInfo.getAccessToken()).willReturn(newAccessToken);
            given(tokenInfo.getRefreshToken()).willReturn(newRefreshToken);

            given(kakaoIdTokenPort.validateToken(idToken)).willReturn(providerId);
            given(loadMemberPort.loadMemberByProviderId(Provider.KAKAO, providerId))
                    .willReturn(Optional.of(existingMember));
            given(generateTokenPort.generateToken(memberId, MemberRole.MEMBER)).willReturn(tokenInfo);
            given(saveMemberPort.saveMember(existingMember)).willReturn(existingMember);

            // When
            SignInUseCase.SignInResponse response = signInService.signInKakao(command);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMemberState()).isEqualTo(MemberState.ALIVE.name());
            assertThat(response.getGrantType()).isEqualTo(grantType);
            assertThat(response.getAccessToken()).isEqualTo(newAccessToken);
            assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);

            then(kakaoIdTokenPort).should().validateToken(idToken);
            then(loadMemberPort).should().loadMemberByProviderId(Provider.KAKAO, providerId);
            then(generateTokenPort).should().generateToken(memberId, MemberRole.MEMBER);
            then(existingMember).should().refreshMemberToken(newRefreshToken);
            then(saveMemberPort).should().saveMember(existingMember);
        }

        @Test
        @DisplayName("성공: 새로운 카카오 사용자 자동 회원가입 후 로그인이 성공한다")
        void givenNewKakaoUser_whenSignInKakao_thenCreateNewMemberAndReturnSignInResponse() {
            // Given
            String idToken = "kakaoIdToken123";
            String accessToken = "kakaoAccessToken123";
            String providerId = "kakaoProviderId123";
            String email = "test@example.com";
            Long newMemberId = 2L;
            String grantType = "Bearer";
            String newAccessToken = "newAccessToken123";
            String newRefreshToken = "newRefreshToken123";
            String inviteCode = "INVITE123";

            SignInUseCase.SignInKakaoCommand command = SignInUseCase.SignInKakaoCommand.builder()
                    .idToken(idToken)
                    .accessToken(accessToken)
                    .build();

            Member newMember = mock(Member.class);
            given(newMember.getId()).willReturn(newMemberId);
            given(newMember.getMemberRole()).willReturn(MemberRole.MEMBER);
            given(newMember.getMemberState()).willReturn(MemberState.BEFORE_ONBOARDING);

            TokenInfo tokenInfo = mock(TokenInfo.class);
            given(tokenInfo.getGrantType()).willReturn(grantType);
            given(tokenInfo.getAccessToken()).willReturn(newAccessToken);
            given(tokenInfo.getRefreshToken()).willReturn(newRefreshToken);

            given(kakaoIdTokenPort.validateToken(idToken)).willReturn(providerId);
            given(loadMemberPort.loadMemberByProviderId(Provider.KAKAO, providerId))
                    .willReturn(Optional.empty());
            given(fetchFromOAuthProviderPort.fetchMemberEmailFromKakao(accessToken)).willReturn(email);
            given(memberDomainService.createMember(eq(Provider.KAKAO), eq(providerId), eq(email), any(InviteCodeValue.class))).willReturn(newMember);
            given(saveMemberPort.saveMember(newMember)).willReturn(newMember);
            given(generateTokenPort.generateToken(newMemberId, MemberRole.MEMBER)).willReturn(tokenInfo);
            given(inviteCodeDomainService.generateUniqueInviteCode()).willReturn(InviteCodeValue.of(inviteCode));

            // When
            SignInUseCase.SignInResponse response = signInService.signInKakao(command);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMemberState()).isEqualTo(MemberState.BEFORE_ONBOARDING.name());
            assertThat(response.getGrantType()).isEqualTo(grantType);
            assertThat(response.getAccessToken()).isEqualTo(newAccessToken);
            assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);

            then(kakaoIdTokenPort).should().validateToken(idToken);
            then(loadMemberPort).should().loadMemberByProviderId(Provider.KAKAO, providerId);
            then(fetchFromOAuthProviderPort).should().fetchMemberEmailFromKakao(accessToken);
            then(memberDomainService).should().createMember(eq(Provider.KAKAO), eq(providerId), eq(email), any(InviteCodeValue.class));
            then(saveMemberPort).should(times(2)).saveMember(newMember);
            then(generateTokenPort).should().generateToken(newMemberId, MemberRole.MEMBER);
            then(inviteCodeDomainService).should().generateUniqueInviteCode();
            then(newMember).should().refreshMemberToken(newRefreshToken);
        }

    }

    @Nested
    @DisplayName("애플 로그인 기능")
    class AppleSignInFeature {

        @Test
        @DisplayName("성공: 기존 애플 사용자 로그인이 성공한다")
        void givenExistingAppleUser_whenSignInApple_thenReturnSignInResponse() {
            // Given
            String idToken = "appleIdToken123";
            String providerId = "appleProviderId123";
            Long memberId = 1L;
            String grantType = "Bearer";
            String newAccessToken = "newAccessToken123";
            String newRefreshToken = "newRefreshToken123";

            SignInUseCase.SignInAppleCommand command = SignInUseCase.SignInAppleCommand.builder()
                    .idToken(idToken)
                    .build();

            Member existingMember = mock(Member.class);
            given(existingMember.getId()).willReturn(memberId);
            given(existingMember.getMemberRole()).willReturn(MemberRole.MEMBER);
            given(existingMember.getMemberState()).willReturn(MemberState.ALIVE);

            TokenInfo tokenInfo = mock(TokenInfo.class);
            given(tokenInfo.getGrantType()).willReturn(grantType);
            given(tokenInfo.getAccessToken()).willReturn(newAccessToken);
            given(tokenInfo.getRefreshToken()).willReturn(newRefreshToken);

            given(appleIdTokenPort.validateToken(idToken)).willReturn(providerId);
            given(loadMemberPort.loadMemberByProviderId(Provider.APPLE, providerId))
                    .willReturn(Optional.of(existingMember));
            given(generateTokenPort.generateToken(memberId, MemberRole.MEMBER)).willReturn(tokenInfo);
            given(saveMemberPort.saveMember(existingMember)).willReturn(existingMember);

            // When
            SignInUseCase.SignInResponse response = signInService.signInApple(command);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMemberState()).isEqualTo(MemberState.ALIVE.name());
            assertThat(response.getGrantType()).isEqualTo(grantType);
            assertThat(response.getAccessToken()).isEqualTo(newAccessToken);
            assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);

            then(appleIdTokenPort).should().validateToken(idToken);
            then(loadMemberPort).should().loadMemberByProviderId(Provider.APPLE, providerId);
            then(generateTokenPort).should().generateToken(memberId, MemberRole.MEMBER);
            then(existingMember).should().refreshMemberToken(newRefreshToken);
            then(saveMemberPort).should().saveMember(existingMember);
        }

        @Test
        @DisplayName("성공: 새로운 애플 사용자 자동 회원가입 후 로그인이 성공한다")
        void givenNewAppleUser_whenSignInApple_thenCreateNewMemberAndReturnSignInResponse() {
            // Given
            String idToken = "appleIdToken123";
            String providerId = "appleProviderId123";
            String email = "test@apple.com";
            Long newMemberId = 3L;
            String grantType = "Bearer";
            String newAccessToken = "newAccessToken123";
            String newRefreshToken = "newRefreshToken123";
            String inviteCode = "INVITE123";

            SignInUseCase.SignInAppleCommand command = SignInUseCase.SignInAppleCommand.builder()
                    .idToken(idToken)
                    .build();

            Member newMember = mock(Member.class);
            given(newMember.getId()).willReturn(newMemberId);
            given(newMember.getMemberRole()).willReturn(MemberRole.MEMBER);
            given(newMember.getMemberState()).willReturn(MemberState.BEFORE_ONBOARDING);

            TokenInfo tokenInfo = mock(TokenInfo.class);
            given(tokenInfo.getGrantType()).willReturn(grantType);
            given(tokenInfo.getAccessToken()).willReturn(newAccessToken);
            given(tokenInfo.getRefreshToken()).willReturn(newRefreshToken);

            given(appleIdTokenPort.validateToken(idToken)).willReturn(providerId);
            given(loadMemberPort.loadMemberByProviderId(Provider.APPLE, providerId))
                    .willReturn(Optional.empty());
            given(appleIdTokenPort.extractEmailFromIdToken(idToken)).willReturn(email);
            given(memberDomainService.createMember(eq(Provider.APPLE), eq(providerId), eq(email), any(InviteCodeValue.class))).willReturn(newMember);
            given(saveMemberPort.saveMember(newMember)).willReturn(newMember);
            given(generateTokenPort.generateToken(newMemberId, MemberRole.MEMBER)).willReturn(tokenInfo);
            given(inviteCodeDomainService.generateUniqueInviteCode()).willReturn(InviteCodeValue.of(inviteCode));

            // When
            SignInUseCase.SignInResponse response = signInService.signInApple(command);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMemberState()).isEqualTo(MemberState.BEFORE_ONBOARDING.name());
            assertThat(response.getGrantType()).isEqualTo(grantType);
            assertThat(response.getAccessToken()).isEqualTo(newAccessToken);
            assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);

            then(appleIdTokenPort).should().validateToken(idToken);
            then(loadMemberPort).should().loadMemberByProviderId(Provider.APPLE, providerId);
            then(appleIdTokenPort).should().extractEmailFromIdToken(idToken);
            then(memberDomainService).should().createMember(eq(Provider.APPLE), eq(providerId), eq(email), any(InviteCodeValue.class));
            then(saveMemberPort).should(times(2)).saveMember(newMember);
            then(generateTokenPort).should().generateToken(newMemberId, MemberRole.MEMBER);
            then(newMember).should().refreshMemberToken(newRefreshToken);
        }
    }
}