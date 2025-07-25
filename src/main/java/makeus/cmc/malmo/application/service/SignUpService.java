package makeus.cmc.malmo.application.service;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.aop.CheckValidMember;
import makeus.cmc.malmo.application.port.in.SignUpUseCase;
import makeus.cmc.malmo.application.port.out.SaveMemberPort;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.service.MemberDomainService;
import makeus.cmc.malmo.domain.service.TermsAgreementDomainService;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SignUpService implements SignUpUseCase {

    private final MemberDomainService memberDomainService;
    private final SaveMemberPort saveMemberPort;
    private final TermsAgreementDomainService termsAgreementDomainService;

    @Override
    @Transactional
    @CheckValidMember
    public void signUp(SignUpCommand command) {
        Member member = memberDomainService.getMemberById(MemberId.of(command.getMemberId()));
        member.signUp(command.getNickname(), command.getLoveStartDate());
        saveMemberPort.saveMember(member);

        List<TermsAgreementDomainService.TermAgreementInput> agreementInputs = command.getTerms().stream()
                .map(term -> new TermsAgreementDomainService.TermAgreementInput(term.getTermsId(), term.getIsAgreed()))
                .toList();
        termsAgreementDomainService.processAgreements(MemberId.of(member.getId()), agreementInputs);
    }
}
