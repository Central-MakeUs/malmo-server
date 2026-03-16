package makeus.cmc.malmo.adaptor.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.web.docs.ApiCommonResponses;
import makeus.cmc.malmo.adaptor.in.web.docs.SwaggerResponses;
import makeus.cmc.malmo.adaptor.in.web.dto.BaseResponse;
import makeus.cmc.malmo.application.port.in.couple.CoupleLinkUseCase;
import makeus.cmc.malmo.application.port.in.couple.CoupleUnlinkUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@Tag(name = "커플 관리 API", description = "커플 연결 및 관리 관련 API (Deprecated)")
@RestController
@RequestMapping("/couples")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleLinkUseCase coupleLinkUseCase;
    private final CoupleUnlinkUseCase coupleUnlinkUseCase;

    @Operation(
            summary = "커플 연결",
            description = "[Deprecated] 커플 초대코드를 사용하여 커플을 연결합니다. 커플 연동 기능은 제거 예정이며, 앞으로는 사용자가 커플 정보를 직접 입력하는 방식을 사용합니다. JWT 토큰이 필요합니다.",
            deprecated = true,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "커플 연결 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.CoupleLinkSuccessResponse.class))
    )
    @ApiCommonResponses.RequireAuth
    @ApiCommonResponses.CoupleCode
    @PostMapping
    @Deprecated
    public BaseResponse<CoupleLinkUseCase.CoupleLinkResponse> linkCouple(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CoupleLinkRequestDto requestDto
    ) {
        CoupleLinkUseCase.CoupleLinkCommand command = CoupleLinkUseCase.CoupleLinkCommand.builder()
                .coupleCode(requestDto.getCoupleCode())
                .userId(Long.valueOf(user.getUsername()))
                .build();
        return BaseResponse.success(coupleLinkUseCase.coupleLink(command));
    }

    @Operation(
            summary = "커플 연결 끊기",
            description = "[Deprecated] 연결된 커플을 끊습니다. 커플 연동 기능은 제거 예정이며, 앞으로는 사용자가 커플 정보를 직접 입력하는 방식을 사용합니다. JWT 토큰이 필요합니다.",
            deprecated = true,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "커플 연결 끊기 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.CoupleUnlinkSuccessResponse.class))
    )
    @ApiCommonResponses.RequireAuth
    @ApiCommonResponses.OnlyCouple
    @DeleteMapping
    @Deprecated
    public BaseResponse unlinkCouple(
            @AuthenticationPrincipal User user
    ) {
        CoupleUnlinkUseCase.CoupleUnlinkCommand command = CoupleUnlinkUseCase.CoupleUnlinkCommand.builder()
                .userId(Long.valueOf(user.getUsername()))
                .build();

        coupleUnlinkUseCase.coupleUnlink(command);

        return BaseResponse.success(null);
    }


    @Data
    @Deprecated
    @Schema(description = "[Deprecated] 커플 연결 요청 DTO")
    public static class CoupleLinkRequestDto {
        @NotBlank(message = "초대코드는 필수 입력값입니다.")
        @Size(min = 6, max = 8, message = "커플 코드는 6 ~ 8자리여야 합니다.")
        private String coupleCode;
    }
}
