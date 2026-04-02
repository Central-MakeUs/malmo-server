package makeus.cmc.malmo.application.helper.weekly_analysis_report;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

@Component
public class WeeklyAnalysisWeekCalculator {

    public static final ZoneId WEEKLY_REPORT_ZONE_ID = ZoneId.of("Asia/Seoul");

    public WeekPeriod getPreviousClosedWeek() {
        return getPreviousClosedWeek(ZonedDateTime.now(WEEKLY_REPORT_ZONE_ID));
    }

    public WeekPeriod getPreviousClosedWeek(ZonedDateTime baseDateTime) {
        ZonedDateTime normalized = baseDateTime.withZoneSameInstant(WEEKLY_REPORT_ZONE_ID);
        LocalDate thisWeekStartDate = normalized.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate previousWeekStartDate = thisWeekStartDate.minusWeeks(1);
        return fromWeekStartDate(previousWeekStartDate);
    }

    public WeekPeriod fromWeekStartDate(LocalDate weekStartDate) {
        LocalDate normalizedWeekStartDate = weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEndDate = normalizedWeekStartDate.plusDays(6);
        LocalDateTime weekStartAt = LocalDateTime.of(normalizedWeekStartDate, LocalTime.MIN);
        LocalDateTime weekEndAtExclusive = LocalDateTime.of(normalizedWeekStartDate.plusWeeks(1), LocalTime.MIN);
        return new WeekPeriod(normalizedWeekStartDate, weekEndDate, weekStartAt, weekEndAtExclusive, WEEKLY_REPORT_ZONE_ID);
    }

    @Getter
    @RequiredArgsConstructor
    public static class WeekPeriod {
        private final LocalDate weekStartDate;
        private final LocalDate weekEndDate;
        private final LocalDateTime weekStartAt;
        private final LocalDateTime weekEndAtExclusive;
        private final ZoneId zoneId;
    }
}
