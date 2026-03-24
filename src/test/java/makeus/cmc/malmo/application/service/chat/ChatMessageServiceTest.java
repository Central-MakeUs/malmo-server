package makeus.cmc.malmo.application.service.chat;

import makeus.cmc.malmo.application.helper.chat_room.ChatRoomCommandHelper;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomQueryHelper;
import makeus.cmc.malmo.application.helper.chat_room.DetailedPromptQueryHelper;
import makeus.cmc.malmo.application.helper.chat_room.MemberChatRoomMetadataCommandHelper;
import makeus.cmc.malmo.application.helper.chat_room.PromptQueryHelper;
import makeus.cmc.malmo.application.helper.member.MemberCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberMemoryCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.helper.outbox.OutboxHelper;
import makeus.cmc.malmo.application.helper.question.CoupleQuestionQueryHelper;
import makeus.cmc.malmo.application.port.in.chat.ProcessMessageUseCase;
import makeus.cmc.malmo.application.port.in.chat.SufficiencyCheckResult;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.chat.DetailedPrompt;
import makeus.cmc.malmo.domain.model.chat.MemberChatRoomMetadata;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.service.ChatRoomDomainService;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.ChatRoomState;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.EmailForwardingStatus;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.PartnerLoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.Provider;
import makeus.cmc.malmo.domain.value.type.RelationshipStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageService 테스트")
class ChatMessageServiceTest {

    @Mock
    private MemberQueryHelper memberQueryHelper;

    @Mock
    private ChatRoomQueryHelper chatRoomQueryHelper;

    @Mock
    private PromptQueryHelper promptQueryHelper;

    @Mock
    private DetailedPromptQueryHelper detailedPromptQueryHelper;

    @Mock
    private MemberChatRoomMetadataCommandHelper memberChatRoomMetadataCommandHelper;

    @Mock
    private ChatPromptBuilder chatPromptBuilder;

    @Mock
    private ChatProcessor chatProcessor;

    @Mock
    private ChatSseSender chatSseSender;

    @Mock
    private ChatRoomCommandHelper chatRoomCommandHelper;

    @Mock
    private ChatRoomDomainService chatRoomDomainService;

    @Mock
    private CoupleQuestionQueryHelper coupleQuestionQueryHelper;

    @Mock
    private MemberMemoryCommandHelper memberMemoryCommandHelper;

    @Mock
    private OutboxHelper outboxHelper;

    @Mock
    private MemberCommandHelper memberCommandHelper;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("1단계 완료 후 2단계 첫 분석 메시지 생성 직후 partnerLoveTypeCategory를 추론해 저장한다")
    void processStreamChatMessage_infersAndPersistsPartnerLoveTypeAfterStage2Opening() {
        Member stage2Member = createMember(1L, null);
        Member refreshedMember = createMember(1L, null);
        Member savedMember = createMember(1L, PartnerLoveTypeCategory.CONFUSION_TYPE);
        ChatRoom chatRoom = createChatRoom(10L, 1, 2);
        DetailedPrompt lastDetailedPrompt = DetailedPrompt.create(1, 2, "guideline", false, false, "상황 요약", true, true);
        Prompt systemPrompt = Prompt.from(1L, 0, "system", true, false, false, false, false, false, false, null, null, null);
        Prompt nextPrompt = Prompt.from(2L, 2, "next", false, false, false, false, true, false, false, null, null, null);
        DetailedPrompt nextDetailedPrompt = DetailedPrompt.create(2, 1, "next-detailed", false, false, "분석", true, true);
        ProcessMessageUseCase.ProcessMessageCommand command = ProcessMessageUseCase.ProcessMessageCommand.builder()
                .memberId(1L)
                .chatRoomId(10L)
                .nowMessage("메시지")
                .promptLevel(1)
                .detailedLevel(2)
                .build();
        SufficiencyCheckResult result = SufficiencyCheckResult.builder()
                .completed(true)
                .summary("1단계 상황 요약")
                .build();

        stubCompletedFlow(stage2Member, refreshedMember, chatRoom, lastDetailedPrompt, systemPrompt, nextPrompt, nextDetailedPrompt, result, savedMember);

        when(chatProcessor.requestPartnerLoveTypeCategoryInference(anyList(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(PartnerLoveTypeCategory.CONFUSION_TYPE));

        chatMessageService.processStreamChatMessage(command).join();

        ArgumentCaptor<Member> savedMemberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberCommandHelper).saveMember(savedMemberCaptor.capture());
        assertThat(savedMemberCaptor.getValue().getPartnerLoveTypeCategory()).isEqualTo(PartnerLoveTypeCategory.CONFUSION_TYPE);

        verify(chatPromptBuilder).createForPartnerLoveTypeInference(stage2Member, chatRoom, 2);
        verify(chatPromptBuilder).createForNextStage(stage2Member, chatRoom, 2);
        verify(chatRoomCommandHelper).upgradeChatRoomLevel(10L, 2, 1);
    }

    @Test
    @DisplayName("이미 partnerLoveTypeCategory가 확정되어 있으면 추론과 저장을 생략한다")
    void processStreamChatMessage_skipsInferenceWhenPartnerLoveTypeAlreadyKnown() {
        Member stage2Member = createMember(1L, PartnerLoveTypeCategory.ANXIETY_TYPE);
        ChatRoom chatRoom = createChatRoom(10L, 1, 2);
        DetailedPrompt lastDetailedPrompt = DetailedPrompt.create(1, 2, "guideline", false, false, "상황 요약", true, true);
        Prompt systemPrompt = Prompt.from(1L, 0, "system", true, false, false, false, false, false, false, null, null, null);
        Prompt nextPrompt = Prompt.from(2L, 2, "next", false, false, false, false, true, false, false, null, null, null);
        DetailedPrompt nextDetailedPrompt = DetailedPrompt.create(2, 1, "next-detailed", false, false, "분석", true, true);
        ProcessMessageUseCase.ProcessMessageCommand command = ProcessMessageUseCase.ProcessMessageCommand.builder()
                .memberId(1L)
                .chatRoomId(10L)
                .nowMessage("메시지")
                .promptLevel(1)
                .detailedLevel(2)
                .build();
        SufficiencyCheckResult result = SufficiencyCheckResult.builder()
                .completed(true)
                .summary("1단계 상황 요약")
                .build();

        when(memberQueryHelper.getMemberByIdOrThrow(MemberId.of(1L))).thenReturn(stage2Member);
        when(chatRoomQueryHelper.getChatRoomByIdOrThrow(any())).thenReturn(chatRoom);
        when(chatProcessor.requestSufficiencyCheck(anyList(), any())).thenReturn(CompletableFuture.completedFuture(result));
        when(chatPromptBuilder.createForSufficiencyCheck(stage2Member, chatRoom, 1, 2)).thenReturn(List.of());
        when(detailedPromptQueryHelper.getValidationPrompt(1, 2)).thenReturn(java.util.Optional.of(lastDetailedPrompt));
        when(detailedPromptQueryHelper.getGuidelinePrompt(1, 2)).thenReturn(java.util.Optional.of(lastDetailedPrompt));
        when(promptQueryHelper.getSystemPrompt()).thenReturn(systemPrompt);
        when(promptQueryHelper.getGuidelinePromptWithFallback(2)).thenReturn(nextPrompt);
        when(detailedPromptQueryHelper.getGuidelinePromptWithFallback(2, 1)).thenReturn(nextDetailedPrompt);
        when(chatPromptBuilder.createForNextStage(stage2Member, chatRoom, 2)).thenReturn(List.of());
        stubStreamChatCompletion();

        chatMessageService.processStreamChatMessage(command).join();

        verify(chatProcessor, never()).requestPartnerLoveTypeCategoryInference(anyList(), anyString());
        verify(memberCommandHelper, never()).saveMember(any());
        verify(chatPromptBuilder).createForNextStage(stage2Member, chatRoom, 2);
    }

    @Test
    @DisplayName("2단계 첫 분석 메시지 이후 추론이 실패해도 저장하지 않고 흐름은 계속 진행한다")
    void processStreamChatMessage_continuesWhenInferenceFailsAfterStage2Opening() {
        Member stage2Member = createMember(1L, PartnerLoveTypeCategory.UNKNOWN);
        Member refreshedMember = createMember(1L, PartnerLoveTypeCategory.UNKNOWN);
        ChatRoom chatRoom = createChatRoom(10L, 1, 2);
        DetailedPrompt lastDetailedPrompt = DetailedPrompt.create(1, 2, "guideline", false, false, "상황 요약", true, true);
        Prompt systemPrompt = Prompt.from(1L, 0, "system", true, false, false, false, false, false, false, null, null, null);
        Prompt nextPrompt = Prompt.from(2L, 2, "next", false, false, false, false, true, false, false, null, null, null);
        DetailedPrompt nextDetailedPrompt = DetailedPrompt.create(2, 1, "next-detailed", false, false, "분석", true, true);
        ProcessMessageUseCase.ProcessMessageCommand command = ProcessMessageUseCase.ProcessMessageCommand.builder()
                .memberId(1L)
                .chatRoomId(10L)
                .nowMessage("메시지")
                .promptLevel(1)
                .detailedLevel(2)
                .build();
        SufficiencyCheckResult result = SufficiencyCheckResult.builder()
                .completed(true)
                .summary("1단계 상황 요약")
                .build();

        when(memberQueryHelper.getMemberByIdOrThrow(MemberId.of(1L))).thenReturn(stage2Member, refreshedMember);
        when(chatRoomQueryHelper.getChatRoomByIdOrThrow(any())).thenReturn(chatRoom);
        when(chatProcessor.requestSufficiencyCheck(anyList(), any())).thenReturn(CompletableFuture.completedFuture(result));
        when(chatPromptBuilder.createForSufficiencyCheck(stage2Member, chatRoom, 1, 2)).thenReturn(List.of());
        when(detailedPromptQueryHelper.getValidationPrompt(1, 2)).thenReturn(java.util.Optional.of(lastDetailedPrompt));
        when(detailedPromptQueryHelper.getGuidelinePrompt(1, 2)).thenReturn(java.util.Optional.of(lastDetailedPrompt));
        when(chatPromptBuilder.createForPartnerLoveTypeInference(stage2Member, chatRoom, 2))
                .thenReturn(List.of(Map.of("role", "system", "content", "context")));
        when(chatProcessor.requestPartnerLoveTypeCategoryInference(anyList(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalArgumentException("invalid")));
        when(promptQueryHelper.getSystemPrompt()).thenReturn(systemPrompt);
        when(promptQueryHelper.getGuidelinePromptWithFallback(2)).thenReturn(nextPrompt);
        when(detailedPromptQueryHelper.getGuidelinePromptWithFallback(2, 1)).thenReturn(nextDetailedPrompt);
        when(chatPromptBuilder.createForNextStage(stage2Member, chatRoom, 2)).thenReturn(List.of());
        stubStreamChatCompletion();

        chatMessageService.processStreamChatMessage(command).join();

        verify(memberCommandHelper, never()).saveMember(any());
        verify(chatPromptBuilder).createForNextStage(stage2Member, chatRoom, 2);
        verify(chatRoomCommandHelper).upgradeChatRoomLevel(10L, 2, 1);
    }

    @Test
    @DisplayName("2단계 종료 시점에는 더 이상 추론하지 않는다")
    void processStreamChatMessage_skipsInferenceAtEndOfStage2() {
        Member stage2Member = createMember(1L, null);
        ChatRoom chatRoom = createChatRoom(10L, 2, 2);
        DetailedPrompt lastDetailedPrompt = DetailedPrompt.create(2, 2, "guideline", false, false, "분석", true, true);
        ProcessMessageUseCase.ProcessMessageCommand command = ProcessMessageUseCase.ProcessMessageCommand.builder()
                .memberId(1L)
                .chatRoomId(10L)
                .nowMessage("메시지")
                .promptLevel(2)
                .detailedLevel(2)
                .build();
        SufficiencyCheckResult result = SufficiencyCheckResult.builder()
                .completed(true)
                .summary("2단계 분석")
                .build();

        when(memberQueryHelper.getMemberByIdOrThrow(MemberId.of(1L))).thenReturn(stage2Member);
        when(chatRoomQueryHelper.getChatRoomByIdOrThrow(any())).thenReturn(chatRoom);
        when(chatProcessor.requestSufficiencyCheck(anyList(), any())).thenReturn(CompletableFuture.completedFuture(result));
        when(chatPromptBuilder.createForSufficiencyCheck(stage2Member, chatRoom, 2, 2)).thenReturn(List.of());
        when(detailedPromptQueryHelper.getValidationPrompt(2, 2)).thenReturn(java.util.Optional.of(lastDetailedPrompt));
        when(detailedPromptQueryHelper.getGuidelinePrompt(2, 2)).thenReturn(java.util.Optional.of(lastDetailedPrompt));
        when(promptQueryHelper.getSystemPrompt()).thenReturn(Prompt.from(1L, 0, "system", true, false, false, false, false, false, false, null, null, null));
        when(promptQueryHelper.getGuidelinePromptWithFallback(3)).thenReturn(Prompt.from(2L, 3, "stage3", false, false, false, false, true, false, false, null, null, null));
        when(detailedPromptQueryHelper.getGuidelinePromptWithFallback(3, 1)).thenReturn(DetailedPrompt.create(3, 1, "next", false, false, "다음", true, true));
        when(chatPromptBuilder.createForNextStage(stage2Member, chatRoom, 3)).thenReturn(List.of());
        stubStreamChatCompletion();

        chatMessageService.processStreamChatMessage(command).join();

        verify(chatProcessor, never()).requestPartnerLoveTypeCategoryInference(anyList(), anyString());
        verify(chatRoomCommandHelper).upgradeChatRoomLevel(10L, 3, 1);
    }

    private void stubCompletedFlow(
            Member stage2Member,
            Member refreshedMember,
            ChatRoom chatRoom,
            DetailedPrompt lastDetailedPrompt,
            Prompt systemPrompt,
            Prompt nextPrompt,
            DetailedPrompt nextDetailedPrompt,
            SufficiencyCheckResult result,
            Member nextStageMember
    ) {
        when(memberQueryHelper.getMemberByIdOrThrow(MemberId.of(1L)))
                .thenReturn(stage2Member, refreshedMember);
        when(chatRoomQueryHelper.getChatRoomByIdOrThrow(any())).thenReturn(chatRoom);
        when(chatProcessor.requestSufficiencyCheck(anyList(), any())).thenReturn(CompletableFuture.completedFuture(result));
        when(chatPromptBuilder.createForSufficiencyCheck(stage2Member, chatRoom, 1, 2)).thenReturn(List.of());
        when(detailedPromptQueryHelper.getValidationPrompt(1, 2)).thenReturn(java.util.Optional.of(lastDetailedPrompt));
        when(detailedPromptQueryHelper.getGuidelinePrompt(1, 2)).thenReturn(java.util.Optional.of(lastDetailedPrompt));
        when(chatPromptBuilder.createForPartnerLoveTypeInference(stage2Member, chatRoom, 2))
                .thenReturn(List.of(Map.of("role", "system", "content", "context")));
        when(memberCommandHelper.saveMember(any())).thenReturn(nextStageMember);
        when(promptQueryHelper.getSystemPrompt()).thenReturn(systemPrompt);
        when(promptQueryHelper.getGuidelinePromptWithFallback(2)).thenReturn(nextPrompt);
        when(detailedPromptQueryHelper.getGuidelinePromptWithFallback(2, 1)).thenReturn(nextDetailedPrompt);
        when(chatPromptBuilder.createForNextStage(stage2Member, chatRoom, 2)).thenReturn(List.of());
        stubStreamChatCompletion();
    }

    private void stubStreamChatCompletion() {
        doAnswer(invocation -> {
            Consumer<String> onComplete = invocation.getArgument(6);
            onComplete.accept("다음 단계 응답");
            return Mono.empty();
        }).when(chatProcessor).streamChat(anyList(), any(), any(), any(), any(), any(), any(), any());
    }

    private Member createMember(Long id, PartnerLoveTypeCategory partnerLoveTypeCategory) {
        LocalDateTime now = LocalDateTime.now();
        return Member.from(
                id,
                Provider.KAKAO,
                "provider-id",
                MemberRole.MEMBER,
                MemberState.ALIVE,
                false,
                null,
                null,
                null,
                0.0f,
                0.0f,
                "tak",
                "tak@example.com",
                EmailForwardingStatus.ENABLED,
                InviteCodeValue.of("ABCD1234"),
                null,
                null,
                null,
                RelationshipStatus.IN_RELATIONSHIP,
                "INTJ",
                "ENFP",
                partnerLoveTypeCategory,
                now,
                now,
                null
        );
    }

    private ChatRoom createChatRoom(Long id, int level, int detailedLevel) {
        LocalDateTime now = LocalDateTime.now();
        return ChatRoom.from(
                id,
                MemberId.of(1L),
                ChatRoomState.ALIVE,
                level,
                detailedLevel,
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                now,
                null
        );
    }
}
