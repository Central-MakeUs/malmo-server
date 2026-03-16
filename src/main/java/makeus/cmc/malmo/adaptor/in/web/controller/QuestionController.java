package makeus.cmc.malmo.adaptor.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.in.web.docs.ApiCommonResponses;
import makeus.cmc.malmo.adaptor.in.web.docs.SwaggerResponses;
import makeus.cmc.malmo.adaptor.in.web.dto.BaseResponse;
import makeus.cmc.malmo.application.port.in.question.AnswerQuestionUseCase;
import makeus.cmc.malmo.application.port.in.question.GetQuestionAnswerUseCase;
import makeus.cmc.malmo.application.port.in.question.GetQuestionUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@Tag(name = "오늘의 질문 API", description = "오늘의 질문 API (Deprecated)")
@Slf4j
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final GetQuestionUseCase getQuestionUseCase;
    private final GetQuestionAnswerUseCase getQuestionAnswerUseCase;
    private final AnswerQuestionUseCase answerQuestionUseCase;

    @Operation(
            summary = "오늘의 질문 조회",
            description = "[Deprecated] 오늘의 질문 기능은 제거 예정입니다. JWT 토큰이 필요합니다.",
            deprecated = true,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "오늘의 질문 조회 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.QuestionSuccessResponse.class))
    )
    @ApiCommonResponses.RequireAuth
    @GetMapping("/today")
    @Deprecated
    public BaseResponse<GetQuestionUseCase.GetQuestionResponse> getTodayQuestion(
            @AuthenticationPrincipal User user
    ) {
        GetQuestionUseCase.GetTodayQuestionCommand command = GetQuestionUseCase.GetTodayQuestionCommand.builder()
                .userId(Long.valueOf(user.getUsername()))
                .build();

        return BaseResponse.success(getQuestionUseCase.getTodayQuestion(command));
    }

    @Operation(
            summary = "과거 질문 조회",
            description = "[Deprecated] 오늘의 질문 기능은 제거 예정입니다. JWT 토큰이 필요합니다.",
            deprecated = true,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "질문 내용 조회 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.PastQuestionSuccessResponse.class))
    )
    @ApiCommonResponses.OnlyCouple
    @ApiCommonResponses.RequireAuth
    @GetMapping("/{level}")
    @Deprecated
    public BaseResponse<GetQuestionUseCase.GetQuestionResponse> getQuestion(
            @AuthenticationPrincipal User user,
            @PathVariable int level) {
        GetQuestionUseCase.GetQuestionCommand command = GetQuestionUseCase.GetQuestionCommand.builder()
                .userId(Long.valueOf(user.getUsername()))
                .level(level)
                .build();

        return BaseResponse.success(getQuestionUseCase.getQuestion(command));
    }

    @Operation(
            summary = "질문 답변 조회",
            description = "[Deprecated] 오늘의 질문 기능은 제거 예정입니다. JWT 토큰이 필요합니다.",
            deprecated = true,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "질문 답변 조회 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.PastAnswerSuccessResponse.class))
    )
    @ApiCommonResponses.OnlyOwner
    @ApiCommonResponses.RequireAuth
    @GetMapping("/{coupleQuestionId}/answers")
    @Deprecated
    public BaseResponse<GetQuestionAnswerUseCase.AnswerResponseDto> getAnswers(
            @AuthenticationPrincipal User user,
            @PathVariable Long coupleQuestionId) {
        GetQuestionAnswerUseCase.GetQuestionAnswerCommand command = GetQuestionAnswerUseCase.GetQuestionAnswerCommand.builder()
                .userId(Long.valueOf(user.getUsername()))
                .coupleQuestionId(coupleQuestionId)
                .build();

        return BaseResponse.success(getQuestionAnswerUseCase.getQuestionAnswers(command));
    }

    @Operation(
            summary = "오늘의 질문 답변 등록",
            description = "[Deprecated] 오늘의 질문 기능은 제거 예정입니다. JWT 토큰이 필요합니다.",
            deprecated = true,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "질문 답변 등록 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.AnswerSuccessResponse.class))
    )
    @ApiCommonResponses.RequireAuth
    @PostMapping("/today/answers")
    @Deprecated
    public BaseResponse<AnswerQuestionUseCase.QuestionAnswerResponse> postAnswer(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AnswerRequestDto requestDto
    ) {
        AnswerQuestionUseCase.AnswerQuestionCommand command = AnswerQuestionUseCase.AnswerQuestionCommand.builder()
                .userId(Long.valueOf(user.getUsername()))
                .answer(requestDto.getAnswer())
                .build();

        return BaseResponse.success(answerQuestionUseCase.answerQuestion(command));
    }

    @Operation(
            summary = "오늘의 질문 답변 수정",
            description = "[Deprecated] 오늘의 질문 기능은 제거 예정입니다. JWT 토큰이 필요합니다.",
            deprecated = true,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "질문 답변 수정 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.AnswerSuccessResponse.class))
    )
    @ApiCommonResponses.RequireAuth
    @PatchMapping("/today/answers")
    @Deprecated
    public BaseResponse<AnswerQuestionUseCase.QuestionAnswerResponse> updateAnswer(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AnswerRequestDto requestDto
    ) {
        AnswerQuestionUseCase.AnswerQuestionCommand command = AnswerQuestionUseCase.AnswerQuestionCommand.builder()
                .userId(Long.valueOf(user.getUsername()))
                .answer(requestDto.getAnswer())
                .build();

        return BaseResponse.success(answerQuestionUseCase.updateAnswer(command));
    }

    @Data
    public static class AnswerRequestDto {
        private String answer;
    }
}
