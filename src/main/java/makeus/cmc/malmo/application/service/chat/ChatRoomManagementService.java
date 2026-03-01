package makeus.cmc.malmo.application.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.in.aop.CheckValidMember;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomCommandHelper;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomQueryHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.port.in.chat.CreateChatRoomUseCase;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.service.ChatRoomDomainService;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.util.JosaUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static makeus.cmc.malmo.util.GlobalConstants.INIT_CHATROOM_LEVEL;
import static makeus.cmc.malmo.util.GlobalConstants.INIT_CHAT_MESSAGE_FIRST;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomManagementService implements CreateChatRoomUseCase {

    private final ChatRoomDomainService chatRoomDomainService;
    private final MemberQueryHelper memberQueryHelper;
    private final ChatRoomQueryHelper chatRoomQueryHelper;
    private final ChatRoomCommandHelper chatRoomCommandHelper;

    @Override
    @Transactional
    @CheckValidMember
    public CreateChatRoomResponse createChatRoom(CreateChatRoomCommand command) {
        MemberId memberId = MemberId.of(command.getUserId());
        Member member = memberQueryHelper.getMemberByIdOrThrow(memberId);
        
        Optional<ChatRoom> existingBeforeInitRoom = chatRoomQueryHelper.getBeforeInitChatRoomByMemberId(memberId);
        if (existingBeforeInitRoom.isPresent()) {
            ChatRoom existingRoom = existingBeforeInitRoom.get();
            log.info("기존 BEFORE_INIT 채팅방 반환: chatRoomId={}, memberId={}", existingRoom.getId(), memberId.getValue());
            return CreateChatRoomResponse.builder()
                    .chatRoomId(existingRoom.getId())
                    .chatRoomState(existingRoom.getChatRoomState())
                    .createdAt(existingRoom.getCreatedAt())
                    .build();
        }
        
        ChatRoom chatRoom = chatRoomDomainService.createChatRoom(memberId);
        ChatRoom savedChatRoom = chatRoomCommandHelper.saveChatRoom(chatRoom);

        // 첫 번째 AI 메시지: nickname아 안녕!
        ChatMessage firstMessage = chatRoomDomainService.createAiMessage(
                ChatRoomId.of(savedChatRoom.getId()),
                INIT_CHATROOM_LEVEL,
                1,
                JosaUtils.아야(member.getNickname()) + INIT_CHAT_MESSAGE_FIRST,
                Objects.requireNonNullElse(savedChatRoom.getCreatedAt(), LocalDateTime.now()));
        chatRoomCommandHelper.saveChatMessage(firstMessage);

        log.info("새 BEFORE_INIT 채팅방 생성: chatRoomId={}, memberId={}", savedChatRoom.getId(), memberId.getValue());
        
        return CreateChatRoomResponse.builder()
                .chatRoomId(savedChatRoom.getId())
                .chatRoomState(savedChatRoom.getChatRoomState())
                .createdAt(savedChatRoom.getCreatedAt())
                .build();
    }

}
