package makeus.cmc.malmo.service;

import makeus.cmc.malmo.application.port.in.SignUpUseCase;
import makeus.cmc.malmo.application.port.out.SaveMemberPort;
import makeus.cmc.malmo.application.service.SignUpService;
import makeus.cmc.malmo.domain.exception.InviteCodeGenerateFailedException;
import makeus.cmc.malmo.domain.exception.MemberNotFoundException;
import makeus.cmc.malmo.domain.exception.TermsNotFoundException;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.service.InviteCodeDomainService;
import makeus.cmc.malmo.domain.service.MemberDomainService;
import makeus.cmc.malmo.domain.service.TermsAgreementDomainService;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignUpService 단위 테스트")
class SignUpServiceTest {

    @Mock
    private MemberDomainService memberDomainService;

    @Mock
    private SaveMemberPort saveMemberPort;

    @Mock
    private TermsAgreementDomainService termsAgreementDomainService;

    @Mock
    private InviteCodeDomainService inviteCodeDomainService;

    @InjectMocks
    private SignUpService signUpService;

    @Nested
    @DisplayName("회원가입 기능")
    class SignUpFeature {

        @Test
        @DisplayName("성공: 유효한 회원정보와 약관동의로 회원가입이 성공한다")
        void givenValidMemberInfoAndTermsAgreement_whenSignUp_thenReturnSignUpResponse() {
            // Given
            Long memberId = 1L;
            String nickname = "테스트닉네임";
            LocalDate loveStartDate = LocalDate.of(2024, 1, 1);
            String expectedInviteCode = "ABC123";

            SignUpUseCase.TermsCommand termsCommand1 = SignUpUseCase.TermsCommand.builder()
                    .termsId(1L)
                    .isAgreed(true)
                    .build();

            SignUpUseCase.TermsCommand termsCommand2 = SignUpUseCase.TermsCommand.builder()
                    .termsId(2L)
                    .isAgreed(true)
                    .build();

            SignUpUseCase.SignUpCommand command = SignUpUseCase.SignUpCommand.builder()
                    .memberId(memberId)
                    .nickname(nickname)
                    .loveStartDate(loveStartDate)
                    .terms(Arrays.asList(termsCommand1, termsCommand2))
                    .build();

            Member member = mock(Member.class);
            InviteCodeValue inviteCodeValue = InviteCodeValue.of(expectedInviteCode);

            given(member.getId()).willReturn(memberId);
            given(memberDomainService.getMemberById(MemberId.of(memberId))).willReturn(member);
            given(saveMemberPort.saveMember(member)).willReturn(member);
            given(inviteCodeDomainService.generateUniqueInviteCode()).willReturn(inviteCodeValue);

            // When
            SignUpUseCase.SignUpResponse response = signUpService.signUp(command);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getCoupleCode()).isEqualTo(expectedInviteCode);

            then(memberDomainService).should().getMemberById(MemberId.of(memberId));
            then(member).should().signUp(nickname, loveStartDate);
            then(member).should().updateInviteCode(inviteCodeValue);
            then(saveMemberPort).should().saveMember(member);
            then(termsAgreementDomainService).should().processAgreements(eq(MemberId.of(memberId)), anyList());
            then(inviteCodeDomainService).should().generateUniqueInviteCode();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원으로 회원가입 시 MemberNotFoundException이 발생한다")
        void givenNonExistentMember_whenSignUp_thenThrowMemberNotFoundException() {
            // Given
            Long memberId = 999L;
            String nickname = "테스트닉네임";
            LocalDate loveStartDate = LocalDate.of(2024, 1, 1);

            SignUpUseCase.SignUpCommand command = SignUpUseCase.SignUpCommand.builder()
                    .memberId(memberId)
                    .nickname(nickname)
                    .loveStartDate(loveStartDate)
                    .terms(Arrays.asList())
                    .build();

            given(memberDomainService.getMemberById(MemberId.of(memberId)))
                    .willThrow(new MemberNotFoundException());

            // When & Then
            assertThatThrownBy(() -> signUpService.signUp(command))
                    .isInstanceOf(MemberNotFoundException.class);

            then(memberDomainService).should().getMemberById(MemberId.of(memberId));
            then(saveMemberPort).should(never()).saveMember(any());
            then(termsAgreementDomainService).should(never()).processAgreements(any(), anyList());
            then(inviteCodeDomainService).should(never()).generateUniqueInviteCode();
        }

        @Test
        @DisplayName("실패: 약관 동의 처리 실패 시 TermsNotFoundException이 발생한다")
        void givenTermsAgreementProcessingFailure_whenSignUp_thenThrowTermsNotFoundException() {
            // Given
            Long memberId = 1L;
            String nickname = "테스트닉네임";
            LocalDate loveStartDate = LocalDate.of(2024, 1, 1);

            SignUpUseCase.TermsCommand termsCommand = SignUpUseCase.TermsCommand.builder()
                    .termsId(999L)
                    .isAgreed(true)
                    .build();

            SignUpUseCase.SignUpCommand command = SignUpUseCase.SignUpCommand.builder()
                    .memberId(memberId)
                    .nickname(nickname)
                    .loveStartDate(loveStartDate)
                    .terms(Arrays.asList(termsCommand))
                    .build();

            Member member = mock(Member.class);
            InviteCodeValue inviteCodeValue = InviteCodeValue.of("INVITE123");

            given(member.getId()).willReturn(memberId);
            given(memberDomainService.getMemberById(MemberId.of(memberId))).willReturn(member);
            given(inviteCodeDomainService.generateUniqueInviteCode()).willReturn(inviteCodeValue);
            given(saveMemberPort.saveMember(member)).willReturn(member);
            willThrow(new TermsNotFoundException())
                    .given(termsAgreementDomainService).processAgreements(eq(MemberId.of(memberId)), anyList());

            // When & Then
            assertThatThrownBy(() -> signUpService.signUp(command))
                    .isInstanceOf(TermsNotFoundException.class);

            then(memberDomainService).should().getMemberById(MemberId.of(memberId));
            then(member).should().signUp(nickname, loveStartDate);
            then(inviteCodeDomainService).should().generateUniqueInviteCode();
            then(member).should().updateInviteCode(inviteCodeValue);
            then(saveMemberPort).should().saveMember(member);
            then(termsAgreementDomainService).should().processAgreements(eq(MemberId.of(memberId)), anyList());
        }

        @Test
        @DisplayName("실패: 커플 코드 생성 실패 시 InviteCodeGenerateFailedException이 발생한다")
        void givenCoupleCodeGenerationFailure_whenSignUp_thenThrowInviteCodeGenerateFailedException() {
            // Given
            Long memberId = 1L;
            String nickname = "테스트닉네임";
            LocalDate loveStartDate = LocalDate.of(2024, 1, 1);

            SignUpUseCase.TermsCommand termsCommand = SignUpUseCase.TermsCommand.builder()
                    .termsId(1L)
                    .isAgreed(true)
                    .build();

            SignUpUseCase.SignUpCommand command = SignUpUseCase.SignUpCommand.builder()
                    .memberId(memberId)
                    .nickname(nickname)
                    .loveStartDate(loveStartDate)
                    .terms(Arrays.asList(termsCommand))
                    .build();

            Member member = mock(Member.class);

            given(memberDomainService.getMemberById(MemberId.of(memberId))).willReturn(member);
            willThrow(new InviteCodeGenerateFailedException("커플 코드 생성에 실패했습니다. 재시도 횟수를 초과했습니다."))
                    .given(inviteCodeDomainService).generateUniqueInviteCode();

            // When & Then
            assertThatThrownBy(() -> signUpService.signUp(command))
                    .isInstanceOf(InviteCodeGenerateFailedException.class)
                    .hasMessage("커플 코드 생성에 실패했습니다. 재시도 횟수를 초과했습니다.");

            then(memberDomainService).should().getMemberById(MemberId.of(memberId));
            then(member).should().signUp(nickname, loveStartDate);
            then(inviteCodeDomainService).should().generateUniqueInviteCode();
            then(member).should(never()).updateInviteCode(any());
            then(saveMemberPort).should(never()).saveMember(any());
            then(termsAgreementDomainService).should(never()).processAgreements(any(), anyList());
        }
    }
}