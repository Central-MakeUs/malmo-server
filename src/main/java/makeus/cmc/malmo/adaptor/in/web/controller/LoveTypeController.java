package makeus.cmc.malmo.adaptor.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.in.web.docs.SwaggerErrorResponse;
import makeus.cmc.malmo.adaptor.in.web.docs.SwaggerResponses;
import makeus.cmc.malmo.adaptor.in.web.dto.BaseListResponse;
import makeus.cmc.malmo.adaptor.in.web.dto.BaseResponse;
import makeus.cmc.malmo.application.port.in.CalculateQuestionResultUseCase;
import makeus.cmc.malmo.application.port.in.GetLoveTypePersonalityTypeResultUseCase;
import makeus.cmc.malmo.application.port.in.GetLoveTypeQuestionResultUseCase;
import makeus.cmc.malmo.application.port.in.GetLoveTypeQuestionsUseCase;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.hibernate.TypeMismatchException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@Tag(name = "애착유형 검사 API", description = "애착유형 검사 결과 등록 API")
@Slf4j
@Validated
@RestController
@RequestMapping("/love-types")
@RequiredArgsConstructor
public class LoveTypeController {

    private final GetLoveTypeQuestionsUseCase getLoveTypeQuestionsUseCase;
    private final CalculateQuestionResultUseCase calculateQuestionResultUseCase;
    private final GetLoveTypeQuestionResultUseCase getLoveTypeQuestionResultUseCase;
    private final GetLoveTypePersonalityTypeResultUseCase getLoveTypePersonalityTypeResultUseCase;

    @Operation(
            summary = "애착 유형 검사 질문 조회",
            description = "애착 유형 검사의 질문을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "애착 유형 조회 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.LoveTypeQuestionSuccessResponse.class))
    )
    @GetMapping("/questions")
    public BaseResponse<BaseListResponse<GetLoveTypeQuestionsUseCase.LoveTypeQuestionDto>> getLoveTypeQuestions() {
        GetLoveTypeQuestionsUseCase.LoveTypeQuestionsResponseDto loveTypeQuestions
                = getLoveTypeQuestionsUseCase.getLoveTypeQuestions();

        return BaseListResponse.success(loveTypeQuestions.getList(), loveTypeQuestions.getTotalCount());
    }

    @Operation(
            summary = "애착 유형 검사 질문 답변 및 결과 조회",
            description = "애착 유형 검사 답변의 결과를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "애착 유형 등록 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.LoveTypeQuestionCalculateSuccessResponse.class))
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = SwaggerErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "존재하지 않는 애착 유형 검사 질문",
                    content = @Content(schema = @Schema(implementation = SwaggerErrorResponse.class))
            )
    })
    @PostMapping("/result")
    public BaseResponse<CalculateQuestionResultUseCase.CalculateResultResponse> registerResult(
            @Valid @RequestBody RegisterLoveTypeRequestDto requestDto
    ) {
        List<CalculateQuestionResultUseCase.LoveTypeTestResult> results = requestDto.getResults().stream()
                .map(result -> CalculateQuestionResultUseCase.LoveTypeTestResult.builder()
                        .questionId(result.getQuestionId())
                        .score(result.getScore())
                        .build())
                .toList();

        CalculateQuestionResultUseCase.UpdateMemberLoveTypeCommand command =
                CalculateQuestionResultUseCase.UpdateMemberLoveTypeCommand.builder()
                        .results(results)
                        .build();

        return BaseResponse.success(calculateQuestionResultUseCase.calculateResult(command));
    }

    @Operation(
            summary = "애착 유형 검사 결과 조회",
            description = "애착 유형 검사 답변의 결과를 조회합니다. 답변 등록 시와 동일한 결과를 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "애착 유형 결과 조회 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.LoveTypeQuestionCalculateSuccessResponse.class))
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = SwaggerErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "존재하지 않는 애착 유형 결과",
                    content = @Content(schema = @Schema(implementation = SwaggerErrorResponse.class))
            )
    })
    @GetMapping("/result/{loveTypeId}")
    public BaseResponse<GetLoveTypeQuestionResultUseCase.LoveTypeResultResponse> getLoveTypeResult(
            @PathVariable Long loveTypeId
    ) {
        GetLoveTypeQuestionResultUseCase.GetLoveTypeResultCommand command = GetLoveTypeQuestionResultUseCase.GetLoveTypeResultCommand.builder()
                .loveTypeId(loveTypeId)
                .build();

        return BaseResponse.success(getLoveTypeQuestionResultUseCase.getResult(command));
    }

    @Operation(
            summary = "MBTI + 애착 유형 상세 결과 조회",
            description = "MBTI와 애착 유형 조합에 해당하는 상세 결과를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "애착 유형 상세 결과 조회 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.LoveTypePersonalityTypeResultSuccessResponse.class))
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = SwaggerErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 또는 존재하지 않는 MBTI/애착 유형 결과",
                    content = @Content(schema = @Schema(implementation = SwaggerErrorResponse.class))
            )
    })
    @GetMapping("/result")
    public BaseResponse<GetLoveTypePersonalityTypeResultUseCase.LoveTypePersonalityTypeResultResponse> getLoveTypePersonalityTypeResult(
            @RequestParam("personalityType")
            @Pattern(regexp = "^[a-zA-Z]{4}$", message = "MBTI는 영문 4자리여야 합니다.")
            String personalityType,
            @RequestParam("lovetype")
            @Pattern(
                    regexp = "(?i)^(STABLE_TYPE|ANXIETY_TYPE|AVOIDANCE_TYPE|CONFUSION_TYPE)$",
                    message = "유효한 애착 유형이 아닙니다."
            )
            String loveType
    ) {
        GetLoveTypePersonalityTypeResultUseCase.GetLoveTypePersonalityTypeResultCommand command =
                GetLoveTypePersonalityTypeResultUseCase.GetLoveTypePersonalityTypeResultCommand.builder()
                .personalityType(normalizePersonalityType(personalityType))
                .loveTypeCategory(normalizeLoveTypeCategory(loveType))
                .build();

        return BaseResponse.success(getLoveTypePersonalityTypeResultUseCase.getResult(command));
    }

    @Data
    public static class RegisterLoveTypeRequestDto {
        @Valid
        private List<LoveTypeTestResult> results;
    }

    @Data
    public static class LoveTypeTestResult {
        @NotNull(message = "질문 ID는 필수 입력값입니다.")
        private Long questionId;
        @NotNull(message = "점수는 필수 입력값입니다.")
        @Max(5) @Min(1)
        private Integer score;
    }

    private static String normalizePersonalityType(String personalityType) {
        return personalityType == null ? null : personalityType.toUpperCase(Locale.ROOT);
    }

    private static LoveTypeCategory normalizeLoveTypeCategory(String loveType) {
        try {
            return LoveTypeCategory.valueOf(loveType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new TypeMismatchException("유효한 애착 유형이 아닙니다.");
        }
    }
}
