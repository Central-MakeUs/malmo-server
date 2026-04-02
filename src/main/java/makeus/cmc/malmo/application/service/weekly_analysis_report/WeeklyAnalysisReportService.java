package makeus.cmc.malmo.application.service.weekly_analysis_report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.message.RequestWeeklyAnalysisReportMessage;
import makeus.cmc.malmo.adaptor.message.StreamMessageType;
import makeus.cmc.malmo.application.helper.chat_room.PromptQueryHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.helper.notification.MemberNotificationCommandHelper;
import makeus.cmc.malmo.application.helper.outbox.OutboxHelper;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisReportCommandHelper;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisReportQueryHelper;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisReportReservationHelper;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisSourceQueryHelper;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisWeekCalculator;
import makeus.cmc.malmo.application.exception.InvalidWeeklyAnalysisWeekException;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.GenerateWeeklyAnalysisReportUseCase;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.ScheduleWeeklyAnalysisReportUseCase;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.TriggerWeeklyAnalysisReportUseCase;
import makeus.cmc.malmo.application.port.out.chat.LlmReasoningScenario;
import makeus.cmc.malmo.application.port.out.chat.RequestChatApiPort;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.MemberChatRoomMetadata;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReportContent;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
public class WeeklyAnalysisReportService implements ScheduleWeeklyAnalysisReportUseCase, GenerateWeeklyAnalysisReportUseCase, TriggerWeeklyAnalysisReportUseCase {

    private static final Logger log = LoggerFactory.getLogger(WeeklyAnalysisReportService.class);

    private static final String SCHEMA_VERSION = "v1";
    private static final int MAX_TOP_TOPICS = 3;

    private final WeeklyAnalysisWeekCalculator weeklyAnalysisWeekCalculator;
    private final WeeklyAnalysisSourceQueryHelper weeklyAnalysisSourceQueryHelper;
    private final WeeklyAnalysisReportQueryHelper weeklyAnalysisReportQueryHelper;
    private final WeeklyAnalysisReportCommandHelper weeklyAnalysisReportCommandHelper;
    private final WeeklyAnalysisReportReservationHelper weeklyAnalysisReportReservationHelper;
    private final OutboxHelper outboxHelper;
    private final PromptQueryHelper promptQueryHelper;
    private final MemberQueryHelper memberQueryHelper;
    private final MemberNotificationCommandHelper memberNotificationCommandHelper;
    private final RequestChatApiPort requestChatApiPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void scheduleWeeklyAnalysisReports() {
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = weeklyAnalysisWeekCalculator.getPreviousClosedWeek();
        scheduleWeeklyAnalysisReports(weekPeriod);
    }

    @Override
    @Transactional
    public TriggerWeeklyAnalysisReportResponse triggerWeeklyAnalysisReports(TriggerWeeklyAnalysisReportCommand command) {
        validateWeekStartDate(command.getWeekStartDate());
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = weeklyAnalysisWeekCalculator.fromWeekStartDate(command.getWeekStartDate());
        return scheduleWeeklyAnalysisReports(weekPeriod);
    }

    @Override
    public CompletableFuture<Void> generateWeeklyAnalysisReport(GenerateWeeklyAnalysisReportCommand command) {
        return CompletableFuture.supplyAsync(() -> weeklyAnalysisReportCommandHelper.markAsGeneratingIfUpdatable(command.getWeeklyAnalysisReportId()))
                .thenCompose(updated -> {
                    if (!updated) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return CompletableFuture.completedFuture(loadReportOrThrow(command.getWeeklyAnalysisReportId()));
                })
                .thenCompose(report -> {
                    if (report == null || report.getStatus() != WeeklyAnalysisReportStatus.GENERATING) {
                        return CompletableFuture.completedFuture(null);
                    }

                    WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = weeklyAnalysisWeekCalculator.fromWeekStartDate(report.getWeekStartDate());
                    MemberId memberId = report.getMemberId();
                    List<Long> eligibleChatRoomIds = weeklyAnalysisSourceQueryHelper.getEligibleChatRoomIds(memberId, weekPeriod);
                    List<MemberChatRoomMetadata> metadata = weeklyAnalysisSourceQueryHelper.getAllMetadataByChatRoomIds(eligibleChatRoomIds);
                    List<ChatMessage> weeklyUserMessages = weeklyAnalysisSourceQueryHelper.getWeeklyUserMessages(memberId, weekPeriod);
                    List<ChatMessage> fallbackMessages = metadata.size() < Math.max(1, eligibleChatRoomIds.size())
                            ? weeklyAnalysisSourceQueryHelper.getFallbackUserMessages(eligibleChatRoomIds, weekPeriod)
                            : List.of();

                    Prompt weeklyPrompt = promptQueryHelper.getWeeklyReportPrompt();
                    String userInput = buildGenerationInput(memberId, weekPeriod, metadata, fallbackMessages);
                    List<Map<String, String>> messages = List.of(
                            createMessage("system", weeklyPrompt.getContent() + "\n반드시 JSON object로만 응답하고, overview/topTopics/conflict/behaviorPattern/solution 필드를 모두 포함하세요."),
                            createMessage("user", userInput)
                    );

                    return requestChatApiPort.requestJsonResponse(messages, LlmReasoningScenario.SUMMARY)
                            .thenAccept(responseJson -> publishReport(report, weekPeriod, weeklyUserMessages, responseJson));
                })
                .exceptionallyCompose(throwable -> handleGenerationFailure(command.getWeeklyAnalysisReportId(), throwable));
    }

    private WeeklyAnalysisReport loadReportOrThrow(Long weeklyAnalysisReportId) {
        return weeklyAnalysisReportQueryHelper.getWeeklyReport(weeklyAnalysisReportId)
                .orElseThrow(() -> new IllegalArgumentException("Weekly analysis report not found"));
    }

    private TriggerWeeklyAnalysisReportResponse scheduleWeeklyAnalysisReports(WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod) {
        List<Long> candidateMemberIds = weeklyAnalysisSourceQueryHelper.getCandidateMemberIds(weekPeriod);
        int reservedCount = 0;
        int republishedFailedCount = 0;
        int skippedExistingCount = 0;
        int skippedIneligibleCount = 0;

        for (Long candidateMemberId : candidateMemberIds) {
            MemberId memberId = MemberId.of(candidateMemberId);
            WeeklyAnalysisReport existingReport = weeklyAnalysisReportQueryHelper
                    .getWeeklyReport(memberId, weekPeriod.getWeekStartDate())
                    .orElse(null);
            if (existingReport != null) {
                if (existingReport.getStatus() == WeeklyAnalysisReportStatus.FAILED) {
                    outboxHelper.publish(
                            StreamMessageType.REQUEST_WEEKLY_ANALYSIS_REPORT,
                            new RequestWeeklyAnalysisReportMessage(existingReport.getId())
                    );
                    republishedFailedCount++;
                } else {
                    skippedExistingCount++;
                }
                continue;
            }

            long sourceUserMessageCount = weeklyAnalysisSourceQueryHelper.countWeeklyUserMessages(memberId, weekPeriod);
            List<Long> eligibleChatRoomIds = weeklyAnalysisSourceQueryHelper.getEligibleChatRoomIds(memberId, weekPeriod);
            if (sourceUserMessageCount < 1 || eligibleChatRoomIds.isEmpty()) {
                skippedIneligibleCount++;
                continue;
            }

            WeeklyAnalysisReport weeklyAnalysisReport = WeeklyAnalysisReport.createPendingReport(
                    memberId,
                    weekPeriod.getWeekStartDate(),
                    weekPeriod.getWeekEndDate(),
                    eligibleChatRoomIds.size(),
                    eligibleChatRoomIds.size(),
                    Math.toIntExact(sourceUserMessageCount)
            );
            try {
                weeklyAnalysisReportReservationHelper.reservePendingReport(weeklyAnalysisReport);
                reservedCount++;
            } catch (DataIntegrityViolationException exception) {
                log.info(
                        "weekly_report_reservation_conflict memberId={} weekStartDate={}",
                        memberId.getValue(),
                        weekPeriod.getWeekStartDate()
                );
                skippedExistingCount++;
            }
        }

        return new TriggerWeeklyAnalysisReportResponse(
                weekPeriod.getWeekStartDate(),
                weekPeriod.getWeekEndDate(),
                candidateMemberIds.size(),
                reservedCount,
                republishedFailedCount,
                skippedExistingCount,
                skippedIneligibleCount
        );
    }

    private void validateWeekStartDate(java.time.LocalDate weekStartDate) {
        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new InvalidWeeklyAnalysisWeekException();
        }
    }

    private CompletableFuture<Void> handleGenerationFailure(Long weeklyAnalysisReportId, Throwable throwable) {
        WeeklyAnalysisReport failedReport = loadReportOrThrow(weeklyAnalysisReportId);
        failedReport.markFailed(resolveFailureReason(throwable));
        weeklyAnalysisReportCommandHelper.saveWeeklyAnalysisReport(failedReport);
        return CompletableFuture.failedFuture(throwable);
    }

    @Transactional
    protected void publishReport(
            WeeklyAnalysisReport generatingReport,
            WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod,
            List<ChatMessage> weeklyUserMessages,
            String responseJson
    ) {
        WeeklyAnalysisReportContent content = buildContent(weekPeriod, weeklyUserMessages, responseJson);
        generatingReport.markPublished(content, LocalDateTime.now(WeeklyAnalysisWeekCalculator.WEEKLY_REPORT_ZONE_ID));
        weeklyAnalysisReportCommandHelper.saveWeeklyAnalysisReport(generatingReport);
        try {
            memberNotificationCommandHelper.createAndSaveWeeklyAnalysisReportPublishedNotification(
                    generatingReport.getMemberId(),
                    generatingReport.getWeekStartDate(),
                    generatingReport.getWeekEndDate()
            );
        } catch (RuntimeException exception) {
            log.warn("weekly_report_notification_failed reportId={}", generatingReport.getId(), exception);
        }
    }

    private String buildGenerationInput(
            MemberId memberId,
            WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod,
            List<MemberChatRoomMetadata> metadata,
            List<ChatMessage> fallbackMessages
    ) {
        MemberQueryHelper.MemberInfoDto memberInfo = memberQueryHelper.getMemberInfoOrThrow(memberId);
        StringBuilder builder = new StringBuilder();
        builder.append("[period]\n")
                .append(weekPeriod.getWeekStartDate()).append(" ~ ").append(weekPeriod.getWeekEndDate()).append("\n\n")
                .append("[member]\n")
                .append("nickname=").append(memberInfo.getNickname()).append("\n")
                .append("relationshipStatus=").append(memberInfo.getRelationshipStatus()).append("\n")
                .append("loveTypeCategory=").append(memberInfo.getLoveTypeCategory()).append("\n")
                .append("personalityType=").append(memberInfo.getPersonalityType()).append("\n\n")
                .append("[metadata]\n");

        for (MemberChatRoomMetadata item : metadata) {
            builder.append("[chatRoom ")
                    .append(item.getChatRoomId().getValue())
                    .append("] level=")
                    .append(item.getLevel())
                    .append(" detailedLevel=")
                    .append(item.getDetailedLevel())
                    .append(" title=")
                    .append(item.getTitle())
                    .append(" summary=")
                    .append(item.getSummary())
                    .append(" createdAt=")
                    .append(item.getCreatedAt())
                    .append("\n");
        }

        if (!fallbackMessages.isEmpty()) {
            builder.append("\n[fallbackMessages]\n");
            for (ChatMessage chatMessage : fallbackMessages) {
                builder.append("[chatRoom ")
                        .append(chatMessage.getChatRoomId().getValue())
                        .append("] ")
                        .append(chatMessage.getCreatedAt())
                        .append(" :: ")
                        .append(chatMessage.getContent())
                        .append("\n");
            }
        }
        return builder.toString();
    }

    private Map<String, String> createMessage(String role, String content) {
        return Map.of("role", role, "content", content);
    }

    private WeeklyAnalysisReportContent buildContent(
            WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod,
            List<ChatMessage> weeklyUserMessages,
            String responseJson
    ) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            return new WeeklyAnalysisReportContent(
                    SCHEMA_VERSION,
                    new WeeklyAnalysisReportContent.Period(
                            weekPeriod.getWeekStartDate(),
                            weekPeriod.getWeekEndDate(),
                            WeeklyAnalysisWeekCalculator.WEEKLY_REPORT_ZONE_ID.getId()
                    ),
                    convertObject(root, "overview", WeeklyAnalysisReportContent.Overview.class),
                    limitTopTopics(convertArray(root, "topTopics", WeeklyAnalysisReportContent.TopTopic.class)),
                    calculateMoodByTime(weeklyUserMessages),
                    normalizeConflict(convertObject(root, "conflict", WeeklyAnalysisReportContent.Conflict.class)),
                    convertObject(root, "behaviorPattern", WeeklyAnalysisReportContent.BehaviorPattern.class),
                    convertObject(root, "solution", WeeklyAnalysisReportContent.Solution.class)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse weekly analysis report JSON", e);
        }
    }

    private <T> T convertObject(JsonNode root, String fieldName, Class<T> targetType) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(fieldName + " must be an object");
        }
        return objectMapper.convertValue(node, targetType);
    }

    private <T> List<T> convertArray(JsonNode root, String fieldName, Class<T> elementType) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isArray()) {
            throw new IllegalArgumentException(fieldName + " must be an array");
        }
        List<T> values = new ArrayList<>();
        for (JsonNode itemNode : node) {
            values.add(objectMapper.convertValue(itemNode, elementType));
        }
        return values;
    }

    private List<WeeklyAnalysisReportContent.TopTopic> limitTopTopics(List<WeeklyAnalysisReportContent.TopTopic> topTopics) {
        if (topTopics.isEmpty()) {
            throw new IllegalArgumentException("topTopics must not be empty");
        }
        return topTopics.stream().limit(MAX_TOP_TOPICS).toList();
    }

    private WeeklyAnalysisReportContent.Conflict normalizeConflict(WeeklyAnalysisReportContent.Conflict conflict) {
        int normalizedScore = Math.max(0, Math.min(100, conflict.score()));
        return new WeeklyAnalysisReportContent.Conflict(normalizedScore, conflict.description());
    }

    private WeeklyAnalysisReportContent.MoodByTime calculateMoodByTime(List<ChatMessage> weeklyUserMessages) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("MORNING", 0);
        counts.put("AFTERNOON", 0);
        counts.put("EVENING", 0);
        counts.put("LATE_NIGHT", 0);

        for (ChatMessage message : weeklyUserMessages) {
            String bucket = toMoodBucket(message.getCreatedAt().getHour());
            counts.put(bucket, counts.get(bucket) + 1);
        }

        int totalCount = counts.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Double> ratios = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double ratio = totalCount == 0 ? 0.0 : (double) entry.getValue() / totalCount;
            ratios.put(entry.getKey(), ratio);
        }

        String dominantPeriod = counts.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry<String, Integer>::getValue)
                        .thenComparing(entry -> dominantPriority(entry.getKey())))
                .map(Map.Entry::getKey)
                .orElse("EVENING");

        return new WeeklyAnalysisReportContent.MoodByTime(
                dominantPeriod,
                new WeeklyAnalysisReportContent.MoodRatios(
                        ratios.get("MORNING"),
                        ratios.get("AFTERNOON"),
                        ratios.get("EVENING"),
                        ratios.get("LATE_NIGHT")
                ),
                moodDescription(dominantPeriod)
        );
    }

    private String toMoodBucket(int hour) {
        if (hour >= 6 && hour < 12) {
            return "MORNING";
        }
        if (hour >= 12 && hour < 18) {
            return "AFTERNOON";
        }
        if (hour >= 18) {
            return "EVENING";
        }
        return "LATE_NIGHT";
    }

    private int dominantPriority(String bucket) {
        return switch (bucket) {
            case "LATE_NIGHT" -> 4;
            case "EVENING" -> 3;
            case "MORNING" -> 2;
            default -> 1;
        };
    }

    private String moodDescription(String dominantPeriod) {
        return switch (dominantPeriod) {
            case "LATE_NIGHT" -> "주로 늦은 밤 시간대에 고민이 커지는 흐름이 보여요.";
            case "EVENING" -> "주로 저녁 시간대에 감정과 고민이 집중되는 흐름이 보여요.";
            case "MORNING" -> "주로 오전 시간대에 생각이 많아지는 흐름이 보여요.";
            default -> "주로 오후 시간대에 고민이 쌓이는 흐름이 보여요.";
        };
    }

    private String resolveFailureReason(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return message.length() > 255 ? message.substring(0, 255) : message;
    }
}
