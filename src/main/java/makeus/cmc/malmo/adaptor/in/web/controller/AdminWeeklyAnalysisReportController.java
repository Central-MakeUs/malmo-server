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
import makeus.cmc.malmo.adaptor.in.web.dto.BaseResponse;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.TriggerWeeklyAnalysisReportUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "관리자 주간 분석 리포트 API", description = "주간 분석 리포트 수동 트리거용 관리자 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/reports/weekly")
public class AdminWeeklyAnalysisReportController {

    private final TriggerWeeklyAnalysisReportUseCase triggerWeeklyAnalysisReportUseCase;

    @Operation(
            summary = "주간 분석 리포트 수동 트리거",
            description = "특정 주차의 주간 분석 리포트 예약 배치를 수동으로 다시 실행합니다. weekStartDate는 yyyy-MM-dd 형식의 월요일 날짜여야 하며, 관리자 권한이 필요합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "주간 분석 리포트 수동 트리거 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.AdminWeeklyAnalysisReportTriggerSuccessResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "weekStartDate가 월요일이 아니거나 잘못된 형식임",
            content = @Content(schema = @Schema(implementation = SwaggerErrorResponse.class))
    )
    @ApiCommonResponses.RequireAdmin
    @PostMapping("/{weekStartDate}/trigger")
    public BaseResponse<TriggerWeeklyAnalysisReportUseCase.TriggerWeeklyAnalysisReportResponse> triggerWeeklyAnalysisReports(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate
    ) {
        return BaseResponse.success(
                triggerWeeklyAnalysisReportUseCase.triggerWeeklyAnalysisReports(
                        new TriggerWeeklyAnalysisReportUseCase.TriggerWeeklyAnalysisReportCommand(weekStartDate)
                )
        );
    }
}
