package makeus.cmc.malmo.service;

import makeus.cmc.malmo.adaptor.out.persistence.exception.CoupleCodeNotFoundException;
import makeus.cmc.malmo.application.port.in.GetInviteCodeUseCase;
import makeus.cmc.malmo.application.port.out.LoadCoupleCodePort;
import makeus.cmc.malmo.application.service.InviteCodeService;
import makeus.cmc.malmo.domain.model.member.CoupleCode;
import makeus.cmc.malmo.domain.model.value.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InviteCodeService 단위 테스트")
class InviteCodeServiceTest {

    @Mock
    private LoadCoupleCodePort loadCoupleCodePort;

    @InjectMocks
    private InviteCodeService inviteCodeService;

    @Nested
    @DisplayName("초대 코드 조회 기능")
    class GetInviteCodeFeature {

        @Test
        @DisplayName("성공: 유효한 사용자 ID로 초대 코드 조회가 성공한다")
        void givenValidUserId_whenGetInviteCode_thenReturnInviteCodeResponse() {
            // Given
            Long userId = 1L;
            String expectedInviteCode = "INVITE123";

            GetInviteCodeUseCase.InviteCodeCommand command = GetInviteCodeUseCase.InviteCodeCommand.builder()
                    .userId(userId)
                    .build();

            CoupleCode coupleCode = mock(CoupleCode.class);
            given(coupleCode.getInviteCode()).willReturn(expectedInviteCode);

            given(loadCoupleCodePort.loadCoupleCodeByMemberId(MemberId.of(userId)))
                    .willReturn(Optional.of(coupleCode));

            // When
            GetInviteCodeUseCase.InviteCodeResponseDto response = inviteCodeService.getInviteCode(command);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getCoupleCode()).isEqualTo(expectedInviteCode);

            then(loadCoupleCodePort).should().loadCoupleCodeByMemberId(MemberId.of(userId));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 ID로 초대 코드 조회 시 CoupleCodeNotFoundException이 발생한다")
        void givenNonExistentUserId_whenGetInviteCode_thenThrowCoupleCodeNotFoundException() {
            // Given
            Long userId = 999L;

            GetInviteCodeUseCase.InviteCodeCommand command = GetInviteCodeUseCase.InviteCodeCommand.builder()
                    .userId(userId)
                    .build();

            given(loadCoupleCodePort.loadCoupleCodeByMemberId(MemberId.of(userId)))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inviteCodeService.getInviteCode(command))
                    .isInstanceOf(CoupleCodeNotFoundException.class);

            then(loadCoupleCodePort).should().loadCoupleCodeByMemberId(MemberId.of(userId));
        }
    }
}