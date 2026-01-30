package makeus.cmc.malmo.application.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.in.aop.CheckValidMember;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.port.in.chat.CreateChatRoomUseCase;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.service.ChatRoomDomainService;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.RelationshipStatus;
import makeus.cmc.malmo.util.JosaUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static makeus.cmc.malmo.util.GlobalConstants.INIT_CHATROOM_LEVEL;
import static makeus.cmc.malmo.util.GlobalConstants.INIT_CHAT_MESSAGE_FIRST;
import static makeus.cmc.malmo.util.GlobalConstants.INIT_CHAT_MESSAGE_SECOND_SEEING_SOMEONE;
import static makeus.cmc.malmo.util.GlobalConstants.INIT_CHAT_MESSAGE_SECOND_IN_RELATIONSHIP;
import static makeus.cmc.malmo.util.GlobalConstants.INIT_CHAT_MESSAGE_SECOND_BREAKUP;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomManagementService implements CreateChatRoomUseCase {

    private final ChatRoomDomainService chatRoomDomainService;
    private final MemberQueryHelper memberQueryHelper;
    private final ChatRoomCommandHelper chatRoomCommandHelper;

    @Override
    @Transactional
    @CheckValidMember
    public CreateChatRoomResponse createChatRoom(CreateChatRoomCommand command) {
        MemberId memberId = MemberId.of(command.getUserId());
        Member member = memberQueryHelper.getMemberByIdOrThrow(memberId);
        
        // 채팅방 생성 (즉시 ALIVE 상태)
        ChatRoom chatRoom = chatRoomDomainService.createChatRoom(memberId);
        ChatRoom savedChatRoom = chatRoomCommandHelper.saveChatRoom(chatRoom);

        LocalDateTime now = LocalDateTime.now();

        // 초기 AI 메시지 2개 생성 및 저장
        // 첫 번째 메시지: nickname아 안녕!
        ChatMessage firstMessage = chatRoomDomainService.createAiMessage(
                ChatRoomId.of(savedChatRoom.getId()),
                INIT_CHATROOM_LEVEL,
                1,
                JosaUtils.아야(member.getNickname()) + INIT_CHAT_MESSAGE_FIRST,
                now);
        chatRoomCommandHelper.saveChatMessage(firstMessage);

        // 두 번째 메시지: 나는 연애 고민 상담사 모모야.~ (1초 뒤 시간으로 저장)
        String secondMessageContent = getSecondMessageByRelationshipStatus(member.getRelationshipStatus());
        ChatMessage secondMessage = chatRoomDomainService.createAiMessage(
                ChatRoomId.of(savedChatRoom.getId()),
                INIT_CHATROOM_LEVEL,
                1,
                secondMessageContent,
                now.plusSeconds(1));
        chatRoomCommandHelper.saveChatMessage(secondMessage);
        
        log.info("새 채팅방 생성: chatRoomId={}, memberId={}", savedChatRoom.getId(), memberId.getValue());
        
        return CreateChatRoomResponse.builder()
                .chatRoomId(savedChatRoom.getId())
                .chatRoomState(savedChatRoom.getChatRoomState())
                .createdAt(savedChatRoom.getCreatedAt())
                .build();
    }

    String getSecondMessageByRelationshipStatus(RelationshipStatus status) {
        if (status == null) {
            return INIT_CHAT_MESSAGE_SECOND_SEEING_SOMEONE;
        }
        return switch (status) {
            case SEEING_SOMEONE -> INIT_CHAT_MESSAGE_SECOND_SEEING_SOMEONE;
            case IN_RELATIONSHIP -> INIT_CHAT_MESSAGE_SECOND_IN_RELATIONSHIP;
            case BREAKUP -> INIT_CHAT_MESSAGE_SECOND_BREAKUP;
        };
    }
}
