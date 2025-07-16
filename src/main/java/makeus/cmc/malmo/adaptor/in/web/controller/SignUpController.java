package makeus.cmc.malmo.adaptor.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.web.docs.ApiCommonResponses;
import makeus.cmc.malmo.adaptor.in.web.docs.SwaggerResponses;
import makeus.cmc.malmo.adaptor.in.web.dto.BaseResponse;
import makeus.cmc.malmo.application.port.in.SignUpUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "회원가입 API", description = "사용자 회원가입 관련 API")
@RestController
@RequiredArgsConstructor
public class SignUpController {

    private final SignUpUseCase signUpUseCase;

    @Operation(
            summary = "회원가입",
            description = "인증된 사용자의 추가 정보를 입력받아 회원가입을 완료합니다. JWT 토큰이 필요합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "회원가입 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.SignUpSuccessResponse.class))
    )
    @ApiCommonResponses.RequireAuth
    @ApiCommonResponses.SignUp
    @PostMapping("/members/onboarding")
    public BaseResponse signUp(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SignUpRequestDto requestDto
    ) {
        List<SignUpUseCase.TermsCommand> termsCommandList = requestDto.getTerms().stream()
                .map(term -> SignUpUseCase.TermsCommand.builder()
                        .termsId(term.getTermsId())
                        .isAgreed(term.getIsAgreed())
                        .build())
                .toList();

        SignUpUseCase.SignUpCommand command = SignUpUseCase.SignUpCommand.builder()
                .memberId(Long.valueOf(user.getUsername()))
                .terms(termsCommandList)
                .nickname(requestDto.getNickname())
                .loveStartDate(requestDto.getLoveStartDate())
                .build();

        signUpUseCase.signUp(command);

        return BaseResponse.success();
    }

    @Data
    public static class SignUpRequestDto {
        private List<TermsDto> terms;

        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        @Size(min = 1, max = 10, message = "닉네임은 1자 이상 10자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다.")
        private String nickname;

        @NotBlank(message = "시작일은 필수 입력값입니다.")
        @PastOrPresent(message = "시작일은 오늘 또는 과거 날짜여야 합니다.")
        private LocalDate loveStartDate;
    }

    @Data
    public static class TermsDto {
        @NotNull(message = "약관 ID는 필수 입력값입니다.")
        private Long termsId;
        @NotNull(message = "약관 동의 여부는 필수 입력값입니다.")
        private Boolean isAgreed;
    }
}
