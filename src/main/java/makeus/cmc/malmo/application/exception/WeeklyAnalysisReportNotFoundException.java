package makeus.cmc.malmo.application.exception;

public class WeeklyAnalysisReportNotFoundException extends RuntimeException {
    public WeeklyAnalysisReportNotFoundException() {
        super("주간 분석 리포트가 존재하지 않습니다.");
    }
}
