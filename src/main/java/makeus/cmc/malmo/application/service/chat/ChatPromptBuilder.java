package makeus.cmc.malmo.application.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomQueryHelper;
import makeus.cmc.malmo.application.helper.chat_room.MemberChatRoomMetadataQueryHelper;
import makeus.cmc.malmo.application.helper.love_type.LoveTypeMbtiPromptQueryHelper;
import makeus.cmc.malmo.application.port.out.chat.LoadChatRoomMetadataPort;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.chat.MemberChatRoomMetadata;
import makeus.cmc.malmo.util.ChatTokenConstants;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.model.member.MemberMemory;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.PartnerLoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.SenderType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPromptBuilder {

    private static final String UNKNOWN_INFERENCE_PROMPT = "UNKNOWN, 사용자와의 대화로부터 유추할 것";

    private final ChatRoomQueryHelper chatRoomQueryHelper;
    private final MemberChatRoomMetadataQueryHelper memberChatRoomMetadataQueryHelper;
    private final LoveTypeMbtiPromptQueryHelper loveTypeMbtiPromptQueryHelper;

    public List<Map<String, String>> createForProcessUserMessage(Member member, ChatRoom chatRoom, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        int chatRoomLevel = chatRoom.getLevel();

        // 1. 사용자 메타데이터
        String metaDataContent = getMetaDataContent(member);
        messages.add(createMessageMap(SenderType.SYSTEM, metaDataContent));

        // 2. MemberChatRoomMetadata 정보 (단계별 요약 대신)
        List<MemberChatRoomMetadata> metadataList = memberChatRoomMetadataQueryHelper.getMemberChatRoomMetadata(ChatRoomId.of(chatRoom.getId()));
        if (!metadataList.isEmpty()) {
            String metadataContent = getMemberChatRoomMetadataContent(metadataList);
            messages.add(createMessageMap(SenderType.SYSTEM, metadataContent));
        }

        // 3. 현재 단계 메시지들
        List<ChatMessage> currentChatRoomMessages = chatRoomQueryHelper.getChatRoomLevelMessages(ChatRoomId.of(chatRoom.getId()), chatRoomLevel);
        for (ChatMessage chatMessage : currentChatRoomMessages) {
            messages.add(createMessageMap(chatMessage.getSenderType(), chatMessage.getContent()));
        }

        if (currentChatRoomMessages.isEmpty()) {
            // 만약 현재 단계 메시지가 없다면, 이전 단계 메시지를 가져온다. (현재 단계 최초 진입)
            currentChatRoomMessages = chatRoomQueryHelper.getChatRoomLevelMessages(ChatRoomId.of(chatRoom.getId()), chatRoomLevel - 1);
            for (ChatMessage chatMessage : currentChatRoomMessages) {
                messages.add(createMessageMap(chatMessage.getSenderType(), chatMessage.getContent()));
            }
        }

        // 4. 현재 사용자 메시지 추가
        messages.add(createMessageMap(SenderType.USER, userMessage));

        return messages;
    }



    private Map<String, String> createMessageMap(SenderType senderType, String content) {
        return Map.of(
                "role", senderType.getApiName(),
                "content", content
        );
    }

    private String getMetaDataContent(Member member) {
        String memberMemoryList = getMemberMemoriesByMemberId(MemberId.of(member.getId()));

        StringBuilder metadataBuilder = new StringBuilder();
        metadataBuilder.append("[사용자 메타데이터] \n");
        String nickname = member.getNickname();
        metadataBuilder.append("- 사용자 이름: ").append(nickname).append("\n");

        String relationshipStatus = member.getRelationshipStatus() != null ? member.getRelationshipStatus().name() : "알 수 없음";
        metadataBuilder.append("- 사용자 연애 상태: ").append(relationshipStatus).append("\n");

        String mbti = member.getMbti() != null ? member.getMbti() : "알 수 없음";
        metadataBuilder.append("- 사용자 MBTI: ").append(mbti).append("\n");

        String partnerMbti = member.getPartnerMbti() != null ? member.getPartnerMbti() : "알 수 없음";
        metadataBuilder.append("- 상대방 MBTI: ").append(partnerMbti).append("\n");

//        String dDayState = memberDomainService.getMemberDDayState(member.getStartLoveDate());
//        metadataBuilder.append("- 연애 기간: ").append(dDayState).append("\n");

        LoadChatRoomMetadataPort.ChatRoomMetadataDto chatRoomMetadataDto = chatRoomQueryHelper.getChatRoomMetadata(MemberId.of(member.getId()));
        String memberLoveTypeTitle = chatRoomMetadataDto.memberLoveType() != null ? chatRoomMetadataDto.memberLoveType().getTitle() : "알 수 없음";
        metadataBuilder.append("- 사용자 애착 유형: ").append(memberLoveTypeTitle).append("\n");

        String partnerLoveType = chatRoomMetadataDto.partnerLoveType() != null ? chatRoomMetadataDto.partnerLoveType().getDescription() : "알 수 없음";
        metadataBuilder.append("- 애인 애착 유형: ").append(partnerLoveType).append("\n");
        metadataBuilder.append("- 사용자 성향 프롬프트:\n")
                .append(resolveMemberPrompt(member))
                .append("\n");

        if (StringUtils.hasText(member.getPartnerMbti())) {
            metadataBuilder.append("- 상대방 성향 프롬프트:\n")
                    .append(resolvePartnerPrompt(member))
                    .append("\n");
        }

        metadataBuilder.append(memberMemoryList);

        return metadataBuilder.toString();
    }

    private String resolveMemberPrompt(Member member) {
        if (!StringUtils.hasText(member.getMbti()) || member.getLoveTypeCategory() == null) {
            return UNKNOWN_INFERENCE_PROMPT;
        }

        return loveTypeMbtiPromptQueryHelper.findByMbtiAndLoveTypeCategory(member.getMbti(), member.getLoveTypeCategory())
                .map(prompt -> prompt.getPrompts())
                .filter(StringUtils::hasText)
                .orElseGet(() -> {
                    log.warn("Missing love_type_mbti_prompt row for member. mbti={}, loveType={}",
                            member.getMbti(), member.getLoveTypeCategory());
                    return UNKNOWN_INFERENCE_PROMPT;
                });
    }

    private String resolvePartnerPrompt(Member member) {
        if (!StringUtils.hasText(member.getPartnerMbti())) {
            return UNKNOWN_INFERENCE_PROMPT;
        }

        if (member.getPartnerLoveTypeCategory() == null || member.getPartnerLoveTypeCategory() == PartnerLoveTypeCategory.UNKNOWN) {
            return UNKNOWN_INFERENCE_PROMPT;
        }

        LoveTypeCategory partnerLoveTypeCategory = LoveTypeCategory.valueOf(member.getPartnerLoveTypeCategory().name());
        return loveTypeMbtiPromptQueryHelper.findByMbtiAndLoveTypeCategory(member.getPartnerMbti(), partnerLoveTypeCategory)
                .map(prompt -> prompt.getPrompts())
                .filter(StringUtils::hasText)
                .orElseGet(() -> {
                    log.warn("Missing love_type_mbti_prompt row for partner. memberId={}, partnerMbti={}, partnerLoveType={}",
                            member.getId(), member.getPartnerMbti(), partnerLoveTypeCategory);
                    return UNKNOWN_INFERENCE_PROMPT;
                });
    }

    public String getMemberMemoriesByMemberId(MemberId memberId) {
        StringBuilder sb = new StringBuilder();
        List<MemberMemory> memoryList = chatRoomQueryHelper.getMemberMemoriesByMemberId(memberId);

        for (MemberMemory memberMemory : memoryList) {
            sb.append("- ").append(memberMemory.getContent()).append("\n");
        }

        return sb.toString();
    }

    public List<Map<String, String>> createForSufficiencyCheck(Member member, ChatRoom chatRoom, int level, int detailedLevel) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 1. 사용자 메타데이터
        String metaDataContent = getMetaDataContent(member);
        messages.add(createMessageMap(SenderType.SYSTEM, metaDataContent));

        // 2. MemberChatRoomMetadata 정보 (단계별 요약 대신)
        List<MemberChatRoomMetadata> metadataList = memberChatRoomMetadataQueryHelper.getMemberChatRoomMetadata(ChatRoomId.of(chatRoom.getId()));
        if (!metadataList.isEmpty()) {
            String metadataContent = getMemberChatRoomMetadataContent(metadataList);
            messages.add(createMessageMap(SenderType.SYSTEM, metadataContent));
        }

        // 3. 현재 단계 메시지들
        List<ChatMessage> currentChatRoomMessages = chatRoomQueryHelper.getChatRoomLevelAndDetailedLevelMessages(ChatRoomId.of(chatRoom.getId()), level, detailedLevel);
        for (ChatMessage chatMessage : currentChatRoomMessages) {
            messages.add(createMessageMap(chatMessage.getSenderType(), chatMessage.getContent()));
        }

        return messages;
    }


    public List<Map<String, String>> createForNextDetailedPrompt(Member member, ChatRoom chatRoom, int level, int nextDetailedLevel) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 1. 사용자 메타데이터
        String metaDataContent = getMetaDataContent(member);
        messages.add(createMessageMap(SenderType.SYSTEM, metaDataContent));

        // 2. MemberChatRoomMetadata 정보
        List<MemberChatRoomMetadata> metadataList = memberChatRoomMetadataQueryHelper.getMemberChatRoomMetadata(ChatRoomId.of(chatRoom.getId()));
        if (!metadataList.isEmpty()) {
            String metadataContent = getMemberChatRoomMetadataContent(metadataList);
            messages.add(createMessageMap(SenderType.SYSTEM, metadataContent));
        }

        // 3. 현재 단계 메시지들 (이전 detailedLevel까지)
//        List<ChatMessage> currentChatRoomMessages = chatRoomQueryHelper.getChatRoomLevelAndDetailedLevelMessages(ChatRoomId.of(chatRoom.getId()), level, nextDetailedLevel - 1);
        // fixed: 현재 단계 메시지들 context 전체 전달(level 기준)
        List<ChatMessage> currentChatRoomMessages = chatRoomQueryHelper.getChatRoomLevelMessages(ChatRoomId.of(chatRoom.getId()), level);
        for (ChatMessage chatMessage : currentChatRoomMessages) {
            messages.add(createMessageMap(chatMessage.getSenderType(), chatMessage.getContent()));
        }

        return messages;
    }

    public List<Map<String, String>> createForNextStage(Member member, ChatRoom chatRoom, int nextLevel) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 1. 사용자 메타데이터
        String metaDataContent = getMetaDataContent(member);
        messages.add(createMessageMap(SenderType.SYSTEM, metaDataContent));

        // 2. MemberChatRoomMetadata 정보
        List<MemberChatRoomMetadata> metadataList = memberChatRoomMetadataQueryHelper.getMemberChatRoomMetadata(ChatRoomId.of(chatRoom.getId()));
        if (!metadataList.isEmpty()) {
            String metadataContent = getMemberChatRoomMetadataContent(metadataList);
            messages.add(createMessageMap(SenderType.SYSTEM, metadataContent));
        }

        return messages;
    }

    private String getMemberChatRoomMetadataContent(List<MemberChatRoomMetadata> metadataList) {
        if (metadataList == null || metadataList.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[사용자의 갈등 내용] \n");
        for (MemberChatRoomMetadata metadata : metadataList) {
            sb.append("- ").append(metadata.getTitle()).append(": ").append(metadata.getSummary()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 제목 생성을 위한 메시지 구성
     * 1단계 대화 내용만 포함
     */
    public List<Map<String, String>> createForTitleGeneration(ChatRoom chatRoom) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 1단계 메시지들만 조회
        List<ChatMessage> stage1Messages = chatRoomQueryHelper.getChatRoomLevelMessages(
                ChatRoomId.of(chatRoom.getId()), 1);
        
        for (ChatMessage chatMessage : stage1Messages) {
            messages.add(createMessageMap(chatMessage.getSenderType(), chatMessage.getContent()));
        }
        
        return messages;
    }

    /**
     * 4단계 자유 대화를 위한 메시지 구성
     * - 최근 20개 메시지만 로드하여 토큰 관리
     * - 이전 대화 요약 포함
     */
    public List<Map<String, String>> createForFreeConversation(Member member, ChatRoom chatRoom, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        int chatRoomLevel = chatRoom.getLevel();
        ChatRoomId chatRoomId = ChatRoomId.of(chatRoom.getId());

        // 1. 사용자 메타데이터
        String metaDataContent = getMetaDataContent(member);
        messages.add(createMessageMap(SenderType.SYSTEM, metaDataContent));

        // 2. 이전 단계 요약 (MemberChatRoomMetadata)
        List<MemberChatRoomMetadata> metadataList = memberChatRoomMetadataQueryHelper.getMemberChatRoomMetadata(chatRoomId);
        if (!metadataList.isEmpty()) {
            String metadataContent = getMemberChatRoomMetadataContent(metadataList);
            messages.add(createMessageMap(SenderType.SYSTEM, metadataContent));
        }

        // 3. 4단계 대화 요약 (ChatMessageSummary) - 있는 경우
        chatRoomQueryHelper.getLatestSummaryByLevel(chatRoomId, chatRoomLevel)
                .ifPresent(summary -> {
                    String summaryContent = "[이전 대화 요약]\n" + summary.getContent();
                    messages.add(createMessageMap(SenderType.SYSTEM, summaryContent));
                });

        // 4. 최근 N개 메시지만 로드 (토큰 관리)
        List<ChatMessage> recentMessages = chatRoomQueryHelper.getRecentMessages(
                chatRoomId, chatRoomLevel, ChatTokenConstants.FREE_CONVERSATION_RECENT_MESSAGE_LIMIT);
        for (ChatMessage chatMessage : recentMessages) {
            messages.add(createMessageMap(chatMessage.getSenderType(), chatMessage.getContent()));
        }

        // 5. 현재 사용자 메시지 추가
        messages.add(createMessageMap(SenderType.USER, userMessage));

        return messages;
    }
}
