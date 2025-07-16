package makeus.cmc.malmo.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.in.GetCurrentChatRoomMessagesUseCase;
import makeus.cmc.malmo.application.port.in.GetCurrentChatRoomUseCase;
import makeus.cmc.malmo.application.port.out.LoadCurrentMessagesPort;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.service.ChatMessagesDomainService;
import makeus.cmc.malmo.domain.service.ChatRoomDomainService;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatRoomService implements GetCurrentChatRoomUseCase, GetCurrentChatRoomMessagesUseCase {

    private final ChatRoomDomainService chatRoomDomainService;
    private final ChatMessagesDomainService chatMessagesDomainService;

    @Override
    @Transactional
    public GetCurrentChatRoomResponse getCurrentChatRoom(GetCurrentChatRoomCommand command) {
        // 현재 채팅방 가져오기
        ChatRoom currentChatRoom = chatRoomDomainService.getCurrentChatRoomByMemberId(MemberId.of(command.getUserId()));

        return GetCurrentChatRoomResponse.builder()
                .chatRoomStatus(currentChatRoom.getChatRoomState())
                .build();
    }

    @Override
    public GetCurrentChatRoomMessagesResponse getCurrentChatRoomMessages(GetCurrentChatRoomMessagesCommand command) {
        // 현재 채팅방 가져오기
        ChatRoom currentChatRoom = chatRoomDomainService.getCurrentChatRoomByMemberId(MemberId.of(command.getUserId()));

        List<ChatRoomMessageDto> list = chatMessagesDomainService.getChatMessagesDto(
                        ChatRoomId.of(currentChatRoom.getId()), command.getPage(), command.getSize())
                .stream()
                .map(cm ->
                        ChatRoomMessageDto.builder()
                                .messageId(cm.getMessageId())
                                .senderType(cm.getSenderType())
                                .content(cm.getContent())
                                .createdAt(cm.getCreatedAt())
                                .isSaved(cm.isSaved())
                                .build())
                .toList();

        return GetCurrentChatRoomMessagesResponse.builder()
                .messages(list)
                .build();
    }
}
