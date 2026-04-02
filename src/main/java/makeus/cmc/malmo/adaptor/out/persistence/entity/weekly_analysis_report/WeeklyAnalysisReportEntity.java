package makeus.cmc.malmo.adaptor.out.persistence.entity.weekly_analysis_report;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import makeus.cmc.malmo.adaptor.out.persistence.entity.BaseTimeEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReportContent;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "weekly_analysis_report",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_weekly_analysis_report_member_week",
                        columnNames = {"member_id", "week_start_date"}
                )
        }
)
public class WeeklyAnalysisReportEntity extends BaseTimeEntity {

    @Column(name = "weekly_analysis_report_id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private MemberEntityId memberId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeeklyAnalysisReportStatus status;

    @Column(name = "source_chat_room_count", nullable = false)
    private int sourceChatRoomCount;

    @Column(name = "eligible_chat_room_count", nullable = false)
    private int eligibleChatRoomCount;

    @Column(name = "source_user_message_count", nullable = false)
    private int sourceUserMessageCount;

    @Column(name = "schema_version", length = 32)
    private String schemaVersion;

    @Column(name = "timezone", length = 64)
    private String timezone;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "title", column = @Column(name = "overview_title", columnDefinition = "TEXT")),
            @AttributeOverride(name = "summary", column = @Column(name = "overview_summary", columnDefinition = "TEXT"))
    })
    private OverviewEmbeddable overview;

    @org.hibernate.annotations.BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "weekly_analysis_report_top_topic",
            joinColumns = @JoinColumn(name = "weekly_analysis_report_id")
    )
    @OrderBy("rank ASC")
    @Builder.Default
    private List<TopTopicEmbeddable> topTopics = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "dominantPeriod", column = @Column(name = "mood_dominant_period", length = 32)),
            @AttributeOverride(name = "morningRatio", column = @Column(name = "mood_ratio_morning")),
            @AttributeOverride(name = "afternoonRatio", column = @Column(name = "mood_ratio_afternoon")),
            @AttributeOverride(name = "eveningRatio", column = @Column(name = "mood_ratio_evening")),
            @AttributeOverride(name = "lateNightRatio", column = @Column(name = "mood_ratio_late_night")),
            @AttributeOverride(name = "description", column = @Column(name = "mood_description", columnDefinition = "TEXT"))
    })
    private MoodByTimeEmbeddable moodByTime;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "score", column = @Column(name = "conflict_score")),
            @AttributeOverride(name = "description", column = @Column(name = "conflict_description", columnDefinition = "TEXT"))
    })
    private ConflictEmbeddable conflict;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "oneLineSummary", column = @Column(name = "behavior_pattern_one_line_summary", columnDefinition = "TEXT")),
            @AttributeOverride(name = "triggerSituation", column = @Column(name = "behavior_pattern_trigger_situation", columnDefinition = "TEXT")),
            @AttributeOverride(name = "belief", column = @Column(name = "behavior_pattern_belief", columnDefinition = "TEXT")),
            @AttributeOverride(name = "responseType", column = @Column(name = "behavior_pattern_response_type", columnDefinition = "TEXT"))
    })
    private BehaviorPatternEmbeddable behaviorPattern;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "title", column = @Column(name = "solution_title", columnDefinition = "TEXT")),
            @AttributeOverride(name = "content", column = @Column(name = "solution_content", columnDefinition = "TEXT"))
    })
    private SolutionEmbeddable solution;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "failed_reason", length = 255)
    private String failedReason;

    public WeeklyAnalysisReportContent toContent() {
        if (schemaVersion == null) {
            return null;
        }

        return new WeeklyAnalysisReportContent(
                schemaVersion,
                new WeeklyAnalysisReportContent.Period(weekStartDate, weekEndDate, timezone),
                overview.toDomain(),
                topTopics.stream().map(TopTopicEmbeddable::toDomain).toList(),
                moodByTime.toDomain(),
                conflict.toDomain(),
                behaviorPattern.toDomain(),
                solution.toDomain()
        );
    }

    @Embeddable
    public static class OverviewEmbeddable {
        private String title;
        private String summary;

        protected OverviewEmbeddable() {
        }

        private OverviewEmbeddable(String title, String summary) {
            this.title = title;
            this.summary = summary;
        }

        public static OverviewEmbeddable from(WeeklyAnalysisReportContent.Overview overview) {
            return new OverviewEmbeddable(overview.title(), overview.summary());
        }

        public WeeklyAnalysisReportContent.Overview toDomain() {
            return new WeeklyAnalysisReportContent.Overview(title, summary);
        }
    }

    @Embeddable
    public static class TopTopicEmbeddable {
        @Column(name = "keyword", nullable = false, columnDefinition = "TEXT")
        private String keyword;

        @Column(name = "topic_rank", nullable = false)
        private int rank;

        @Column(name = "weight", nullable = false)
        private double weight;

        @Column(name = "description", nullable = false, columnDefinition = "TEXT")
        private String description;

        protected TopTopicEmbeddable() {
        }

        private TopTopicEmbeddable(String keyword, int rank, double weight, String description) {
            this.keyword = keyword;
            this.rank = rank;
            this.weight = weight;
            this.description = description;
        }

        public static TopTopicEmbeddable from(WeeklyAnalysisReportContent.TopTopic topTopic) {
            return new TopTopicEmbeddable(topTopic.keyword(), topTopic.rank(), topTopic.weight(), topTopic.description());
        }

        public WeeklyAnalysisReportContent.TopTopic toDomain() {
            return new WeeklyAnalysisReportContent.TopTopic(keyword, rank, weight, description);
        }
    }

    @Embeddable
    public static class MoodByTimeEmbeddable {
        private String dominantPeriod;
        private double morningRatio;
        private double afternoonRatio;
        private double eveningRatio;
        private double lateNightRatio;
        private String description;

        protected MoodByTimeEmbeddable() {
        }

        private MoodByTimeEmbeddable(
                String dominantPeriod,
                double morningRatio,
                double afternoonRatio,
                double eveningRatio,
                double lateNightRatio,
                String description
        ) {
            this.dominantPeriod = dominantPeriod;
            this.morningRatio = morningRatio;
            this.afternoonRatio = afternoonRatio;
            this.eveningRatio = eveningRatio;
            this.lateNightRatio = lateNightRatio;
            this.description = description;
        }

        public static MoodByTimeEmbeddable from(WeeklyAnalysisReportContent.MoodByTime moodByTime) {
            return new MoodByTimeEmbeddable(
                    moodByTime.dominantPeriod(),
                    moodByTime.ratios().morning(),
                    moodByTime.ratios().afternoon(),
                    moodByTime.ratios().evening(),
                    moodByTime.ratios().lateNight(),
                    moodByTime.description()
            );
        }

        public WeeklyAnalysisReportContent.MoodByTime toDomain() {
            return new WeeklyAnalysisReportContent.MoodByTime(
                    dominantPeriod,
                    new WeeklyAnalysisReportContent.MoodRatios(
                            morningRatio,
                            afternoonRatio,
                            eveningRatio,
                            lateNightRatio
                    ),
                    description
            );
        }
    }

    @Embeddable
    public static class ConflictEmbeddable {
        private int score;
        private String description;

        protected ConflictEmbeddable() {
        }

        private ConflictEmbeddable(int score, String description) {
            this.score = score;
            this.description = description;
        }

        public static ConflictEmbeddable from(WeeklyAnalysisReportContent.Conflict conflict) {
            return new ConflictEmbeddable(conflict.score(), conflict.description());
        }

        public WeeklyAnalysisReportContent.Conflict toDomain() {
            return new WeeklyAnalysisReportContent.Conflict(score, description);
        }
    }

    @Embeddable
    public static class BehaviorPatternEmbeddable {
        private String oneLineSummary;
        private String triggerSituation;
        private String belief;
        private String responseType;

        protected BehaviorPatternEmbeddable() {
        }

        private BehaviorPatternEmbeddable(
                String oneLineSummary,
                String triggerSituation,
                String belief,
                String responseType
        ) {
            this.oneLineSummary = oneLineSummary;
            this.triggerSituation = triggerSituation;
            this.belief = belief;
            this.responseType = responseType;
        }

        public static BehaviorPatternEmbeddable from(WeeklyAnalysisReportContent.BehaviorPattern behaviorPattern) {
            return new BehaviorPatternEmbeddable(
                    behaviorPattern.oneLineSummary(),
                    behaviorPattern.triggerSituation(),
                    behaviorPattern.belief(),
                    behaviorPattern.responseType()
            );
        }

        public WeeklyAnalysisReportContent.BehaviorPattern toDomain() {
            return new WeeklyAnalysisReportContent.BehaviorPattern(
                    oneLineSummary,
                    triggerSituation,
                    belief,
                    responseType
            );
        }
    }

    @Embeddable
    public static class SolutionEmbeddable {
        private String title;
        private String content;

        protected SolutionEmbeddable() {
        }

        private SolutionEmbeddable(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public static SolutionEmbeddable from(WeeklyAnalysisReportContent.Solution solution) {
            return new SolutionEmbeddable(solution.title(), solution.content());
        }

        public WeeklyAnalysisReportContent.Solution toDomain() {
            return new WeeklyAnalysisReportContent.Solution(title, content);
        }
    }
}
