package makeus.cmc.malmo.application.helper.weekly_analysis_report;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatMessageEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.MemberChatRoomMetadataEntity;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.ChatMessageMapper;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.MemberChatRoomMetadataMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.chat.ChatMessageRepository;
import makeus.cmc.malmo.adaptor.out.persistence.repository.chat.DetailedPromptRepository;
import makeus.cmc.malmo.adaptor.out.persistence.repository.chat.MemberChatRoomMetadataRepository;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.MemberChatRoomMetadata;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.SenderType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class WeeklyAnalysisSourceQueryHelper {

    private static final int LEVEL_ONE = 1;

    private final DetailedPromptRepository detailedPromptRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberChatRoomMetadataRepository memberChatRoomMetadataRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final MemberChatRoomMetadataMapper memberChatRoomMetadataMapper;

    public int getLastDetailedLevelOfLevelOne() {
        return detailedPromptRepository.findLastDetailedLevelByLevel(LEVEL_ONE)
                .orElseThrow(() -> new IllegalStateException("Last detailed prompt for level 1 not found"));
    }

    public List<Long> getCandidateMemberIds(WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod) {
        int lastDetailedLevelOfLevelOne = getLastDetailedLevelOfLevelOne();

        Set<Long> candidateMemberIds = new LinkedHashSet<>();
        candidateMemberIds.addAll(
                chatMessageRepository.findDistinctMemberIdsBySenderTypeAndCreatedAtBetween(
                        SenderType.USER,
                        weekPeriod.getWeekStartAt(),
                        weekPeriod.getWeekEndAtExclusive()
                )
        );
        candidateMemberIds.addAll(
                memberChatRoomMetadataRepository.findDistinctCandidateMemberIdsByCreatedAtBetween(
                        weekPeriod.getWeekStartAt(),
                        weekPeriod.getWeekEndAtExclusive(),
                        lastDetailedLevelOfLevelOne
                )
        );
        return List.copyOf(candidateMemberIds);
    }

    public List<Long> getEligibleChatRoomIds(MemberId memberId, WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod) {
        return memberChatRoomMetadataRepository.findEligibleChatRoomIdsByMemberIdAndWeek(
                memberId.getValue(),
                weekPeriod.getWeekStartAt(),
                weekPeriod.getWeekEndAtExclusive(),
                getLastDetailedLevelOfLevelOne()
        );
    }

    public long countWeeklyUserMessages(MemberId memberId, WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod) {
        return chatMessageRepository.countByMemberIdAndSenderTypeAndCreatedAtBetween(
                memberId.getValue(),
                SenderType.USER,
                weekPeriod.getWeekStartAt(),
                weekPeriod.getWeekEndAtExclusive()
        );
    }

    public List<ChatMessage> getWeeklyUserMessages(MemberId memberId, WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod) {
        List<ChatMessageEntity> entities = chatMessageRepository.findByMemberIdAndSenderTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
                memberId.getValue(),
                SenderType.USER,
                weekPeriod.getWeekStartAt(),
                weekPeriod.getWeekEndAtExclusive()
        );
        return entities.stream()
                .map(chatMessageMapper::toDomain)
                .toList();
    }

    public List<MemberChatRoomMetadata> getAllMetadataByChatRoomIds(List<Long> chatRoomIds) {
        if (chatRoomIds == null || chatRoomIds.isEmpty()) {
            return List.of();
        }
        List<MemberChatRoomMetadataEntity> entities = memberChatRoomMetadataRepository
                .findAllByChatRoomIdInOrderByChatRoomIdAscLevelAscDetailedLevelAsc(chatRoomIds);
        return entities.stream()
                .map(memberChatRoomMetadataMapper::toDomain)
                .toList();
    }

    public List<ChatMessage> getFallbackUserMessages(List<Long> chatRoomIds, WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod) {
        if (chatRoomIds == null || chatRoomIds.isEmpty()) {
            return List.of();
        }

        List<ChatMessageEntity> entities = chatMessageRepository.findByChatRoomIdsAndSenderTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                chatRoomIds,
                SenderType.USER,
                weekPeriod.getWeekStartAt(),
                weekPeriod.getWeekEndAtExclusive()
        );

        return entities.stream()
                .map(chatMessageMapper::toDomain)
                .limit(20)
                .toList();
    }
}
