package makeus.cmc.malmo.domain.service;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.*;
import makeus.cmc.malmo.domain.exception.ChatRoomNotFoundException;
import makeus.cmc.malmo.domain.exception.MemberAccessDeniedException;
import makeus.cmc.malmo.domain.exception.MemberNotFoundException;
import makeus.cmc.malmo.domain.exception.NotValidChatRoomException;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

import static makeus.cmc.malmo.domain.model.chat.ChatRoomConstant.INIT_CHATROOM_LEVEL;
import static makeus.cmc.malmo.domain.model.chat.ChatRoomConstant.INIT_CHAT_MESSAGE;

@Component
@RequiredArgsConstructor
public class ChatRoomDomainService {

    private final LoadChatRoomPort loadChatRoomPort;
    private final LoadMemberPort loadMemberPort;
    private final SaveChatRoomPort saveChatRoomPort;
    private final DeleteChatRoomPort deleteChatRoomPort;
    private final SaveChatMessagePort saveChatMessagePort;
    private final LoadChatRoomMetadataPort loadChatRoomMetadataPort;

    public void validateChatRoomOwnership(MemberId memberId, ChatRoomId chatRoomId) {
        // 채팅방이 존재하는지 확인하고, 존재하지 않으면 예외 발생
        ChatRoom chatRoom = loadChatRoomPort.loadChatRoomById(chatRoomId)
                .orElseThrow(ChatRoomNotFoundException::new);

        // 채팅방의 소유자와 요청한 멤버가 일치하는지 확인
        if (!chatRoom.isOwner(memberId)) {
            throw new MemberAccessDeniedException("채팅방에 접근할 권한이 없습니다.");
        }
    }

    public void validateChatRoomsOwnership(MemberId memberId, List<ChatRoomId> chatRoomIds) {
        boolean valid = loadChatRoomPort.isMemberOwnerOfChatRooms(memberId, chatRoomIds);

        if (!valid) {
            throw new MemberAccessDeniedException("채팅방에 접근할 권한이 없습니다.");
        }
    }

    public ChatRoom getCurrentChatRoomByMemberId(MemberId memberId) {
        // 현재 채팅방이 존재하는지 확인하고, 없으면 초기 메시지와 함께 새로 생성
        return loadChatRoomPort.loadCurrentChatRoomByMemberId(memberId)
                .orElseGet(() -> {
                    Member member = loadMemberPort.loadMemberById(MemberId.of(memberId.getValue()))
                            .orElseThrow(MemberNotFoundException::new);
                    ChatRoom chatRoom = saveChatRoomPort.saveChatRoom(ChatRoom.createChatRoom(memberId));
                    ChatMessage initMessage = ChatMessage.createAssistantTextMessage(
                            ChatRoomId.of(chatRoom.getId()), INIT_CHATROOM_LEVEL, member.getNickname() + INIT_CHAT_MESSAGE);
                    saveChatMessagePort.saveChatMessage(initMessage);

                    return chatRoom;
                });
    }

    public void validateChatRoomAlive(MemberId memberId) {
        loadChatRoomPort.loadCurrentChatRoomByMemberId(memberId)
                .ifPresentOrElse(chatRoom -> {
                            if (!chatRoom.isChatRoomValid()) {
                                throw new NotValidChatRoomException();
                            }
                        }
                        , () -> {
                            throw new ChatRoomNotFoundException();
                        }
                );
    }


    public LoadChatRoomMetadataPort.ChatRoomMetadataDto getChatRoomMetadata(MemberId memberId) {
        return loadChatRoomMetadataPort.loadChatRoomMetadata(memberId)
                .orElse(new LoadChatRoomMetadataPort.ChatRoomMetadataDto(null, null));
    }

    public ChatRoom getChatRoomById(ChatRoomId chatRoomId) {
        return loadChatRoomPort.loadChatRoomById(chatRoomId).orElseThrow(ChatRoomNotFoundException::new);
    }

    public Page<ChatRoom> getCompletedChatRoomsByMemberId(MemberId memberId, String keyword, Pageable pageable) {
        return loadChatRoomPort.loadAliveChatRoomsByMemberId(memberId, keyword, pageable);
    }

    public void saveChatRoom(ChatRoom chatRoom) {
        saveChatRoomPort.saveChatRoom(chatRoom);
    }

    public void updateChatRoomStateToPaused(ChatRoomId chatRoomId) {
        ChatRoom chatRoom = loadChatRoomPort.loadChatRoomById(chatRoomId)
                .orElseThrow(ChatRoomNotFoundException::new);

        chatRoom.updateChatRoomStatePaused();
        saveChatRoom(chatRoom);
    }

    public void updateChatRoomStateToNeedNextQuestion(ChatRoomId chatRoomId) {
        ChatRoom chatRoom = loadChatRoomPort.loadChatRoomById(chatRoomId)
                .orElseThrow(ChatRoomNotFoundException::new);

        chatRoom.upgradeChatRoom();
        saveChatRoom(chatRoom);
    }

    public void updateChatRoomStateToAlive(ChatRoomId chatRoomId) {
        ChatRoom chatRoom = loadChatRoomPort.loadChatRoomById(chatRoomId)
                .orElseThrow(ChatRoomNotFoundException::new);

        chatRoom.updateChatRoomStateAlive();
        saveChatRoom(chatRoom);
    }

    public void completeChatRoom(ChatRoom chatRoom) {
        chatRoom.complete();
        saveChatRoom(chatRoom);
    }

    public void updateChatRoomSummary(ChatRoom chatRoom, String totalSummary, String situationKeyword, String solutionKeyword) {
        chatRoom.updateChatRoomSummary(totalSummary, situationKeyword, solutionKeyword);
        saveChatRoom(chatRoom);
    }

    public void updateMemberPausedChatRoomStateToAlive(MemberId memberId) {
        loadChatRoomPort.loadPausedChatRoomByMemberId(memberId)
                        .ifPresent(
                                chatRoom -> {
                                    chatRoom.updateChatRoomStateNeedNextQuestion();
                                    saveChatRoomPort.saveChatRoom(chatRoom);
                                }
                        );
    }

    public void deleteChatRooms(List<ChatRoomId> chatRoomIds) {
        deleteChatRoomPort.deleteChatRooms(chatRoomIds);
    }
}
