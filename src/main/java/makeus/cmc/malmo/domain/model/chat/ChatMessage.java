package makeus.cmc.malmo.domain.model.chat;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.type.SenderType;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class ChatMessage {
    private Long id;
    private ChatRoomId chatRoomId;
    private boolean isImage;
    private String imageUrl;
    private String extractedText;
    private String content;
    private SenderType senderType;
    private boolean isSummarized;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    public static ChatMessage createUserTextMessage(ChatRoomId chatRoomId, String content) {
        return ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .isImage(false)
                .imageUrl(null)
                .extractedText(null)
                .content(content)
                .senderType(SenderType.USER)
                .isSummarized(false)
                .build();
    }

    public static ChatMessage createAssistantTextMessage(ChatRoomId chatRoomId, String content) {
        return ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .isImage(false)
                .imageUrl(null)
                .extractedText(null)
                .content(content)
                .senderType(SenderType.ASSISTANT)
                .isSummarized(false)
                .build();
    }

    public static ChatMessage from(Long id, ChatRoomId chatRoomId, boolean isImage, String imageUrl, String extractedText, String content, SenderType senderType, boolean isSummarized, LocalDateTime createdAt, LocalDateTime modifiedAt, LocalDateTime deletedAt) {
        return ChatMessage.builder()
                .id(id)
                .chatRoomId(chatRoomId)
                .isImage(isImage)
                .imageUrl(imageUrl)
                .extractedText(extractedText)
                .content(content)
                .senderType(senderType)
                .isSummarized(isSummarized)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }

}