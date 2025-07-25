package makeus.cmc.malmo.domain.service;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.LoadTermsAgreementPort;
import makeus.cmc.malmo.application.port.out.LoadTermsPort;
import makeus.cmc.malmo.application.port.out.SaveMemberTermsAgreement;
import makeus.cmc.malmo.domain.exception.TermsNotFoundException;
import makeus.cmc.malmo.domain.model.terms.MemberTermsAgreement;
import makeus.cmc.malmo.domain.model.terms.Terms;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.id.TermsId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TermsAgreementDomainService {

    private final LoadTermsPort loadTermsPort;
    private final LoadTermsAgreementPort loadTermsAgreementPort;
    private final SaveMemberTermsAgreement saveMemberTermsAgreement;

    public record TermAgreementInput(Long termsId, boolean isAgreed) {}

    public void processAgreements(MemberId memberId, List<TermAgreementInput> agreementInputs) {
        agreementInputs.forEach(input -> {
            Terms terms = loadTermsPort.loadTermsById(input.termsId())
                    .orElseThrow(TermsNotFoundException::new);

            MemberTermsAgreement memberTermsAgreement = MemberTermsAgreement.signTerms(
                    memberId,
                    TermsId.of(terms.getId()),
                    input.isAgreed());
            saveMemberTermsAgreement.saveMemberTermsAgreement(memberTermsAgreement);
        });
    }

    public MemberTermsAgreement getTermsAgreement(MemberId memberId, TermsId termsId) {
        return loadTermsAgreementPort.loadTermsAgreementByMemberIdAndTermsId(memberId, termsId)
                .orElseThrow(TermsNotFoundException::new);
    }
}
