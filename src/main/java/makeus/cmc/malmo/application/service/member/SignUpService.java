package makeus.cmc.malmo.application.service.member;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.aop.CheckValidMember;
import makeus.cmc.malmo.application.helper.love_type.TempLoveTypeHelper;
import makeus.cmc.malmo.application.helper.member.MemberCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.helper.terms.TermsCommandHelper;
import makeus.cmc.malmo.application.helper.terms.TermsQueryHelper;
import makeus.cmc.malmo.application.port.in.member.SignUpUseCase;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.model.terms.MemberTermsAgreement;
import makeus.cmc.malmo.domain.model.terms.Terms;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.id.TermsId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignUpService implements SignUpUseCase {

    private final MemberQueryHelper memberQueryHelper;
    private final MemberCommandHelper memberCommandHelper;

    private final TermsQueryHelper termsQueryHelper;
    private final TermsCommandHelper termsCommandHelper;

    private final TempLoveTypeHelper tempLoveTypeHelper;

    @Override
    @Transactional
    @CheckValidMember
    public void signUp(SignUpUseCase.SignUpCommand command) {
        Member member = memberQueryHelper.getMemberByIdOrThrow(MemberId.of(command.getMemberId()));
        member.signUp(
                command.getNickname(),
                command.getRelationshipStatus()
        );

        // 회원가입 전 애착 유형 검사를 진행했던 사용자인 경우 해당 정보를 가져와 덮어쓰기
        if (command.getLoveTypeId() != null) {
            tempLoveTypeHelper.getTempLoveType(command.getLoveTypeId())
                    .ifPresent(tempLoveType ->
                            member.updateLoveType(tempLoveType.getCategory(),
                                    tempLoveType.getAvoidanceRate(),
                                    tempLoveType.getAnxietyRate())
                    );
        }

        memberCommandHelper.saveMember(member);

        // 약관 동의 여부 저장
        command.getTerms().forEach(termsCommand -> {
            Terms terms = termsQueryHelper.getTermsByIdOrThrow(termsCommand.getTermsId());
            MemberTermsAgreement memberTermsAgreement = MemberTermsAgreement.signTerms(
                    MemberId.of(member.getId()),
                    TermsId.of(terms.getId()),
                    termsCommand.getIsAgreed());

            termsCommandHelper.saveMemberTermsAgreement(memberTermsAgreement);
        });
    }
}
