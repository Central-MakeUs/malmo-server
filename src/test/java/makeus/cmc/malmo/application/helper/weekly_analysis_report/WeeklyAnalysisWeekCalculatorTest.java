package makeus.cmc.malmo.application.helper.weekly_analysis_report;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyAnalysisWeekCalculatorTest {

    private final WeeklyAnalysisWeekCalculator weeklyAnalysisWeekCalculator = new WeeklyAnalysisWeekCalculator();

    @Test
    void getPreviousClosedWeek_returnsPreviousMondayToSundayInSeoul() {
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = weeklyAnalysisWeekCalculator.getPreviousClosedWeek(
                ZonedDateTime.of(2026, 3, 30, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))
        );

        assertThat(weekPeriod.getWeekStartDate()).isEqualTo(java.time.LocalDate.of(2026, 3, 23));
        assertThat(weekPeriod.getWeekEndDate()).isEqualTo(java.time.LocalDate.of(2026, 3, 29));
        assertThat(weekPeriod.getWeekStartAt()).isEqualTo(java.time.LocalDateTime.of(2026, 3, 23, 0, 0));
        assertThat(weekPeriod.getWeekEndAtExclusive()).isEqualTo(java.time.LocalDateTime.of(2026, 3, 30, 0, 0));
        assertThat(weekPeriod.getZoneId()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }

    @Test
    void fromWeekStartDate_normalizesToMonday() {
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = weeklyAnalysisWeekCalculator.fromWeekStartDate(
                java.time.LocalDate.of(2026, 3, 25)
        );

        assertThat(weekPeriod.getWeekStartDate()).isEqualTo(java.time.LocalDate.of(2026, 3, 23));
        assertThat(weekPeriod.getWeekEndDate()).isEqualTo(java.time.LocalDate.of(2026, 3, 29));
    }
}
