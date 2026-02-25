package makeus.cmc.malmo.application.service.chat;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.aop.CheckValidMember;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomCommandHelper;
import makeus.cmc.malmo.application.helper.chat_room.ChatRoomQueryHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.port.in.chat.DeleteChatRoomUseCase;
import makeus.cmc.malmo.application.port.in.chat.GetChatRoomListUseCase;
import makeus.cmc.malmo.application.port.in.chat.GetChatRoomMessagesUseCase;
import makeus.cmc.malmo.application.port.in.chat.GetChatRoomSummaryUseCase;
import makeus.cmc.malmo.application.port.out.chat.LoadMessagesPort;
import makeus.cmc.malmo.domain.model.chat.ChatMessageSummary;
import makeus.cmc.malmo.domain.model.chat.ChatRoom;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.RelationshipStatus;
import makeus.cmc.malmo.domain.value.type.SenderType;
import makeus.cmc.malmo.util.GlobalConstants;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ChatRoomService
        implements GetChatRoomSummaryUseCase, GetChatRoomListUseCase,
        GetChatRoomMessagesUseCase, DeleteChatRoomUseCase {

    private final ChatRoomQueryHelper chatRoomQueryHelper;
    private final ChatRoomCommandHelper chatRoomCommandHelper;
    private final MemberQueryHelper memberQueryHelper;

    @Override
    @CheckValidMember
    public GetChatRoomSummaryResponse getChatRoomSummary(GetChatRoomSummaryCommand command) {
        chatRoomQueryHelper.validateChatRoomOwnership(MemberId.of(command.getUserId()), ChatRoomId.of(command.getChatRoomId()));

        ChatRoom chatRoom = chatRoomQueryHelper.getChatRoomByIdOrThrow(ChatRoomId.of(command.getChatRoomId()));

        String totalSummary = chatRoom.getTotalSummary();
        List<ChatMessageSummary> summarizedMessages = chatRoomQueryHelper.getSummarizedMessages(ChatRoomId.of(chatRoom.getId()));
        String firstSummary = summarizedMessages.isEmpty() ? "" : summarizedMessages.get(0).getContent();
        String secondSummary = summarizedMessages.size() > 1 ? summarizedMessages.get(1).getContent() : "";
        String thirdSummary = summarizedMessages.size() > 2 ? summarizedMessages.get(2).getContent() : "";

        return GetChatRoomSummaryResponse.builder()
                .chatRoomId(chatRoom.getId())
                .createdAt(chatRoom.getCreatedAt())
                .totalSummary(totalSummary)
                .firstSummary(firstSummary)
                .secondSummary(secondSummary)
                .thirdSummary(thirdSummary)
                .build();
    }

    @Override
    @CheckValidMember
    public GetChatRoomListResponse getChatRoomList(GetChatRoomListCommand command) {
        Page<ChatRoom> chatRoomList = chatRoomQueryHelper.getChatRoomsByMemberId(
                MemberId.of(command.getUserId()), command.getKeyword(), command.getPageable()
        );

        List<GetChatRoomResponse> response = chatRoomList.getContent().stream()
                .map(chatRoom -> GetChatRoomResponse.builder()
                        .chatRoomId(chatRoom.getId())
                        .title(chatRoom.getTitle())  // 제목 (null일 수 있음)
                        .chatRoomState(chatRoom.getChatRoomState())  // 상태 포함
                        .level(chatRoom.getLevel())  // 현재 단계
                        .lastMessageSentTime(chatRoom.getLastMessageSentTime())
                        .createdAt(chatRoom.getCreatedAt())
                        .build())
                .toList();

        return GetChatRoomListResponse.builder()
                .chatRoomList(response)
                .totalCount(chatRoomList.getTotalElements())
                .build();
    }

    @Override
    @CheckValidMember
    public GetCurrentChatRoomMessagesResponse getChatRoomMessages(GetChatRoomMessagesCommand command) {
        MemberId memberId = MemberId.of(command.getUserId());
        ChatRoomId chatRoomId = ChatRoomId.of(command.getChatRoomId());
        chatRoomQueryHelper.validateChatRoomOwnership(memberId, chatRoomId);

        ChatRoom chatRoom = chatRoomQueryHelper.getChatRoomByIdOrThrow(chatRoomId);

        Page<LoadMessagesPort.ChatRoomMessageRepositoryDto> result =
                chatRoomQueryHelper.getChatMessagesDtoAsc(chatRoomId, memberId, command.getPageable());

        List<GetChatRoomMessagesUseCase.ChatRoomMessageDto> list = new ArrayList<>(result.stream().map(cm ->
                        GetChatRoomMessagesUseCase.ChatRoomMessageDto.builder()
                                .messageId(cm.getMessageId())
                                .senderType(cm.getSenderType())
                                .content(cm.getContent())
                                .createdAt(cm.getCreatedAt())
                                .bookmarkId(cm.getBookmarkId())
                                .build())
                .toList());

        boolean hasUserMessages = chatRoomQueryHelper.hasUserMessages(chatRoomId);
        Member member = memberQueryHelper.getMemberByIdOrThrow(memberId);

        LocalDateTime nextSyntheticTime = list.isEmpty()
                ? LocalDateTime.now()
                : list.get(list.size() - 1).getCreatedAt().plusNanos(1);

        // BEFORE_INIT 상태인 경우 두 번째 AI 메시지를 동적으로 삽입
        if (chatRoom.isBeforeInit()) {
            String secondMessageContent = getSecondMessageByRelationshipStatus(member.getRelationshipStatus());
            LocalDateTime secondMessageTime = list.isEmpty()
                    ? LocalDateTime.now()
                    : list.get(0).getCreatedAt().plusNanos(1);
            list.add(Math.min(1, list.size()), GetChatRoomMessagesUseCase.ChatRoomMessageDto.builder()
                    .messageId(null)
                    .senderType(SenderType.ASSISTANT)
                    .content(secondMessageContent)
                    .createdAt(secondMessageTime)
                    .bookmarkId(null)
                    .build());
            nextSyntheticTime = secondMessageTime.plusNanos(1);
        }

        if (!hasUserMessages && member.getLoveTypeCategory() == null) {
            // Append dynamic SYSTEM message (not persisted)
            list.add(GetChatRoomMessagesUseCase.ChatRoomMessageDto.builder()
                    .messageId(null)
                    .senderType(SenderType.SYSTEM)
                    .content(GlobalConstants.ATTACHMENT_TYPE_PROMPT_MESSAGE)
                    .createdAt(nextSyntheticTime)
                    .bookmarkId(null)
                    .build());
        }

        list.sort(Comparator
                .comparing(GetChatRoomMessagesUseCase.ChatRoomMessageDto::getCreatedAt)
                .thenComparing(dto -> dto.getMessageId() == null ? Long.MAX_VALUE : dto.getMessageId())
        );

        return GetCurrentChatRoomMessagesResponse.builder()
                .messages(list)
                .totalCount(result.getTotalElements())
                .build();
    }

    private String getSecondMessageByRelationshipStatus(RelationshipStatus status) {
        if (status == null) {
            return GlobalConstants.INIT_CHAT_MESSAGE_SECOND_SEEING_SOMEONE;
        }
        return switch (status) {
            case SEEING_SOMEONE -> GlobalConstants.INIT_CHAT_MESSAGE_SECOND_SEEING_SOMEONE;
            case IN_RELATIONSHIP -> GlobalConstants.INIT_CHAT_MESSAGE_SECOND_IN_RELATIONSHIP;
            case BREAKUP -> GlobalConstants.INIT_CHAT_MESSAGE_SECOND_BREAKUP;
        };
    }

    @Override
    @CheckValidMember
    @Transactional
    public void deleteChatRooms(DeleteChatRoomsCommand command) {
        // 모든 채팅방이 멤버 소유인지 검증
        chatRoomQueryHelper.validateChatRoomsOwnership(
                MemberId.of(command.getUserId()),
                command.getChatRoomIdList().stream().map(ChatRoomId::of).toList());

        chatRoomCommandHelper.deleteChatRooms(
                command.getChatRoomIdList().stream().map(ChatRoomId::of).toList()
        );
    }
}
