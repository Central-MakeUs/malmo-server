package makeus.cmc.malmo.adaptor.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.message.StreamMessageType;
import makeus.cmc.malmo.application.port.in.MarkOutboxUseCase;
import makeus.cmc.malmo.application.port.in.chat.ProcessMessageUseCase;
import makeus.cmc.malmo.application.port.in.weekly_analysis_report.GenerateWeeklyAnalysisReportUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamConsumer {

    @Value("${spring.data.redis.stream-key}")
    private String streamKey;

    @Value("${spring.data.redis.consumer-group}")
    private String consumerGroup;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProcessMessageUseCase processMessageUseCase;
    private final GenerateWeeklyAnalysisReportUseCase generateWeeklyAnalysisReportUseCase;
    private final ObjectMapper objectMapper;

    private final MarkOutboxUseCase markOutboxUseCase;

    @PostConstruct
    public void init() {
        try {
            // Consumer Group 생성 (이미 존재하면 무시)
            try {
                redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), consumerGroup);
                log.info("Created consumer group: {}", consumerGroup);
            } catch (Exception e) {
                log.debug("Consumer group already exists or stream doesn't exist yet");
            }
        } catch (Exception e) {
            log.error("Error creating consumer group", e);
        }
    }

    public void onMessage(MapRecord<String, String, String> record) {
        try {
            String type = record.getValue().get("type");
            String payloadJson = record.getValue().get("payload");
            JsonNode payloadNode = objectMapper.readTree(payloadJson);

            log.info("Processing record type={}, id={}", type, record.getId());

            CompletableFuture<Void> future;
            switch (StreamMessageType.valueOf(type)) {
                case REQUEST_CHAT_MESSAGE:
                    future = processChatMessage(payloadNode);
                    break;
                case REQUEST_EXTRACT_METADATA:
                    future = processMetadata(payloadNode);
                    break;
                case REQUEST_TITLE_GENERATION:
                    future = processTitleGeneration(payloadNode);
                    break;
                case REQUEST_CONVERSATION_SUMMARY:
                    future = processConversationSummary(payloadNode);
                    break;
                case REQUEST_WEEKLY_ANALYSIS_REPORT:
                    future = processWeeklyAnalysisReport(payloadNode);
                    break;
                default:
                    log.warn("Unknown message type: {}", type);
                    // 알 수 없는 타입은 바로 ACK 처리
                    redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
                    return;
            }

            // 비동기 작업이 완료되었을 때의 처리
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    // 비동기 작업 중 예외 발생 시
                    log.error("Error processing record {} asynchronously", record.getId(), throwable);
                    handleFailedMessage(record); // 실패 처리 로직 (DLQ 등)
                } else {
                    // 성공적으로 완료 시 ACK
                    redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
                    markOutboxUseCase.markOutboxDone(Long.valueOf(record.getValue().get("outboxId")));
                    log.info("Successfully processed and acknowledged record id={}", record.getId());
                }
            });

        } catch (Exception e) {
            // onMessage 메서드 자체에서 동기적으로 에러 발생 시
            log.error("Error processing record {}", record.getId(), e);
            handleFailedMessage(record);
        }
    }

    private CompletableFuture<Void> processChatMessage(JsonNode payloadNode) {
        return processMessageUseCase.processStreamChatMessage(
                ProcessMessageUseCase.ProcessMessageCommand.builder()
                        .memberId(payloadNode.get("memberId").asLong())
                        .chatRoomId(payloadNode.get("chatRoomId").asLong())
                        .nowMessage(payloadNode.get("nowMessage").asText())
                        .promptLevel(payloadNode.get("promptLevel").asInt())
                        .detailedLevel(payloadNode.get("detailedLevel").asInt())
                        .build()
        );
    }

    private CompletableFuture<Void> processMetadata(JsonNode payloadNode) {
        return processMessageUseCase.processAnswerMetadata(
                ProcessMessageUseCase.ProcessAnswerCommand.builder()
                        .coupleId(payloadNode.get("coupleId").asLong())
                        .memberId(payloadNode.get("memberId").asLong())
                        .coupleQuestionId(payloadNode.get("coupleQuestionId").asLong())
                        .build()
        );
    }

    private CompletableFuture<Void> processTitleGeneration(JsonNode payloadNode) {
        return processMessageUseCase.processTitleGeneration(
                ProcessMessageUseCase.ProcessTitleGenerationCommand.builder()
                        .chatRoomId(payloadNode.get("chatRoomId").asLong())
                        .build()
        );
    }

    private CompletableFuture<Void> processConversationSummary(JsonNode payloadNode) {
        return processMessageUseCase.processConversationSummary(
                ProcessMessageUseCase.ProcessConversationSummaryCommand.builder()
                        .chatRoomId(payloadNode.get("chatRoomId").asLong())
                        .level(payloadNode.get("level").asInt())
                        .build()
        );
    }

    private CompletableFuture<Void> processWeeklyAnalysisReport(JsonNode payloadNode) {
        return generateWeeklyAnalysisReportUseCase.generateWeeklyAnalysisReport(
                new GenerateWeeklyAnalysisReportUseCase.GenerateWeeklyAnalysisReportCommand(
                        payloadNode.get("weeklyAnalysisReportId").asLong()
                )
        );
    }

    private void handleFailedMessage(MapRecord<String, String, String> record) {
        try {
            // 현재 retry 횟수 확인
            Object retryCountObj = record.getValue().get("retry");
            int retryCount = retryCountObj != null ? Integer.parseInt(String.valueOf(retryCountObj)) : 0;

            int maxRetries = 2;

            if (retryCount < maxRetries) {
                // retry count 증가
                retryCount++;

                // 새로운 메시지 생성 (기존 데이터 + retry count 업데이트)
                ObjectRecord<String, Map<String, String>> retryRecord = StreamRecords.objectBacked(record.getValue())
                        .withStreamKey(streamKey);

                // retryCount 필드 추가/업데이트
                retryRecord.getValue().put("type", record.getValue().get("type"));
                retryRecord.getValue().put("payload", record.getValue().get("payload"));
                retryRecord.getValue().put("outboxId", record.getValue().get("outboxId"));
                retryRecord.getValue().put("retry", String.valueOf(retryCount));
                retryRecord.getValue().put("originalId", record.getId().getValue());

                // Stream에 다시 publish
                redisTemplate.opsForStream().add(retryRecord);

                log.info("Retry message published - originalId: {}, retryCount: {}, outboxId: {}",
                        record.getId(), retryCount, record.getValue().get("outboxId"));
            } else {
                log.error("Maximum retry count exceeded for message: {}", record.getId());
                // DLQ에 실패한 메시지 추가
                String dlqKey = streamKey + ":dlq";
                ObjectRecord<String, Map<String, String>> dlqRecord = StreamRecords.objectBacked(record.getValue())
                        .withStreamKey(dlqKey);
                dlqRecord.getValue().put("failedAt", String.valueOf(System.currentTimeMillis()));
                dlqRecord.getValue().put("originalId", record.getId().getValue());

                redisTemplate.opsForStream().add(dlqRecord);
                markOutboxUseCase.markOutboxFailed(Long.valueOf(record.getValue().get("outboxId")));
            }

            // 기존 메시지는 ACK 처리해서 PEL에서 제거
            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
        } catch (Exception e) {
            log.error("Error handling failed message: {}", record.getId(), e);
        }
    }
}
