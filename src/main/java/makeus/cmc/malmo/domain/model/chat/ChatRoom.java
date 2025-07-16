package makeus.cmc.malmo.domain.model.chat;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.ChatRoomState;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class ChatRoom {
    private Long id;
    private MemberId memberId;
    private ChatRoomState chatRoomState;

    private boolean isCurrentPromptForMetadata;
    private int level;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    public static ChatRoom createChatRoom(MemberId memberId, int level, boolean isCurrentPromptForMetadata) {
        return ChatRoom.builder()
                .memberId(memberId)
                .level(level)
                .isCurrentPromptForMetadata(isCurrentPromptForMetadata)
                .chatRoomState(ChatRoomState.ALIVE)
                .build();
    }

    public static ChatRoom from(Long id, MemberId memberId, ChatRoomState chatRoomState,
                                boolean isCurrentPromptForMetadata, int level,
                                LocalDateTime createdAt, LocalDateTime modifiedAt, LocalDateTime deletedAt) {
        return ChatRoom.builder()
                .id(id)
                .memberId(memberId)
                .chatRoomState(chatRoomState)
                .isCurrentPromptForMetadata(isCurrentPromptForMetadata)
                .level(level)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public void updateCurrentPromptStateNotForMetadata() {
        this.isCurrentPromptForMetadata = false;
    }

    public void updateChatRoomStatePaused() {
        this.chatRoomState = ChatRoomState.PAUSED;
    }

    public void updateChatRoomStateNeedNextQuestion() {
        this.level += 1;
        this.chatRoomState = ChatRoomState.NEED_NEXT_QUESTION;
    }

    public void updateChatRoomStateAlive() {
        this.chatRoomState = ChatRoomState.ALIVE;
    }
}
