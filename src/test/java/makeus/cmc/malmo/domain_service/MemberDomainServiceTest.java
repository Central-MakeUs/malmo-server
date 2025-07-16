package makeus.cmc.malmo.domain_service;

import makeus.cmc.malmo.application.port.out.LoadMemberPort;
import makeus.cmc.malmo.domain.exception.MemberNotFoundException;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.Provider;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.service.MemberDomainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberDomainService 단위 테스트")
class MemberDomainServiceTest {

    @Mock
    private LoadMemberPort loadMemberPort;

    @InjectMocks
    private MemberDomainService memberDomainService;

    @Nested
    @DisplayName("멤버 ID로 멤버 조회 기능")
    class GetMemberByIdFeature {

        @Test
        @DisplayName("성공: 유효한 멤버 ID로 멤버를 조회한다")
        void givenValidMemberId_whenGetMemberById_thenReturnMember() {
            // Given
            MemberId memberId = MemberId.of(1L);
            Member member = mock(Member.class);

            given(loadMemberPort.loadMemberById(1L)).willReturn(Optional.of(member));

            // When
            Member result = memberDomainService.getMemberById(memberId);

            // Then
            assertThat(result).isEqualTo(member);
            then(loadMemberPort).should().loadMemberById(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 멤버 ID로 조회 시 MemberNotFoundException이 발생한다")
        void givenNonExistentMemberId_whenGetMemberById_thenThrowMemberNotFoundException() {
            // Given
            MemberId memberId = MemberId.of(999L);

            given(loadMemberPort.loadMemberById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> memberDomainService.getMemberById(memberId))
                    .isInstanceOf(MemberNotFoundException.class);

            then(loadMemberPort).should().loadMemberById(999L);
        }

        @Test
        @DisplayName("경계값: 최소 멤버 ID로 멤버를 조회한다")
        void givenMinMemberId_whenGetMemberById_thenReturnMember() {
            // Given
            MemberId memberId = MemberId.of(1L);
            Member member = mock(Member.class);

            given(loadMemberPort.loadMemberById(1L)).willReturn(Optional.of(member));

            // When
            Member result = memberDomainService.getMemberById(memberId);

            // Then
            assertThat(result).isEqualTo(member);
            then(loadMemberPort).should().loadMemberById(1L);
        }

        @Test
        @DisplayName("경계값: 최대 멤버 ID로 멤버를 조회한다")
        void givenMaxMemberId_whenGetMemberById_thenReturnMember() {
            // Given
            MemberId memberId = MemberId.of(Long.MAX_VALUE);
            Member member = mock(Member.class);

            given(loadMemberPort.loadMemberById(Long.MAX_VALUE)).willReturn(Optional.of(member));

            // When
            Member result = memberDomainService.getMemberById(memberId);

            // Then
            assertThat(result).isEqualTo(member);
            then(loadMemberPort).should().loadMemberById(Long.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("멤버 생성 기능")
    class CreateMemberFeature {

        @Test
        @DisplayName("성공: 카카오 프로바이더로 멤버를 생성한다")
        void givenKakaoProvider_whenCreateMember_thenReturnMember() {
            // Given
            Provider provider = Provider.KAKAO;
            String providerId = "kakao123";
            String email = "test@kakao.com";
            InviteCodeValue inviteCode = InviteCodeValue.of("INVITE123");
            Member expectedMember = mock(Member.class);

            try (MockedStatic<Member> memberMock = mockStatic(Member.class)) {
                memberMock.when(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                )).thenReturn(expectedMember);

                // When
                Member result = memberDomainService.createMember(provider, providerId, email, inviteCode);

                // Then
                assertThat(result).isEqualTo(expectedMember);
                memberMock.verify(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                ));
            }
        }

        @Test
        @DisplayName("성공: 애플 프로바이더로 멤버를 생성한다")
        void givenAppleProvider_whenCreateMember_thenReturnMember() {
            // Given
            Provider provider = Provider.APPLE;
            String providerId = "apple123";
            String email = "test@apple.com";
            InviteCodeValue inviteCode = InviteCodeValue.of("INVITE456");
            Member expectedMember = mock(Member.class);

            try (MockedStatic<Member> memberMock = mockStatic(Member.class)) {
                memberMock.when(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                )).thenReturn(expectedMember);

                // When
                Member result = memberDomainService.createMember(provider, providerId, email, inviteCode);

                // Then
                assertThat(result).isEqualTo(expectedMember);
                memberMock.verify(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                ));
            }
        }

        @Test
        @DisplayName("경계값: null 이메일로 멤버를 생성한다")
        void givenNullEmail_whenCreateMember_thenReturnMember() {
            // Given
            Provider provider = Provider.KAKAO;
            String providerId = "kakao123";
            String email = null;
            InviteCodeValue inviteCode = InviteCodeValue.of("INVITE789");
            Member expectedMember = mock(Member.class);

            try (MockedStatic<Member> memberMock = mockStatic(Member.class)) {
                memberMock.when(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                )).thenReturn(expectedMember);

                // When
                Member result = memberDomainService.createMember(provider, providerId, email, inviteCode);

                // Then
                assertThat(result).isEqualTo(expectedMember);
                memberMock.verify(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                ));
            }
        }

        @Test
        @DisplayName("경계값: 빈 문자열 이메일로 멤버를 생성한다")
        void givenEmptyEmail_whenCreateMember_thenReturnMember() {
            // Given
            Provider provider = Provider.APPLE;
            String providerId = "apple123";
            String email = "";
            InviteCodeValue inviteCode = InviteCodeValue.of("INVITE000");
            Member expectedMember = mock(Member.class);

            try (MockedStatic<Member> memberMock = mockStatic(Member.class)) {
                memberMock.when(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                )).thenReturn(expectedMember);

                // When
                Member result = memberDomainService.createMember(provider, providerId, email, inviteCode);

                // Then
                assertThat(result).isEqualTo(expectedMember);
                memberMock.verify(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                ));
            }
        }

        @Test
        @DisplayName("경계값: 빈 문자열 프로바이더 ID로 멤버를 생성한다")
        void givenEmptyProviderId_whenCreateMember_thenReturnMember() {
            // Given
            Provider provider = Provider.KAKAO;
            String providerId = "";
            String email = "test@example.com";
            InviteCodeValue inviteCode = InviteCodeValue.of("INVITE111");
            Member expectedMember = mock(Member.class);

            try (MockedStatic<Member> memberMock = mockStatic(Member.class)) {
                memberMock.when(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                )).thenReturn(expectedMember);

                // When
                Member result = memberDomainService.createMember(provider, providerId, email, inviteCode);

                // Then
                assertThat(result).isEqualTo(expectedMember);
                memberMock.verify(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                ));
            }
        }

        @Test
        @DisplayName("경계값: 매우 긴 이메일로 멤버를 생성한다")
        void givenVeryLongEmail_whenCreateMember_thenReturnMember() {
            // Given
            Provider provider = Provider.APPLE;
            String providerId = "apple123";
            String email = "a".repeat(100) + "@example.com";
            InviteCodeValue inviteCode = InviteCodeValue.of("INVITE222");
            Member expectedMember = mock(Member.class);

            try (MockedStatic<Member> memberMock = mockStatic(Member.class)) {
                memberMock.when(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                )).thenReturn(expectedMember);

                // When
                Member result = memberDomainService.createMember(provider, providerId, email, inviteCode);

                // Then
                assertThat(result).isEqualTo(expectedMember);
                memberMock.verify(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                ));
            }
        }

        @Test
        @DisplayName("경계값: 매우 긴 프로바이더 ID로 멤버를 생성한다")
        void givenVeryLongProviderId_whenCreateMember_thenReturnMember() {
            // Given
            Provider provider = Provider.KAKAO;
            String providerId = "a".repeat(1000);
            String email = "test@example.com";
            InviteCodeValue inviteCode = InviteCodeValue.of("INVITE333");
            Member expectedMember = mock(Member.class);

            try (MockedStatic<Member> memberMock = mockStatic(Member.class)) {
                memberMock.when(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                )).thenReturn(expectedMember);

                // When
                Member result = memberDomainService.createMember(provider, providerId, email, inviteCode);

                // Then
                assertThat(result).isEqualTo(expectedMember);
                memberMock.verify(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                ));
            }
        }

        @Test
        @DisplayName("경계값: 특수문자가 포함된 이메일로 멤버를 생성한다")
        void givenSpecialCharacterEmail_whenCreateMember_thenReturnMember() {
            // Given
            Provider provider = Provider.APPLE;
            String providerId = "apple123";
            String email = "test+tag@sub.example.com";
            InviteCodeValue inviteCode = InviteCodeValue.of("INVITE444");
            Member expectedMember = mock(Member.class);

            try (MockedStatic<Member> memberMock = mockStatic(Member.class)) {
                memberMock.when(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                )).thenReturn(expectedMember);

                // When
                Member result = memberDomainService.createMember(provider, providerId, email, inviteCode);

                // Then
                assertThat(result).isEqualTo(expectedMember);
                memberMock.verify(() -> Member.createMember(
                        provider,
                        providerId,
                        MemberRole.MEMBER,
                        MemberState.BEFORE_ONBOARDING,
                        email,
                        inviteCode
                ));
            }
        }
    }
}