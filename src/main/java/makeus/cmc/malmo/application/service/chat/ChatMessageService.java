package makeus.cmc.malmo.application.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.message.RequestConversationSummaryMessage;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomCommandHelper;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomQueryHelper;
import makeus.cmc.malmo.application.helper.chat_room.PromptQueryHelper;
import makeus.cmc.malmo.application.helper.chat_room.DetailedPromptQueryHelper;
import makeus.cmc.malmo.application.helper.chat_room.MemberChatRoomMetadataCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberMemoryCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.helper.question.CoupleQuestionQueryHelper;
import makeus.cmc.malmo.application.port.in.chat.ProcessMessageUseCase;
import makeus.cmc.malmo.application.port.in.chat.SufficiencyCheckResult;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatMessageSummary;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import makeus.cmc.malmo.domain.model.chat.DetailedPrompt;
import makeus.cmc.malmo.domain.model.chat.MemberChatRoomMetadata;
import makeus.cmc.malmo.util.ChatTokenConstants;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.model.member.MemberMemory;
import makeus.cmc.malmo.domain.model.question.CoupleQuestion;
import makeus.cmc.malmo.domain.model.question.MemberAnswer;
import makeus.cmc.malmo.domain.service.ChatRoomDomainService;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.CoupleId;
import makeus.cmc.malmo.domain.value.id.CoupleQuestionId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.util.ChatMessageSplitter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static makeus.cmc.malmo.adaptor.message.StreamMessageType.REQUEST_CONVERSATION_SUMMARY;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService implements ProcessMessageUseCase {

    private final MemberQueryHelper memberQueryHelper;
    private final ChatRoomQueryHelper chatRoomQueryHelper;
    private final PromptQueryHelper promptQueryHelper;
    private final DetailedPromptQueryHelper detailedPromptQueryHelper;
    private final MemberChatRoomMetadataCommandHelper memberChatRoomMetadataCommandHelper;
    private final ChatPromptBuilder chatPromptBuilder;
    private final ChatProcessor chatProcessor;
    private final ChatSseSender chatSseSender;

    private final ChatRoomCommandHelper chatRoomCommandHelper;
    private final ChatRoomDomainService chatRoomDomainService;

    private final CoupleQuestionQueryHelper coupleQuestionQueryHelper;

    private final MemberMemoryCommandHelper memberMemoryCommandHelper;

    private final makeus.cmc.malmo.application.helper.outbox.OutboxHelper outboxHelper;

    @Override
    public CompletableFuture<Void> processStreamChatMessage(ProcessMessageCommand command) {
        MemberId memberId = MemberId.of(command.getMemberId());
        Member member = memberQueryHelper.getMemberByIdOrThrow(memberId);
        ChatRoom chatRoom = chatRoomQueryHelper.getChatRoomByIdOrThrow(ChatRoomId.of(command.getChatRoomId()));

        // 1. 유저 메시지 저장
//        saveUserMessage(chatRoom, command);

        // 4단계 이상: 충분성 검사 없이 자유 대화 응답
        if (command.getPromptLevel() >= 4) {
            return processFreeConversation(member, chatRoom, command);
        }

        // 2. 충분성 조건 검사 (1~3단계)
        CompletableFuture<SufficiencyCheckResult> sufficiencyCheck = 
            requestSufficiencyCheck(member, chatRoom, command);

        return sufficiencyCheck.thenCompose(result -> {
            if (!result.isCompleted()) {
                // 조건 미충족: advice 포함하여 응답 생성
                return requestResponseToMeetCondition(member, chatRoom, command, result.getAdvice());
            }
            
            // 조건 충족: MemberChatRoomMetadata 저장 + 요약 요청
            saveMemberChatRoomMetadata(chatRoom, command, result);

            DetailedPrompt detailedPrompt = detailedPromptQueryHelper.getGuidelinePrompt(
                command.getPromptLevel(), command.getDetailedLevel())
                .orElseThrow(() -> new RuntimeException("Guideline prompt not found"));

            // 마지막 충분성 조건이 아닌 경우
            if (!detailedPrompt.isLastDetailedPrompt()) {
                // 다음 충분성 조건으로
                chatRoom.upgradeDetailedLevel();
                chatRoomCommandHelper.saveChatRoom(chatRoom);
                return requestNextDetailedPromptOpening(chatRoom, command);
            }

            // 마지막 충분성 조건인 경우
            // 1단계 종료 시 제목 생성 요청
            if (command.getPromptLevel() == 1) {
                requestTitleGenerationAsync(chatRoom);
            }

            // 다음 단계 오프닝 생성 요청
            chatRoom.upgradeToNextStage();
            chatRoomCommandHelper.saveChatRoom(chatRoom);
            return requestNextStageOpening(member, chatRoom, command);
        });
    }


    @Override
    public CompletableFuture<Void> processAnswerMetadata(ProcessAnswerCommand command) {
        // 1. 비동기 작업에 필요한 데이터는 미리 동기적으로 조회합니다.
        MemberAnswer memberAnswer = coupleQuestionQueryHelper.getMemberAnswerOrThrow(
                CoupleQuestionId.of(command.getCoupleQuestionId()),
                MemberId.of(command.getMemberId()));

        CoupleQuestion coupleQuestion = coupleQuestionQueryHelper.getCoupleQuestionByIdOrThrow(
                CoupleQuestionId.of(command.getCoupleQuestionId()));

        Prompt metadataPrompt = promptQueryHelper.getAnswerMetadataPrompt();

        // 2. 비동기 API 호출로 CompletableFuture 체인을 시작하고, 그 결과를 즉시 반환합니다.
        return chatProcessor.requestMetaData(
                        coupleQuestion.getQuestion().getContent(),
                        memberAnswer.getAnswer(),
                        metadataPrompt
                )
                // 3. API 호출이 성공적으로 완료되면(then), 그 결과(metadata)를 받아서(Accept) 다음 작업을 비동기(Async)로 실행합니다.
                .thenAcceptAsync(metadata -> {
                    // 멤버 메모리 생성 및 저장
                    MemberMemory memberMemory = MemberMemory.createMemberMemory(
                            CoupleId.of(command.getCoupleId()),
                            MemberId.of(command.getMemberId()),
                            metadata);

                    memberMemoryCommandHelper.saveMemberMemory(memberMemory);
                });
    }


    /**
     * 4단계 이상: 자유 대화 처리
     * - 충분성 검사 없이 바로 응답 생성
     * - 단계 전환 없이 현재 level 유지
     * - 메타데이터 저장 스킵
     * - 토큰 관리를 위한 요약 트리거 포함
     */
    private CompletableFuture<Void> processFreeConversation(Member member, ChatRoom chatRoom, ProcessMessageCommand command) {
        ChatRoomId chatRoomId = ChatRoomId.of(chatRoom.getId());
        int level = chatRoom.getLevel();
        
        // 메시지 개수 체크 및 요약 트리거
        long messageCount = chatRoomQueryHelper.countMessagesByLevel(chatRoomId, level);
        if (messageCount > ChatTokenConstants.FREE_CONVERSATION_SUMMARY_THRESHOLD 
                && messageCount % ChatTokenConstants.FREE_CONVERSATION_SUMMARY_INTERVAL == 0) {
            // 비동기로 요약 생성 요청 (20개 단위로)
            requestConversationSummaryAsync(chatRoom);
        }
        
        // createForFreeConversation 사용 (최근 20개 + 요약 포함)
        List<Map<String, String>> messages = chatPromptBuilder.createForFreeConversation(
                member, chatRoom, command.getNowMessage());
        
        Prompt systemPrompt = promptQueryHelper.getSystemPrompt();
        Prompt prompt = promptQueryHelper.getGuidelinePromptWithFallback(command.getPromptLevel());
        DetailedPrompt detailedPrompt = detailedPromptQueryHelper.getGuidelinePromptWithFallback(
                command.getPromptLevel(), command.getDetailedLevel());
        
        return chatProcessor.streamChat(messages, systemPrompt, prompt, detailedPrompt,
                chunk -> chatSseSender.sendResponseChunk(MemberId.of(member.getId()), chunk),
                fullAnswer -> saveAiMessage(MemberId.of(member.getId()), ChatRoomId.of(chatRoom.getId()),
                        command.getPromptLevel(), command.getDetailedLevel(), fullAnswer),
                errorMessage -> chatSseSender.sendError(MemberId.of(member.getId()), errorMessage)
        ).toFuture();
    }

    private CompletableFuture<SufficiencyCheckResult> requestSufficiencyCheck(Member member, ChatRoom chatRoom, ProcessMessageCommand command) {
        List<Map<String, String>> messages = chatPromptBuilder.createForSufficiencyCheck(
                member, chatRoom, command.getPromptLevel(), command.getDetailedLevel());
        
        log.info("Requesting sufficiency check for memberId: {}, chatRoomId: {}, level: {}, detailedLevel: {}",
                member.getId(), chatRoom.getId(), command.getPromptLevel(), command.getDetailedLevel());
        DetailedPrompt validationPrompt = detailedPromptQueryHelper.getValidationPrompt(
                command.getPromptLevel(), command.getDetailedLevel())
                .orElseThrow(() -> new RuntimeException("Validation prompt not found"));
        
        return chatProcessor.requestSufficiencyCheck(messages, validationPrompt);
    }

    private CompletableFuture<Void> requestResponseToMeetCondition(Member member, ChatRoom chatRoom, ProcessMessageCommand command, String advice) {
        List<Map<String, String>> messages = chatPromptBuilder.createForProcessUserMessage(
                member, chatRoom, command.getNowMessage());
        
        // advice를 포함한 프롬프트 추가
        if (advice != null && !advice.isEmpty()) {
            messages.add(Map.of("role", "system", "content", "상담 검수자의 Advice: " + advice));
        }
        
        Prompt systemPrompt = promptQueryHelper.getSystemPrompt();
        Prompt prompt = promptQueryHelper.getGuidelinePromptWithFallback(command.getPromptLevel());
        DetailedPrompt detailedPrompt = detailedPromptQueryHelper.getGuidelinePromptWithFallback(
                        command.getPromptLevel(), command.getDetailedLevel());
        
        return chatProcessor.streamChat(messages, systemPrompt, prompt, detailedPrompt,
                chunk -> chatSseSender.sendResponseChunk(MemberId.of(member.getId()), chunk),
                fullAnswer -> saveAiMessage(MemberId.of(member.getId()), ChatRoomId.of(chatRoom.getId()),
                        command.getPromptLevel(), command.getDetailedLevel(), fullAnswer),
                errorMessage -> chatSseSender.sendError(MemberId.of(member.getId()), errorMessage)
        ).toFuture();
    }

    private void saveMemberChatRoomMetadata(ChatRoom chatRoom, ProcessMessageCommand command, SufficiencyCheckResult result) {
        DetailedPrompt detailedPrompt = detailedPromptQueryHelper.getGuidelinePrompt(
            command.getPromptLevel(), command.getDetailedLevel())
            .orElseThrow(() -> new RuntimeException("Guideline prompt not found"));
        MemberChatRoomMetadata metadata = MemberChatRoomMetadata.create(
                ChatRoomId.of(chatRoom.getId()),
                MemberId.of(command.getMemberId()),
                command.getPromptLevel(),
                command.getDetailedLevel(),
                detailedPrompt.getMetadataTitle(),
                result.getSummary()
        );
        memberChatRoomMetadataCommandHelper.saveMemberChatRoomMetadata(metadata);
    }

    private CompletableFuture<Void> requestNextDetailedPromptOpening(ChatRoom chatRoom, ProcessMessageCommand command) {
        // 다음 충분성 조건 오프닝 생성 요청
        MemberId memberId = MemberId.of(command.getMemberId());
        Member member = memberQueryHelper.getMemberByIdOrThrow(memberId);

        // 사용자 메타데이터 정보 생성
        List<Map<String, String>> messages = chatPromptBuilder.createForNextDetailedPrompt(
                member, chatRoom, command.getPromptLevel(), command.getDetailedLevel() + 1);

        // 시스템 프롬프트 + 현재 단계 프롬프트 + 다음 충분성 조건 프롬프트
        Prompt systemPrompt = promptQueryHelper.getSystemPrompt();
        Prompt prompt = promptQueryHelper.getGuidelinePromptWithFallback(chatRoom.getLevel());
        DetailedPrompt nextDetailedPrompt = detailedPromptQueryHelper.getGuidelinePromptWithFallback(
                command.getPromptLevel(), command.getDetailedLevel() + 1);
        
        return chatProcessor.streamChat(messages, systemPrompt, prompt, nextDetailedPrompt,
                chunk -> chatSseSender.sendResponseChunk(memberId, chunk),
                fullAnswer -> saveAiMessage(memberId, ChatRoomId.of(chatRoom.getId()), 
                        command.getPromptLevel(), command.getDetailedLevel() + 1, fullAnswer),
                errorMessage -> chatSseSender.sendError(memberId, errorMessage)
        ).toFuture();
    }

    private CompletableFuture<Void> requestNextStageOpening(Member member, ChatRoom chatRoom, ProcessMessageCommand command) {
        int nextLevel = command.getPromptLevel() + 1;
        
        // 단계별 요약 없이 컨텍스트 구성
        List<Map<String, String>> messages = chatPromptBuilder.createForNextStage(member, chatRoom, nextLevel);
        
        Prompt systemPrompt = promptQueryHelper.getSystemPrompt();
        
        // 4단계 이상에서는 3단계 프롬프트 재사용
        Prompt nextPrompt = promptQueryHelper.getGuidelinePromptWithFallback(nextLevel);
        
        // DetailedPrompt도 fallback 로직 적용
        DetailedPrompt nextDetailedPrompt = detailedPromptQueryHelper.getGuidelinePromptWithFallback(nextLevel, 1);
        
        return chatProcessor.streamChat(messages, systemPrompt, nextPrompt, nextDetailedPrompt,
                chunk -> chatSseSender.sendResponseChunk(MemberId.of(member.getId()), chunk),
                fullAnswer -> saveAiMessage(MemberId.of(member.getId()), ChatRoomId.of(chatRoom.getId()),
                        nextLevel, 1, fullAnswer),
                errorMessage -> chatSseSender.sendError(MemberId.of(member.getId()), errorMessage)
        ).toFuture();
    }

    /**
     * 비동기 제목 생성 요청
     * Redis Stream을 통해 제목 생성 워커에 전달
     */
    private void requestTitleGenerationAsync(ChatRoom chatRoom) {
        outboxHelper.publish(
                makeus.cmc.malmo.adaptor.message.StreamMessageType.REQUEST_TITLE_GENERATION,
                new makeus.cmc.malmo.adaptor.message.RequestTitleGenerationMessage(chatRoom.getId())
        );
        log.info("Title generation requested for chatRoomId: {}", chatRoom.getId());
    }

    @Override
    public CompletableFuture<Void> processTitleGeneration(ProcessTitleGenerationCommand command) {
        ChatRoom chatRoom = chatRoomQueryHelper.getChatRoomByIdOrThrow(ChatRoomId.of(command.getChatRoomId()));
        
        if (chatRoom.getTitle() != null) {
            log.debug("Title already exists for chatRoomId: {}, skipping generation", command.getChatRoomId());
            return CompletableFuture.completedFuture(null);
        }
        
        // 1단계 대화 내용 조회
        List<Map<String, String>> messages = chatPromptBuilder.createForTitleGeneration(chatRoom);
        
        // 제목 생성 프롬프트 조회
        Prompt titlePrompt = promptQueryHelper.getTitleGenerationPrompt();
        
        return chatProcessor.requestTitleGeneration(messages, titlePrompt)
                .thenAcceptAsync(title -> {
                    chatRoom.updateTitle(title);
                    chatRoomCommandHelper.saveChatRoom(chatRoom);
                    log.info("Title generated for chatRoomId: {}, title: {}", command.getChatRoomId(), title);
                });
    }

    @Override
    public CompletableFuture<Void> processConversationSummary(ProcessConversationSummaryCommand command) {
        ChatRoomId chatRoomId = ChatRoomId.of(command.getChatRoomId());
        int level = command.getLevel();
        
        // 최신 요약 이후의 메시지들을 가져와서 요약
        // 최근 요약이 있다면 그 이후의 메시지, 없다면 전체 메시지 중 최근 요약 주기만큼
        List<ChatMessage> messagesToSummarize = chatRoomQueryHelper.getRecentMessages(
                chatRoomId, level, ChatTokenConstants.FREE_CONVERSATION_SUMMARY_INTERVAL);
        
        if (messagesToSummarize.isEmpty()) {
            log.debug("No messages to summarize for chatRoomId: {}, level: {}", chatRoomId.getValue(), level);
            return CompletableFuture.completedFuture(null);
        }
        
        // 요약할 메시지들을 프롬프트 형식으로 변환
        List<Map<String, String>> summaryMessages = new ArrayList<>();
        for (ChatMessage chatMessage : messagesToSummarize) {
            summaryMessages.add(Map.of(
                    "role", chatMessage.getSenderType().getApiName(),
                    "content", chatMessage.getContent()
            ));
        }
        
        // 요약 프롬프트 조회 (4단계 요약 프롬프트 사용)
        Prompt summaryPrompt = promptQueryHelper.getSummaryPromptByLevel(level)
                .orElseGet(() -> {
                    log.warn("Summary prompt not found for level: {}, using default", level);
                    return promptQueryHelper.getSummaryPromptByLevel(3)
                            .orElseThrow(() -> new RuntimeException("Summary prompt not found"));
                });
        
        // 비동기 요약 생성 및 저장
        return chatProcessor.requestConversationSummary(summaryMessages, summaryPrompt)
                .thenAcceptAsync(summaryContent -> {
                    ChatMessageSummary chatMessageSummary = ChatMessageSummary.createChatMessageSummary(
                            chatRoomId, summaryContent, level);
                    chatRoomCommandHelper.saveChatMessageSummary(chatMessageSummary);
                    log.info("Conversation summary saved for chatRoomId: {}, level: {}", chatRoomId.getValue(), level);
                })
                .exceptionally(throwable -> {
                    log.error("Failed to generate conversation summary for chatRoomId: {}, level: {}",
                            chatRoomId.getValue(), level, throwable);
                    return null;
                });
    }

    /**
     * 비동기 4단계 대화 요약 생성 요청
     * Redis Stream을 통해 요약 생성 워커에 전달
     */
    private void requestConversationSummaryAsync(ChatRoom chatRoom) {
        outboxHelper.publish(
                REQUEST_CONVERSATION_SUMMARY,
                new RequestConversationSummaryMessage(chatRoom.getId(), chatRoom.getLevel())
        );
        log.info("Conversation summary requested for chatRoomId: {}, level: {}", chatRoom.getId(), chatRoom.getLevel());
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
