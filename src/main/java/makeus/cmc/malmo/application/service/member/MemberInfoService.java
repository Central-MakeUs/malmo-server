package makeus.cmc.malmo.application.service.member;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.aop.CheckValidMember;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.port.in.member.GetMemberUseCase;
import makeus.cmc.malmo.application.port.in.member.GetPartnerUseCase;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.MemberState;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberInfoService implements GetMemberUseCase, GetPartnerUseCase {

    private final MemberQueryHelper memberQueryHelper;

    @Override
    @CheckValidMember
    public MemberResponseDto getMemberInfo(MemberInfoCommand command) {
        MemberQueryHelper.MemberInfoDto member = memberQueryHelper.getMemberInfoOrThrow(MemberId.of(command.getUserId()));

        return MemberResponseDto.builder()
                .memberState(MemberState.valueOf(member.getMemberState()))
                .provider(member.getProvider())
                .startLoveDate(member.getStartLoveDate())
                .avoidanceRate(member.getAvoidanceRate())
                .anxietyRate(member.getAnxietyRate())
                .loveTypeCategory(member.getLoveTypeCategory())
                .totalChatRoomCount(member.getTotalChatRoomCount())
                .totalCoupleQuestionCount(member.getTotalCoupleQuestionCount())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .relationshipStatus(member.getRelationshipStatus())
                .personalityType(member.getPersonalityType())
                .otherPersonalityType(member.getOtherPersonalityType())
                .partnerLoveTypeCategory(member.getPartnerLoveTypeCategory())
                .build();
    }

    @Override
    @CheckValidMember
    public PartnerMemberResponseDto getPartnerInfo(PartnerInfoCommand command) {
        MemberQueryHelper.PartnerMemberDto partner = memberQueryHelper.getPartnerInfoOrThrow(MemberId.of(command.getUserId()));

        return PartnerMemberResponseDto.builder()
                .personalityType(partner.getPersonalityType())
                .loveTypeCategory(partner.getLoveTypeCategory())
                .description(partner.getDescription())
                .build();
    }
}
