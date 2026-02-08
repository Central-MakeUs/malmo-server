package makeus.cmc.malmo.application.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.in.aop.CheckValidMember;
import makeus.cmc.malmo.adaptor.message.StreamChatMessage;
import makeus.cmc.malmo.adaptor.message.StreamMessageType;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomCommandHelper;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomQueryHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.helper.outbox.OutboxHelper;
import makeus.cmc.malmo.application.port.in.chat.SendChatMessageUseCase;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.service.ChatRoomDomainService;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.util.ChatMessageSplitter;
import makeus.cmc.malmo.util.GlobalConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

//import static makeus.cmc.malmo.util.GlobalConstants.FINAL_MESSAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService implements SendChatMessageUseCase {

    private final ChatRoomDomainService chatRoomDomainService;
    private final ChatSseSender chatSseSender;

    private final MemberQueryHelper memberQueryHelper;
    private final ChatRoomQueryHelper chatRoomQueryHelper;
    private final ChatRoomCommandHelper chatRoomCommandHelper;

    private final OutboxHelper outboxHelper;

    @Override
    @Transactional
    @CheckValidMember
    public SendChatMessageResponse processUserMessage(SendChatMessageCommand command) {
        MemberId memberId = MemberId.of(command.getUserId());
        ChatRoomId chatRoomId = ChatRoomId.of(command.getChatRoomId());
        
        // 명시적 채팅방 ID로 조회 및 소유권 검증
        chatRoomQueryHelper.validateChatRoomOwnership(memberId, chatRoomId);
        chatRoomQueryHelper.validateChatRoomActive(chatRoomId);
        
        Member member = memberQueryHelper.getMemberByIdOrThrow(memberId);
        ChatRoom chatRoom = chatRoomQueryHelper.getChatRoomByIdOrThrow(chatRoomId);

        // BEFORE_INIT 상태인 경우 ALIVE로 전환 (첫 메시지 시)
        if (chatRoom.isBeforeInit()) {
            chatRoom.initialize();
            chatRoomCommandHelper.saveChatRoom(chatRoom);
            log.info("채팅방 상태 전환: BEFORE_INIT -> ALIVE, chatRoomId={}", chatRoomId.getValue());
        }

        // 첫 메시지이고 애착 유형 미진단인 경우 시스템 메시지 추가
        boolean hasUserMessages = chatRoomQueryHelper.hasUserMessages(chatRoomId);
        if (!hasUserMessages && member.getLoveTypeCategory() == null) {
            ChatMessage systemMessage = chatRoomDomainService.createSystemMessage(
                    chatRoomId,
                    chatRoom.getLevel(),
                    chatRoom.getDetailedLevel(),
                    GlobalConstants.ATTACHMENT_TYPE_PROMPT_MESSAGE
            );
            chatRoomCommandHelper.saveChatMessage(systemMessage);
        }

        // 현재 유저 메시지를 저장
        ChatMessage savedUserMessage = saveUserMessage(chatRoom, command.getMessage());

        // 채팅방의 마지막 메시지 전송 시간 갱신
        chatRoom.updateLastMessageSentTime();
        chatRoomCommandHelper.saveChatRoom(chatRoom);

        // 채팅 응답 API 요청 스트림에 추가
        outboxHelper.publish(
                StreamMessageType.REQUEST_CHAT_MESSAGE,
                new StreamChatMessage(
                        member.getId(),
                        chatRoom.getId(),
                        command.getMessage(),
                        chatRoom.getLevel(),
                        chatRoom.getDetailedLevel()
                )
        );

        return SendChatMessageResponse.builder()
                .messageId(savedUserMessage.getId())
                .build();
    }

    private ChatMessage saveUserMessage(ChatRoom chatRoom, String message) {
        ChatMessage userMessage = chatRoomDomainService.createUserMessage(
                ChatRoomId.of(chatRoom.getId()), 
                chatRoom.getLevel(), 
                chatRoom.getDetailedLevel(),
                message);
        return chatRoomCommandHelper.saveChatMessage(userMessage);
    }

    private void saveAiMessage(MemberId memberId, ChatRoomId chatRoomId, int level, int detailedLevel, String fullAnswer) {
        // fullAnswer를 문장 단위로 분할하고 세 문장씩 그룹화
        List<String> groupedTexts = ChatMessageSplitter.splitIntoGroups(fullAnswer);
        
        // 각 그룹을 ChatMessage로 생성
        List<ChatMessage> chatMessages = groupedTexts.stream()
                .map(groupText -> chatRoomDomainService.createAiMessage(chatRoomId, level, detailedLevel, groupText))
                .collect(Collectors.toList());
        
        // bulk 저장
        List<ChatMessage> savedMessages = chatRoomCommandHelper.saveChatMessages(chatMessages);
        
        // 저장된 메시지들의 ID 리스트 추출
        List<Long> messageIds = savedMessages.stream()
                .map(ChatMessage::getId)
                .collect(Collectors.toList());
        
        // SSE로 ID 리스트 전송
        chatSseSender.sendAiResponseIds(memberId, messageIds);
    }
}
