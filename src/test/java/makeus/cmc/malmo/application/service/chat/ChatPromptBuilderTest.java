package makeus.cmc.malmo.application.service.chat;

import makeus.cmc.malmo.application.helper.chat_room.ChatRoomQueryHelper;
import makeus.cmc.malmo.application.helper.chat_room.MemberChatRoomMetadataQueryHelper;
import makeus.cmc.malmo.application.helper.love_type.LoveTypePersonalityTypePromptQueryHelper;
import makeus.cmc.malmo.application.port.out.chat.LoadChatRoomMetadataPort;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.love_type.LoveTypePersonalityTypePrompt;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.type.EmailForwardingStatus;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.PartnerLoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.Provider;
import makeus.cmc.malmo.domain.value.type.RelationshipStatus;
import makeus.cmc.malmo.domain.value.state.MemberState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatPromptBuilder 테스트")
class ChatPromptBuilderTest {

    private static final String UNKNOWN_INFERENCE_PROMPT = "UNKNOWN, 사용자와의 대화로부터 유추할 것";

    @Mock
    private ChatRoomQueryHelper chatRoomQueryHelper;

    @Mock
    private MemberChatRoomMetadataQueryHelper memberChatRoomMetadataQueryHelper;

    @Mock
    private LoveTypePersonalityTypePromptQueryHelper loveTypePersonalityTypePromptQueryHelper;

    @InjectMocks
    private ChatPromptBuilder chatPromptBuilder;

    @Test
    @DisplayName("사용자와 상대방 조합 프롬프트를 메타데이터에 삽입한다")
    void createForProcessUserMessage_includesUserAndPartnerPrompts() {
        // given
        Member member = createMember("ISTJ", LoveTypeCategory.STABLE_TYPE, "ENFP", PartnerLoveTypeCategory.ANXIETY_TYPE);
        ChatRoom chatRoom = createChatRoom(1L);

        stubCommon(chatRoom, LoveTypeCategory.STABLE_TYPE, PartnerLoveTypeCategory.ANXIETY_TYPE);
        when(loveTypePersonalityTypePromptQueryHelper.findByPersonalityTypeAndLoveTypeCategory("ISTJ", LoveTypeCategory.STABLE_TYPE))
                .thenReturn(Optional.of(LoveTypePersonalityTypePrompt.from("ISTJ", LoveTypeCategory.STABLE_TYPE, "ISTJ 안정형 프롬프트")));
        when(loveTypePersonalityTypePromptQueryHelper.findByPersonalityTypeAndLoveTypeCategory("ENFP", LoveTypeCategory.ANXIETY_TYPE))
                .thenReturn(Optional.of(LoveTypePersonalityTypePrompt.from("ENFP", LoveTypeCategory.ANXIETY_TYPE, "ENFP 불안형 프롬프트")));

        // when
        List<Map<String, String>> messages = chatPromptBuilder.createForProcessUserMessage(member, chatRoom, "사용자 메시지");

        // then
        assertThat(messages).hasSize(2);
        String metadata = messages.get(0).get("content");
        assertThat(metadata).contains("- 사용자 성향 프롬프트:");
        assertThat(metadata).contains("ISTJ 안정형 프롬프트");
        assertThat(metadata).contains("- 상대방 성향 프롬프트:");
        assertThat(metadata).contains("ENFP 불안형 프롬프트");
    }

    @Test
    @DisplayName("사용자 정보가 없거나 상대방 애착유형이 UNKNOWN이면 폴백 문구를 삽입한다")
    void createForProcessUserMessage_usesFallbackForUnknownCases() {
        // given
        Member member = createMember(null, null, "ENFP", PartnerLoveTypeCategory.UNKNOWN);
        ChatRoom chatRoom = createChatRoom(2L);

        stubCommon(chatRoom, null, PartnerLoveTypeCategory.UNKNOWN);

        // when
        List<Map<String, String>> messages = chatPromptBuilder.createForProcessUserMessage(member, chatRoom, "사용자 메시지");

        // then
        String metadata = messages.get(0).get("content");
        assertThat(metadata).contains("- 사용자 성향 프롬프트:\n" + UNKNOWN_INFERENCE_PROMPT);
        assertThat(metadata).contains("- 상대방 성향 프롬프트:\n" + UNKNOWN_INFERENCE_PROMPT);
    }

    @Test
    @DisplayName("상대방 프로필이 없으면 상대방 성향 프롬프트 항목을 추가하지 않는다")
    void createForProcessUserMessage_omitsPartnerPromptWithoutPartnerProfile() {
        // given
        Member member = createMember("ISTJ", LoveTypeCategory.STABLE_TYPE, null, null);
        ChatRoom chatRoom = createChatRoom(3L);

        stubCommon(chatRoom, LoveTypeCategory.STABLE_TYPE, null);
        when(loveTypePersonalityTypePromptQueryHelper.findByPersonalityTypeAndLoveTypeCategory("ISTJ", LoveTypeCategory.STABLE_TYPE))
                .thenReturn(Optional.of(LoveTypePersonalityTypePrompt.from("ISTJ", LoveTypeCategory.STABLE_TYPE, "ISTJ 안정형 프롬프트")));

        // when
        List<Map<String, String>> messages = chatPromptBuilder.createForProcessUserMessage(member, chatRoom, "사용자 메시지");

        // then
        String metadata = messages.get(0).get("content");
        assertThat(metadata).contains("- 사용자 성향 프롬프트:\nISTJ 안정형 프롬프트");
        assertThat(metadata).doesNotContain("- 상대방 성향 프롬프트:");
    }

    @Test
    @DisplayName("조합 row가 없더라도 예외 없이 폴백 문구를 삽입한다")
    void createForProcessUserMessage_fallsBackWhenPromptRowMissing() {
        // given
        Member member = createMember("ISTJ", LoveTypeCategory.STABLE_TYPE, "ENFP", PartnerLoveTypeCategory.ANXIETY_TYPE);
        ChatRoom chatRoom = createChatRoom(4L);

        stubCommon(chatRoom, LoveTypeCategory.STABLE_TYPE, PartnerLoveTypeCategory.ANXIETY_TYPE);
        when(loveTypePersonalityTypePromptQueryHelper.findByPersonalityTypeAndLoveTypeCategory(any(), any()))
                .thenReturn(Optional.empty());

        // when
        List<Map<String, String>> messages = chatPromptBuilder.createForProcessUserMessage(member, chatRoom, "사용자 메시지");

        // then
        String metadata = messages.get(0).get("content");
        assertThat(metadata).contains("- 사용자 성향 프롬프트:\n" + UNKNOWN_INFERENCE_PROMPT);
        assertThat(metadata).contains("- 상대방 성향 프롬프트:\n" + UNKNOWN_INFERENCE_PROMPT);
    }

    @Test
    @DisplayName("다음 단계 첫 메시지 생성 시 이전 단계 대화 메시지를 포함한다")
    void createForNextStage_includesPreviousStageMessages() {
        // given
        Member member = createMember("ISTJ", LoveTypeCategory.STABLE_TYPE, "ENFP", PartnerLoveTypeCategory.ANXIETY_TYPE);
        ChatRoom chatRoom = createChatRoom(5L);
        ChatRoomId chatRoomId = ChatRoomId.of(chatRoom.getId());
        List<ChatMessage> previousStageMessages = List.of(
                ChatMessage.createUserTextMessage(chatRoomId, 1, 1, "사용자 고민"),
                ChatMessage.createAssistantTextMessage(chatRoomId, 1, 1, "상담 응답")
        );

        stubCommon(chatRoom, LoveTypeCategory.STABLE_TYPE, PartnerLoveTypeCategory.ANXIETY_TYPE);
        when(loveTypePersonalityTypePromptQueryHelper.findByPersonalityTypeAndLoveTypeCategory("ISTJ", LoveTypeCategory.STABLE_TYPE))
                .thenReturn(Optional.of(LoveTypePersonalityTypePrompt.from("ISTJ", LoveTypeCategory.STABLE_TYPE, "ISTJ 안정형 프롬프트")));
        when(loveTypePersonalityTypePromptQueryHelper.findByPersonalityTypeAndLoveTypeCategory("ENFP", LoveTypeCategory.ANXIETY_TYPE))
                .thenReturn(Optional.of(LoveTypePersonalityTypePrompt.from("ENFP", LoveTypeCategory.ANXIETY_TYPE, "ENFP 불안형 프롬프트")));
        when(chatRoomQueryHelper.getChatRoomLevelMessages(eq(chatRoomId), eq(1)))
                .thenReturn(previousStageMessages);

        // when
        List<Map<String, String>> messages = chatPromptBuilder.createForNextStage(member, chatRoom, 2);

        // then
        assertThat(messages).hasSize(3);
        assertThat(messages.get(1)).containsEntry("role", "user")
                .containsEntry("content", "사용자 고민");
        assertThat(messages.get(2)).containsEntry("role", "assistant")
                .containsEntry("content", "상담 응답");
        assertThat(messages).anySatisfy(message -> assertThat(message).containsEntry("role", "user"));
    }

    private void stubCommon(ChatRoom chatRoom, LoveTypeCategory userLoveType, PartnerLoveTypeCategory partnerLoveType) {
        when(chatRoomQueryHelper.getMemberMemoriesByMemberId(any())).thenReturn(List.of());
        when(chatRoomQueryHelper.getChatRoomMetadata(any()))
                .thenReturn(new LoadChatRoomMetadataPort.ChatRoomMetadataDto(userLoveType, partnerLoveType));
        when(memberChatRoomMetadataQueryHelper.getMemberChatRoomMetadata(any())).thenReturn(List.of());
        when(chatRoomQueryHelper.getChatRoomLevelMessages(any(), any(Integer.class))).thenReturn(List.of());
    }

    private Member createMember(
            String personalityType,
            LoveTypeCategory loveTypeCategory,
            String otherPersonalityType,
            PartnerLoveTypeCategory partnerLoveTypeCategory
    ) {
        LocalDateTime now = LocalDateTime.now();
        return Member.from(
                1L,
                Provider.KAKAO,
                "provider-id",
                MemberRole.MEMBER,
                MemberState.ALIVE,
                false,
                null,
                null,
                loveTypeCategory,
                1.0f,
                1.0f,
                "tak",
                "tak@example.com",
                EmailForwardingStatus.ENABLED,
                InviteCodeValue.of("ABCD1234"),
                null,
                null,
                null,
                RelationshipStatus.IN_RELATIONSHIP,
                personalityType,
                otherPersonalityType,
                partnerLoveTypeCategory,
                now,
                now,
                null
        );
    }

    private ChatRoom createChatRoom(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return ChatRoom.from(
                id,
                makeus.cmc.malmo.domain.value.id.MemberId.of(1L),
                makeus.cmc.malmo.domain.value.state.ChatRoomState.ALIVE,
                1,
                1,
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
