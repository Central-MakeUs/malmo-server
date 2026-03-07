package makeus.cmc.malmo.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.state.OutboxState;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class Outbox {

    private Long id;

    private String type;
    private String payload;

    private int retryCount;
    private OutboxState state;
    private String messageId;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    public boolean isPending() {
        return this.state == OutboxState.PENDING;
    }

    public boolean isDone() {
        return this.state == OutboxState.DONE;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markAsSent(String messageId) {
        this.state = OutboxState.SENT;
        this.messageId = messageId;
    }

    public void markAsFailed() {
        this.state = OutboxState.FAILED;
    }

    public void markAsDone() {
        this.state = OutboxState.DONE;
    }

    public void markAsDead() {
        this.state = OutboxState.DEAD;
    }

    public boolean isRetryable(int maxRetryCount) {
        return this.retryCount < maxRetryCount && this.state != OutboxState.DEAD;
    }

    public static Outbox create(String type, String payload) {
        return Outbox.builder()
                .type(type)
                .payload(payload)
                .retryCount(0)
                .state(OutboxState.PENDING)
                .build();
    }

    public static Outbox from(Long id, String type, String payload, int retryCount, OutboxState state, String messageId, LocalDateTime createdAt, LocalDateTime modifiedAt, LocalDateTime deletedAt) {
        return Outbox.builder()
                .id(id)
                .type(type)
                .payload(payload)
                .retryCount(retryCount)
                .state(state)
                .messageId(messageId)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }
}
