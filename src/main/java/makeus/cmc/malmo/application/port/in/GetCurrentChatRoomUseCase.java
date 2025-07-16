package makeus.cmc.malmo.application.port.in;

import lombok.Builder;
import lombok.Data;
import makeus.cmc.malmo.domain.value.state.ChatRoomState;

public interface GetCurrentChatRoomUseCase {

    GetCurrentChatRoomResponse getCurrentChatRoom(GetCurrentChatRoomCommand command);

    @Data
    @Builder
    class GetCurrentChatRoomCommand {
        private Long userId;
    }

    @Data
    @Builder
    class GetCurrentChatRoomResponse {
        private ChatRoomState chatRoomStatus;
    }
}
