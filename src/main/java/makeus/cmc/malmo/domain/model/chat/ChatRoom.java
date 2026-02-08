package makeus.cmc.malmo.domain.model.chat;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.ChatRoomCompletedReason;
import makeus.cmc.malmo.domain.value.state.ChatRoomState;

import java.time.LocalDateTime;
import java.util.Objects;

import static makeus.cmc.malmo.util.GlobalConstants.INIT_CHATROOM_LEVEL;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class ChatRoom {
    private Long id;
    private MemberId memberId;
    private ChatRoomState chatRoomState;
    private int level;
    private int detailedLevel;
    private LocalDateTime lastMessageSentTime;
    private String title;  // 채팅방 제목 (1단계 종료 후 생성)
    
    // 유지: 기존 COMPLETED 채팅방 보고서 조회용 필드들
    private String totalSummary;
    private String situationKeyword;
    private String solutionKeyword;
    private ChatRoomCompletedReason chatRoomCompletedReason;
    private String counselingType;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    public static ChatRoom createChatRoom(MemberId memberId) {
        return ChatRoom.builder()
                .memberId(memberId)
                .level(INIT_CHATROOM_LEVEL)
                .detailedLevel(1)
                .chatRoomState(ChatRoomState.BEFORE_INIT)
                .lastMessageSentTime(LocalDateTime.now())
                .title(null)  // 제목은 1단계 종료 후 생성
                // 새 채팅방은 보고서 관련 필드 null
                .totalSummary(null)
                .situationKeyword(null)
                .solutionKeyword(null)
                .chatRoomCompletedReason(null)
                .counselingType(null)
                .build();
    }

    public static ChatRoom from(Long id, MemberId memberId, ChatRoomState chatRoomState,
                                int level, int detailedLevel, LocalDateTime lastMessageSentTime,
                                String title,
                                String totalSummary, String situationKeyword, String solutionKeyword,
                                ChatRoomCompletedReason chatRoomCompletedReason, String counselingType,
                                LocalDateTime createdAt, LocalDateTime modifiedAt, LocalDateTime deletedAt) {
        return ChatRoom.builder()
                .id(id)
                .memberId(memberId)
                .chatRoomState(chatRoomState)
                .level(level)
                .detailedLevel(detailedLevel)
                .lastMessageSentTime(lastMessageSentTime)
                .title(title)
                // 기존 데이터 매핑용
                .totalSummary(totalSummary)
                .situationKeyword(situationKeyword)
                .solutionKeyword(solutionKeyword)
                .chatRoomCompletedReason(chatRoomCompletedReason)
                .counselingType(counselingType)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public void upgradeDetailedLevel() {
        this.detailedLevel += 1;
    }

    public void upgradeToNextStage() {
        this.level += 1;
        this.detailedLevel = 1;
    }

    public void updateLastMessageSentTime() {
        this.lastMessageSentTime = LocalDateTime.now();
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public boolean isChatRoomValid() {
        return this.chatRoomState == ChatRoomState.ALIVE 
            || this.chatRoomState == ChatRoomState.BEFORE_INIT;
    }

    public boolean isBeforeInit() {
        return this.chatRoomState == ChatRoomState.BEFORE_INIT;
    }

    public void initialize() {
        if (this.chatRoomState == ChatRoomState.BEFORE_INIT) {
            this.chatRoomState = ChatRoomState.ALIVE;
        }
    }

    public boolean isOwner(MemberId memberId) {
        return Objects.equals(this.memberId.getValue(), memberId.getValue());
    }

    public void softDelete() {
        this.chatRoomState = ChatRoomState.DELETED;
    }

    // 기존 보고서가 있는지 확인
    public boolean hasReport() {
        return this.chatRoomState == ChatRoomState.COMPLETED 
            && this.totalSummary != null;
    }
}
