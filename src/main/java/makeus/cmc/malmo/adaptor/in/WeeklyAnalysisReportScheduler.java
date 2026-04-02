package makeus.cmc.malmo.adaptor.in;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.ScheduleWeeklyAnalysisReportUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class WeeklyAnalysisReportScheduler {

    private final ScheduleWeeklyAnalysisReportUseCase scheduleWeeklyAnalysisReportUseCase;

    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    public void scheduleWeeklyReports() {
        scheduleWeeklyAnalysisReportUseCase.scheduleWeeklyAnalysisReports();
    }
}
