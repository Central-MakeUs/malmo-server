package makeus.cmc.malmo.domain.model.weekly_analysis_report;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record WeeklyAnalysisReportContent(
        String schemaVersion,
        Period period,
        Overview overview,
        List<TopTopic> topTopics,
        MoodByTime moodByTime,
        Conflict conflict,
        BehaviorPattern behaviorPattern,
        Solution solution
) {

    public WeeklyAnalysisReportContent {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        period = Objects.requireNonNull(period, "period must not be null");
        overview = Objects.requireNonNull(overview, "overview must not be null");
        topTopics = List.copyOf(Objects.requireNonNull(topTopics, "topTopics must not be null"));
        if (topTopics.isEmpty()) {
            throw new IllegalArgumentException("topTopics must not be empty");
        }
        moodByTime = Objects.requireNonNull(moodByTime, "moodByTime must not be null");
        conflict = Objects.requireNonNull(conflict, "conflict must not be null");
        behaviorPattern = Objects.requireNonNull(behaviorPattern, "behaviorPattern must not be null");
        solution = Objects.requireNonNull(solution, "solution must not be null");
    }

    public record Period(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            String timezone
    ) {
        public Period {
            weekStartDate = Objects.requireNonNull(weekStartDate, "weekStartDate must not be null");
            weekEndDate = Objects.requireNonNull(weekEndDate, "weekEndDate must not be null");
            timezone = requireText(timezone, "timezone");
        }
    }

    public record Overview(
            String title,
            String summary
    ) {
        public Overview {
            title = requireText(title, "title");
            summary = requireText(summary, "summary");
        }
    }

    public record TopTopic(
            String keyword,
            int rank,
            double weight,
            String description
    ) {
        public TopTopic {
            keyword = requireText(keyword, "keyword");
            if (rank < 1) {
                throw new IllegalArgumentException("rank must be greater than 0");
            }
            description = requireText(description, "description");
        }
    }

    public record MoodByTime(
            String dominantPeriod,
            MoodRatios ratios,
            String description
    ) {
        public MoodByTime {
            dominantPeriod = requireText(dominantPeriod, "dominantPeriod");
            ratios = Objects.requireNonNull(ratios, "ratios must not be null");
            description = requireText(description, "description");
        }
    }

    public record MoodRatios(
            double morning,
            double afternoon,
            double evening,
            double lateNight
    ) {
    }

    public record Conflict(
            int score,
            String description
    ) {
        public Conflict {
            description = requireText(description, "description");
        }
    }

    public record BehaviorPattern(
            String oneLineSummary,
            String triggerSituation,
            String belief,
            String responseType
    ) {
        public BehaviorPattern {
            oneLineSummary = requireText(oneLineSummary, "oneLineSummary");
            triggerSituation = requireText(triggerSituation, "triggerSituation");
            belief = requireText(belief, "belief");
            responseType = requireText(responseType, "responseType");
        }
    }

    public record Solution(
            String title,
            String content
    ) {
        public Solution {
            title = requireText(title, "title");
            content = requireText(content, "content");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
