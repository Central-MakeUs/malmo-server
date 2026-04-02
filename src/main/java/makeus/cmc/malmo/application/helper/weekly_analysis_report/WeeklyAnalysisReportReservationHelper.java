package makeus.cmc.malmo.application.helper.weekly_analysis_report;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.message.RequestWeeklyAnalysisReportMessage;
import makeus.cmc.malmo.adaptor.message.StreamMessageType;
import makeus.cmc.malmo.application.helper.outbox.OutboxHelper;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class WeeklyAnalysisReportReservationHelper {

    private final WeeklyAnalysisReportCommandHelper weeklyAnalysisReportCommandHelper;
    private final OutboxHelper outboxHelper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WeeklyAnalysisReport reservePendingReport(WeeklyAnalysisReport weeklyAnalysisReport) {
        WeeklyAnalysisReport savedReport = weeklyAnalysisReportCommandHelper.saveAndFlushWeeklyAnalysisReport(weeklyAnalysisReport);

        outboxHelper.publish(
                StreamMessageType.REQUEST_WEEKLY_ANALYSIS_REPORT,
                new RequestWeeklyAnalysisReportMessage(savedReport.getId())
        );
        return savedReport;
    }
}
