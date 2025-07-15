package makeus.cmc.malmo.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.in.SendChatMessageUseCase;
import makeus.cmc.malmo.application.port.out.LoadChatRoomMetadataPort;
import makeus.cmc.malmo.application.port.out.LoadPromptPort;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import makeus.cmc.malmo.domain.service.*;
import makeus.cmc.malmo.domain.value.type.SenderType;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService implements SendChatMessageUseCase {

    private final ChatRoomDomainService chatRoomDomainService;
    private final ChatMessagesDomainService chatMessagesDomainService;
    private final MemberDomainService memberDomainService;
    private final ChatStreamProcessor chatStreamProcessor;
    private final MemberMemoryDomainService memberMemoryDomainService;

    private final LoadPromptPort loadPromptPort;

    @Override
    @Transactional
    public SendChatMessageResponse processUserMessage(SendChatMessageCommand command) {
        List<Map<String, String>> messages = new ArrayList<>();

        Member member = memberDomainService.getMemberById(MemberId.of(command.getUserId()));

        // MemberMemory 가져오기
        String memberMemoryList = memberMemoryDomainService.getMemberMemoriesByMemberId(MemberId.of(member.getId()));

        // Member의 닉네임, 디데이, 애착 유형, 상대방 애착 유형 정보 가져오기. (user : [사용자 메타데이터])
        //  D-day 정보는 단기, 중기, 장기로 구분하여 활용
        StringBuilder metadataBuilder = new StringBuilder();
        metadataBuilder.append("[사용자 메타데이터]\n");
        String nickname = member.getNickname();
        metadataBuilder.append("- 사용자 이름: ").append(nickname).append("\n");
        String dDayState = memberDomainService.getMemberDDayState(member.getStartLoveDate());
        metadataBuilder.append("- 연애 기간: ").append(dDayState).append("\n");

        LoadChatRoomMetadataPort.ChatRoomMetadataDto chatRoomMetadataDto = chatRoomDomainService.getChatRoomMetadata(MemberId.of(member.getId()));
        String memberLoveTypeTitle = chatRoomMetadataDto.memberLoveTypeTitle();
        metadataBuilder.append("- 사용자 애착 유형: ").append(memberLoveTypeTitle).append("\n");

        String partnerLoveType = chatRoomMetadataDto.partnerLoveTypeTitle();
        metadataBuilder.append("- 애인 애착 유형: ").append(partnerLoveType).append("\n");
        metadataBuilder.append(memberMemoryList);

        messages.add(Map.of("role", "user", "content", metadataBuilder.toString()));

        // 시스템 프롬프트 불러오기 (system)
        // TODO : 예외 처리 필요
        String systemPrompt = loadPromptPort.loadPromptByLevel(-2)
                .map(Prompt::getContent)
                .orElse("시스템 프롬프트가 없습니다.");
        messages.add(
                Map.of(
                        "role", "system",
                        "content", systemPrompt
                )
        );
        log.info(systemPrompt);

        //  현재 ChatRoom의 LEVEL 불러오기
        ChatRoom chatRoom = chatRoomDomainService.getCurrentChatRoomByMemberId(MemberId.of(member.getId()));
        int nowChatRoomLevel = chatRoom.getLevel();

        //  LEVEL에 따라 프롬프트 불러오기 (user : [현재 단계 지시])
        Prompt prompt = loadPromptPort.loadPromptByLevel(nowChatRoomLevel)
                .orElse(null); // TODO: 예외 처리 필요
        String promptContent = prompt.getContent();

        messages.add(
                Map.of(
                        "role", "user",
                        "content",promptContent
                )
        );
        log.info(promptContent);

        //  ChatRoom의 isCurrentPromptForMetadata를 Prompt와 동기화
        if (!prompt.isForMetadata()) {
            chatRoom.updateCurrentPromptStateNotForMetadata();
            chatRoomDomainService.saveChatRoom(chatRoom);
        }

        // TODO : Message가 없다면?? => 이전 LEVEL이 종료, 현재 레벨에 처음 진입했다는 의미.
        //  이전 레벨의 ChatMessageSummary와 summarized = false인 ChatMessage를 비동기 요약 처리
        //  요약 전 메시지를 바탕으로 현재 단계 지시 프롬프트로 요청 (status를 다시 ALIVE로 변경)

        // TODO : 현재 LEVEL에 해당하는 ChatMessage 불러오기 (level = now, summarized = false) (user, assistant)
        //  현재 LEVEL의 ChatMessage 수가 10 이상
        //      => 요약본 생성 프롬프트로 요약 요청
        //          - 비동기 처리
        //          - stream = false
        //          - 현재 단계 지시 + 이후부터는 요약된 메시지만 참조할 것 ~ + 현재 단계 메시지들
        //      => ChatMessageSummary에 (current=true) 저장
        //  요약된 ChatMessage의 summarized = true로 변경 (bulk update)
        //  이전 요약본을 가져오기 위해 ChatMessageSummary를 전체 조회 (user : [이전 단계 요약])
        //      - isSummaryForMetaData = false && current = false
        //  현재 LEVEL의 ChatMessageSummary 불러오기 (level=now, current=true) (user : [현재 단계 요약])

        // TODO : 커플 연동이 된다면, PAUSED 상태인 ChatRoom을 ALIVE 상태로 변경

        // TODO : 시스템 프롬프트, 이전 단계 요약, 현재 단계 지시, 현재 단계 요약, 현재 단계 메시지들을 모아서 OpenAI API에 요청
        List<ChatMessage> history = chatMessagesDomainService.getChatMessages(ChatRoomId.of(chatRoom.getId()));

        for (ChatMessage record : history) {
            messages.add(createMessageMap(record.getSenderType(), record.getContent()));
        }

        // 현재 메시지 추가
        messages.add(createMessageMap(SenderType.USER, command.getMessage()));
        ChatMessage savedUserTextMessage = chatMessagesDomainService.createUserTextMessage(ChatRoomId.of(chatRoom.getId()), command.getMessage());

        // OpenAI API 스트리밍 호출
        chatStreamProcessor.requestApiStream(
                MemberId.of(command.getUserId()),
                !Objects.equals(partnerLoveType, "알 수 없음"), // TODO: 커플 연동 여부 확인 로직 추가
                prompt.isLastPromptForMetadata(),
                messages,
                ChatRoomId.of(chatRoom.getId()));

        return SendChatMessageResponse.builder()
                .messageId(savedUserTextMessage.getId())
                .build();
    }

    private Map<String, String> createMessageMap(SenderType senderType, String content) {
        return Map.of(
                "role", senderType.getApiName(),
                "content", content
        );
    }
}
