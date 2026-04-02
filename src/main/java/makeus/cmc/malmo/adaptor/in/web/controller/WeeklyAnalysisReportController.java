package makeus.cmc.malmo.adaptor.in.web.controller;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import makeus.cmc.malmo.adaptor.in.web.docs.ApiCommonResponses;
import makeus.cmc.malmo.adaptor.in.web.docs.SwaggerErrorResponse;
import makeus.cmc.malmo.adaptor.in.web.docs.SwaggerResponses;
import makeus.cmc.malmo.adaptor.in.web.dto.BaseListResponse;
import makeus.cmc.malmo.adaptor.in.web.dto.BaseResponse;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.GetWeeklyAnalysisReportListUseCase;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.GetWeeklyAnalysisReportUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "주간 분석 리포트 API", description = "주간 분석 리포트 목록, 상세 조회 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/reports/weekly")
public class WeeklyAnalysisReportController {

    private final GetWeeklyAnalysisReportUseCase getWeeklyAnalysisReportUseCase;
    private final GetWeeklyAnalysisReportListUseCase getWeeklyAnalysisReportListUseCase;

    @Operation(
            summary = "주간 분석 리포트 목록 조회",
            description = "현재 로그인한 사용자의 발행된 주간 분석 리포트 목록을 최신 주차 순으로 조회합니다. JWT 토큰이 필요합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "주간 분석 리포트 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.WeeklyAnalysisReportListSuccessResponse.class))
    )
    @ApiCommonResponses.RequireAuth
    @GetMapping
    public BaseResponse<BaseListResponse<GetWeeklyAnalysisReportListUseCase.GetWeeklyAnalysisReportListItemResponse>> getWeeklyAnalysisReportList(
            @AuthenticationPrincipal User user
    ) {
        GetWeeklyAnalysisReportListUseCase.GetWeeklyAnalysisReportListResponse response = getWeeklyAnalysisReportListUseCase.getWeeklyAnalysisReportList(
                new GetWeeklyAnalysisReportListUseCase.GetWeeklyAnalysisReportListCommand(
                        Long.valueOf(user.getUsername())
                )
        );

        return BaseListResponse.success(response.getReports(), response.getTotalCount());
    }

    @Operation(
            summary = "주간 분석 리포트 상세 조회",
            description = "주차 시작일 기준으로 현재 로그인한 사용자의 주간 분석 리포트 상세를 조회합니다. weekStartDate는 yyyy-MM-dd 형식의 월요일 날짜여야 하며, JWT 토큰이 필요합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "주간 분석 리포트 상세 조회 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.WeeklyAnalysisReportDetailSuccessResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "해당 주차의 주간 분석 리포트가 존재하지 않음",
            content = @Content(schema = @Schema(implementation = SwaggerErrorResponse.class))
    )
    @ApiCommonResponses.RequireAuth
    @GetMapping("/{weekStartDate}")
    public BaseResponse<GetWeeklyAnalysisReportUseCase.GetWeeklyAnalysisReportResponse> getWeeklyAnalysisReport(
            @AuthenticationPrincipal User user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate
    ) {
        return BaseResponse.success(
                getWeeklyAnalysisReportUseCase.getWeeklyAnalysisReport(
                        new GetWeeklyAnalysisReportUseCase.GetWeeklyAnalysisReportCommand(
                                Long.valueOf(user.getUsername()),
                                weekStartDate
                        )
                )
        );
    }

}
