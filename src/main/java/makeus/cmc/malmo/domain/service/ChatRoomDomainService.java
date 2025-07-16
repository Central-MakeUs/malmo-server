package makeus.cmc.malmo.domain.service;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.LoadChatRoomMetadataPort;
import makeus.cmc.malmo.application.port.out.LoadChatRoomPort;
import makeus.cmc.malmo.application.port.out.LoadPromptPort;
import makeus.cmc.malmo.application.port.out.SaveChatRoomPort;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatRoomDomainService {

    private final LoadChatRoomPort loadChatRoomPort;
    private final SaveChatRoomPort saveChatRoomPort;
    private final LoadPromptPort loadPromptPort;
    private final LoadChatRoomMetadataPort loadChatRoomMetadataPort;

    public ChatRoom getCurrentChatRoomByMemberId(MemberId memberId) {
        return loadChatRoomPort.loadCurrentChatRoomByMemberId(memberId)
                .orElseGet(() -> {
                    // 현재 채팅방이 존재하지 않는 경우 새로 채팅방을 생성
                    int level = 0;
                    boolean isCurrentPromptForMetadata = true;
                    // 이전 채팅방 중 가장 LEVEL이 큰 채팅방 조회
                    Optional<ChatRoom> chatRoom = loadChatRoomPort.loadMaxLevelChatRoomByMemberId(memberId);
                    if (chatRoom.isPresent()) {
                        ChatRoom maxChatRoom = chatRoom.get();
                        if (maxChatRoom.isCurrentPromptForMetadata()) {
                            // Metadata 수집 중인 채팅방이 존재하는 경우, 해당 채팅방의 LEVEL로 설정
                            level = maxChatRoom.getLevel();
                        } else {
                            // 과거에 Metadata 수집을 완료한 채팅방이 존재하는 경우, Metadata 수집 이후 단계의 Prompt로 레벨 설정
                            // TODO : 예외 설정
                            Prompt prompt = loadPromptPort.loadPromptMinLevelPrompt().get();
                            level = prompt.getLevel();
                            isCurrentPromptForMetadata = false;
                        }
                    }
                    // 이전 채팅방이 존재하지 않는 경우, LEVEL 0으로 설정
                    return saveChatRoomPort.saveChatRoom(
                            ChatRoom.createChatRoom(memberId, level, isCurrentPromptForMetadata)
                    );
                });
    }


    public LoadChatRoomMetadataPort.ChatRoomMetadataDto getChatRoomMetadata(MemberId memberId) {
        return loadChatRoomMetadataPort.loadChatRoomMetadata(memberId)
                .map(
                        metadata -> new LoadChatRoomMetadataPort.ChatRoomMetadataDto(
                                metadata.memberLoveTypeTitle() != null ? metadata.memberLoveTypeTitle() : "알 수 없음",
                                metadata.partnerLoveTypeTitle() != null ? metadata.partnerLoveTypeTitle() : "알 수 없음"
                )).orElse(new LoadChatRoomMetadataPort.ChatRoomMetadataDto("알 수 없음", "알 수 없음"));
    }

    @Transactional
    public void saveChatRoom(ChatRoom chatRoom) {
        saveChatRoomPort.saveChatRoom(chatRoom);
    }

    @Transactional
    public void updateChatRoomStateToPaused(ChatRoomId chatRoomId) {
        // TODO: 예외 처리 필요
        ChatRoom chatRoom = loadChatRoomPort.loadChatRoomById(chatRoomId)
                .orElse(null);

        if (chatRoom != null) {
            chatRoom.updateChatRoomStatePaused();
            saveChatRoom(chatRoom);
        }
    }

    @Transactional
    public void updateChatRoomStateToNeedNextQuestion(ChatRoomId chatRoomId) {
        // TODO: 예외 처리 필요
        ChatRoom chatRoom = loadChatRoomPort.loadChatRoomById(chatRoomId)
                .orElse(null);

        if (chatRoom != null) {
            chatRoom.updateChatRoomStateNeedNextQuestion();
            saveChatRoom(chatRoom);
        }
    }

    @Transactional
    public void updateChatRoomStateToAlive(ChatRoomId chatRoomId) {
        // TODO: 예외 처리 필요
        ChatRoom chatRoom = loadChatRoomPort.loadChatRoomById(chatRoomId)
                .orElse(null);

        if (chatRoom != null) {
            chatRoom.updateChatRoomStateAlive();
            saveChatRoom(chatRoom);
        }
    }

    @Transactional
    public void updateAllMessagesSummarized(ChatRoomId chatRoomId) {
        saveChatRoomPort.updateAllMessagesSummarizedIsTrue(chatRoomId);
    }
}
