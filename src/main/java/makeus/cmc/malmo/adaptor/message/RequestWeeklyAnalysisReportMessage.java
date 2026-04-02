package makeus.cmc.malmo.adaptor.message;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RequestWeeklyAnalysisReportMessage implements StreamMessage {
    private Long weeklyAnalysisReportId;

    public RequestWeeklyAnalysisReportMessage(Long weeklyAnalysisReportId) {
        this.weeklyAnalysisReportId = weeklyAnalysisReportId;
    }
}
