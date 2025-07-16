package makeus.cmc.malmo.application.port.out;

import lombok.AllArgsConstructor;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.id.MemberId;

public interface SendSseEventPort {
    void sendToMember(MemberId memberId, NotificationEvent event);

    @Getter
    @AllArgsConstructor
    class NotificationEvent {
        private SseEventType eventType;
        private Object data;
    }

    @Getter
    @AllArgsConstructor
    enum SseEventType {
        COUPLE_CONNECTED("couple_connected"),
        CHAT_RESPONSE("chat_response"),
        AI_RESPONSE_ID("ai_response_id"),
        CHAT_ROOM_PAUSED("chat_room_paused"),
        CURRENT_LEVEL_FINISHED("current_level_finished");

        private final String eventName;
    }
}

