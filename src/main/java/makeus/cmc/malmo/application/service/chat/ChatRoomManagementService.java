package makeus.cmc.malmo.application.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.in.aop.CheckValidMember;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomCommandHelper;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomQueryHelper;
import makeus.cmc.malmo.application.port.in.chat.CreateChatRoomUseCase;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.service.ChatRoomDomainService;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomManagementService implements CreateChatRoomUseCase {

    private final ChatRoomDomainService chatRoomDomainService;
    private final ChatRoomQueryHelper chatRoomQueryHelper;
    private final ChatRoomCommandHelper chatRoomCommandHelper;

    @Override
    @Transactional
    @CheckValidMember
    public CreateChatRoomResponse createChatRoom(CreateChatRoomCommand command) {
        MemberId memberId = MemberId.of(command.getUserId());

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

        log.info("새 BEFORE_INIT 채팅방 생성: chatRoomId={}, memberId={}", savedChatRoom.getId(), memberId.getValue());
        
        return CreateChatRoomResponse.builder()
                .chatRoomId(savedChatRoom.getId())
                .chatRoomState(savedChatRoom.getChatRoomState())
                .createdAt(savedChatRoom.getCreatedAt())
                .build();
    }

}
