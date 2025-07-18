package makeus.cmc.malmo.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.in.SendChatMessageUseCase;
import makeus.cmc.malmo.application.port.out.LoadChatRoomMetadataPort;
import makeus.cmc.malmo.application.port.out.LoadPromptPort;
import makeus.cmc.malmo.application.port.out.ValidateMemberPort;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatMessageSummary;
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
    private final LoadPromptDomainService loadPromptDomainService;

    private final ValidateMemberPort validateMemberPort;

    @Override
    @Transactional
    public SendChatMessageResponse processUserMessage(SendChatMessageCommand command) {
        // TODO : 채팅방이 PAUSED, DELETED 상태인 경우 예외 처리 필요

        Member member = memberDomainService.getMemberById(MemberId.of(command.getUserId()));

        // MemberMemory 가져오기
        List<Map<String, String>> messages = initMessages(member);

        //  현재 ChatRoom의 LEVEL 불러오기
        ChatRoom chatRoom = chatRoomDomainService.getCurrentChatRoomByMemberId(MemberId.of(member.getId()));
        int nowChatRoomLevel = chatRoom.getLevel();

        //  시스템 프롬프트 불러오기 (system)
        Prompt systemPrompt = loadPromptDomainService.getSystemPrompt();
        //  LEVEL에 따라 프롬프트 불러오기 (user : [현재 단계 지시])
        Prompt prompt = loadPromptDomainService.getPromptByLevel(nowChatRoomLevel);

        //  ChatRoom의 isCurrentPromptForMetadata를 Prompt와 동기화
        if (!prompt.isForMetadata()) {
            chatRoom.updateCurrentPromptStateNotForMetadata();
            chatRoomDomainService.saveChatRoom(chatRoom);
        }

        // 현재 LEVEL에 해당하는 ChatMessage 불러오기 (level = now, summarized = false) (user, assistant)

        // 이전 단계에서 중간 요약되지 않은 메시지들
        List<ChatMessage> unsummarizedMessages = chatMessagesDomainService.getNotSummarizedChatMessages(ChatRoomId.of(chatRoom.getId()));
        // 현재 단계에서 중간 요약된 메시지들
        //  현재 LEVEL의 ChatMessageSummary 불러오기 (level=now, current=true) (user : [현재 단계 요약])
        List<ChatMessageSummary> currentLevelSummaries = chatMessagesDomainService.getCurrentSummarizedMessagesByLevel(ChatRoomId.of(chatRoom.getId()), nowChatRoomLevel);
        // 이전 단계에서 요약된 메시지들
        //  이전 요약본을 가져오기 위해 ChatMessageSummary를 전체 조회 (user : [이전 단계 요약])
        //      - isSummaryForMetaData = false && current = false
        List<ChatMessageSummary> previousLevelSummaries = chatMessagesDomainService.getPreviousLevelsSummarizedMessages(ChatRoomId.of(chatRoom.getId()));

        //  현재 단계에서 채팅 메시지의 수가 10 이상인 경우 요약 처리를 비동기 요청
        if (unsummarizedMessages.size() >= 10) {
            List<Map<String, String>> summaryRequestMessages = new ArrayList<>();
            for (ChatMessage unsummarizedMessage : unsummarizedMessages) {
                summaryRequestMessages.add(
                        Map.of(
                                "role", unsummarizedMessage.getSenderType().getApiName(),
                                "content", unsummarizedMessage.getContent()
                        )
                );
            }

            // 요약 요청 프롬프트 생성
            Prompt summaryPrompt = loadPromptDomainService.getSummaryPrompt();

            //      => 요약본 생성 프롬프트로 요약 요청
            chatStreamProcessor.requestSummaryAsync(
                    ChatRoomId.of(chatRoom.getId()),
                    MemberId.of(member.getId()),
                    systemPrompt, prompt, summaryPrompt,
                    true,
                    summaryRequestMessages
            );
        }

        // 이전 단계 요약본
        String previousLevelSummaryContent = extractSummaryMessages("[이전 단계 요약]\n", previousLevelSummaries);
        messages.add(createMessageMap(SenderType.USER, previousLevelSummaryContent));

        // 현재 단계 중간 요약 메시지들
        String currentLevelSummaryContent = extractSummaryMessages("[현재 단계 중간 요약]\n", currentLevelSummaries);
        messages.add(createMessageMap(SenderType.USER, currentLevelSummaryContent));

        // 현재 단계 메시지들
        for (ChatMessage unsummarizedMessage : unsummarizedMessages) {
            messages.add(
                    createMessageMap(
                            unsummarizedMessage.getSenderType(),
                            unsummarizedMessage.getContent()
                    ));
        }
        // TODO : 커플 연동이 된다면, PAUSED 상태인 ChatRoom을 ALIVE 상태로 변경

        // 현재 메시지 추가
        messages.add(createMessageMap(SenderType.USER, command.getMessage()));

        // 사용자가 보낸 메시지를 영속화
        ChatMessage savedUserTextMessage = chatMessagesDomainService.createUserTextMessage(ChatRoomId.of(chatRoom.getId()), command.getMessage());

        // OpenAI API 스트리밍 호출
        // 시스템 프롬프트, 이전 단계 요약, 현재 단계 지시, 현재 단계 요약, 현재 단계 메시지들을 모아서 OpenAI API에 요청
        boolean isMemberCouple = validateMemberPort.isCoupleMember(MemberId.of(member.getId()));
        chatStreamProcessor.requestApiStream(
                MemberId.of(command.getUserId()),
                isMemberCouple,
                systemPrompt,
                prompt,
                messages,
                ChatRoomId.of(chatRoom.getId()));

        return SendChatMessageResponse.builder()
                .messageId(savedUserTextMessage.getId())
                .build();
    }

    @Override
    @Transactional
    public SendChatMessageResponse upgradeChatRoom(SendChatMessageCommand command) {
        // 이전 LEVEL이 종료, 현재 레벨에 처음 진입한 경우
        //  이전 레벨의 ChatMessageSummary와 summarized = false인 ChatMessage를 비동기 요약 처리
        //  요약 전 메시지를 바탕으로 현재 단계 지시 프롬프트로 요청 (status를 다시 ALIVE로 변경)
        Member member = memberDomainService.getMemberById(MemberId.of(command.getUserId()));
        ChatRoom chatRoom = chatRoomDomainService.getCurrentChatRoomByMemberId(MemberId.of(member.getId()));
        int nowChatRoomLevel = chatRoom.getLevel();

        //  시스템 프롬프트 불러오기 (system)
        Prompt systemPrompt = loadPromptDomainService.getSummaryPrompt();
        //  LEVEL에 따라 프롬프트 불러오기 (user : [현재 단계 지시])
        Prompt prompt = loadPromptDomainService.getPromptByLevel(nowChatRoomLevel);

        // 요약 요청 프롬프트 생성
        chatRoomDomainService.updateChatRoomStateToNeedNextQuestion(ChatRoomId.of(chatRoom.getId()));

        Prompt nextPrompt = loadPromptDomainService.getPromptByLevel(nowChatRoomLevel + 1);

        List<Map<String, String>> summaryRequestMessages = new ArrayList<>();

        // 현재 단계에서 중간 요약된 메시지들
        List<ChatMessageSummary> currentSummarizedMessages = chatMessagesDomainService.getCurrentSummarizedMessagesByLevel(ChatRoomId.of(chatRoom.getId()), nowChatRoomLevel - 1);
        // 현재 단계에서 중간 요약되지 않은 메시지들
        List<ChatMessage> unsummarizedMessages = chatMessagesDomainService.getNotSummarizedChatMessages(ChatRoomId.of(chatRoom.getId()));
        // 이전 단계에서 요약된 메시지들 (지금 사용하지 않지만 요약 요청의 비동기 처리 간섭을 막기 위해 미리 로드)
        List<ChatMessageSummary> previousLevelSummaries = chatMessagesDomainService.getPreviousLevelsSummarizedMessages(ChatRoomId.of(chatRoom.getId()));

        // 현재 단계 요약된 메시지들
        String currentLevelSummarizedMessages = extractSummaryMessages("[현재 단계 요약된 메시지]\n", currentSummarizedMessages);
        summaryRequestMessages.add(createMessageMap(SenderType.USER, currentLevelSummarizedMessages));

        // 현재 단계 요약되지 않은 메시지들
        for (ChatMessage unsummarizedMessage : unsummarizedMessages) {
            summaryRequestMessages.add(
                    createMessageMap(
                            unsummarizedMessage.getSenderType(),
                            unsummarizedMessage.getContent()
                    ));
        }

        Prompt summaryPrompt = loadPromptDomainService.getSummaryPrompt();

        // 요약 요청
        chatStreamProcessor.requestSummaryAsync(
                ChatRoomId.of(chatRoom.getId()),
                MemberId.of(member.getId()),
                systemPrompt, prompt, summaryPrompt,
                false,
                summaryRequestMessages
        );

        // 이전 단계 요약 메시지들 메시지에 추가
        String previousLevelSummarizedMessages = extractSummaryMessages("[이전 단계 요약]\n", previousLevelSummaries);
        summaryRequestMessages.add(createMessageMap(SenderType.USER, previousLevelSummarizedMessages));

        boolean isMemberCouple = validateMemberPort.isCoupleMember(MemberId.of(member.getId()));

        // 과거 대화로부터 현재 단계 오프닝 멘트 요청
        chatStreamProcessor.requestApiStream(
                MemberId.of(command.getUserId()),
                isMemberCouple,
                systemPrompt,
                nextPrompt,
                summaryRequestMessages,
                ChatRoomId.of(chatRoom.getId()));

        chatRoomDomainService.updateChatRoomStateToAlive(ChatRoomId.of(chatRoom.getId()));

        return SendChatMessageResponse.builder()
                .messageId(null) // 업그레이드 시에는 메시지 ID가 필요하지 않음
                .build();
    }

    private Map<String, String> createMessageMap(SenderType role, String content) {
        return Map.of("role", role.getApiName(), "content", content);
    }

    private String extractSummaryMessages(String title, List<ChatMessageSummary> summaryList) {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        for (ChatMessageSummary previousLevelSummary : summaryList) {
            sb.append("- ").append(previousLevelSummary.getContent()).append("\n");
        }

        return sb.toString();
    }

    private List<Map<String, String>> initMessages(Member member) {
        List<Map<String, String>> messages = new ArrayList<>();

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

        return messages;
    }
}
