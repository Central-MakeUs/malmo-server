package makeus.cmc.malmo.application.service;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.aop.CheckValidMember;
import makeus.cmc.malmo.application.port.in.UpdateMemberLoveTypeUseCase;
import makeus.cmc.malmo.application.port.out.SaveMemberPort;
import makeus.cmc.malmo.application.service.helper.love_type.LoveTypeQueryHelper;
import makeus.cmc.malmo.application.service.helper.member.MemberCommandHelper;
import makeus.cmc.malmo.application.service.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.service.LoveTypeCalculator;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoveTypeService implements UpdateMemberLoveTypeUseCase {

    private final LoveTypeCalculator loveTypeCalculator;
    private final LoveTypeQueryHelper loveTypeQueryHelper;
    private final MemberQueryHelper memberQueryHelper;
    private final MemberCommandHelper memberCommandHelper;

    @Override
    @Transactional
    @CheckValidMember
    public void updateMemberLoveType(UpdateMemberLoveTypeCommand command) {
        Member member = memberQueryHelper.getMemberByIdOrThrow(MemberId.of(command.getMemberId()));

        List<LoveTypeCalculator.TestResultInput> testResultInputs = command.getResults().stream()
                .map(result -> new LoveTypeCalculator.TestResultInput(result.getQuestionId(), result.getScore()))
                .collect(Collectors.toList());

        LoveTypeCalculator.LoveTypeCalculationResult calculationResult =
                loveTypeCalculator.calculate(testResultInputs, loveTypeQueryHelper::getQuestionById);

        member.updateLoveTypeId(calculationResult.category(),
                calculationResult.avoidanceScore(),
                calculationResult.anxietyScore());

        memberCommandHelper.saveMember(member);
    }
}
